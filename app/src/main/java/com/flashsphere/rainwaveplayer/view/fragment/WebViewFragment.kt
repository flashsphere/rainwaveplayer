package com.flashsphere.rainwaveplayer.view.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.flashsphere.rainwaveplayer.databinding.LayoutWebViewBinding
import com.flashsphere.rainwaveplayer.databinding.WebViewBinding
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.view.activity.delegate.StoreUserCredentialsDelegate
import com.flashsphere.rainwaveplayer.view.autofill.Autofill
import com.flashsphere.rainwaveplayer.view.viewmodel.StoreUserCredentialsViewModel
import com.flashsphere.rainwaveplayer.view.webview.CustomWebChromeClient
import com.flashsphere.rainwaveplayer.view.webview.CustomWebViewClient
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class WebViewFragment : Fragment() {
    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var playbackManager: PlaybackManager

    private lateinit var storeUserCredentialsDelegate: StoreUserCredentialsDelegate

    private val viewModel: StoreUserCredentialsViewModel by viewModels()

    private var backPressedCallback: OnBackPressedCallback? = null

    private var _binding: LayoutWebViewBinding? = null
    private val binding get() = _binding!!

    private var webView: WebView? = null

    private var isWebViewPendingRecovery = false
    private var webViewStateToRestore: Bundle? = null
    private var crashCount = 0

    var pageTitleChangedCallback: ((title: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutWebViewBinding.inflate(inflater, container, false)

        setupBackPressCallback()
        setupWebView()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // If a web view crash happened in the background, rebuild it now that the user can see it
        if (isWebViewPendingRecovery) {
            isWebViewPendingRecovery = false
            recoverWebView()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webViewState = savedInstanceState?.getBundle(BUNDLE_WEB_VIEW_STATE)
        if (webViewState != null) {
            webView?.let {
                it.restoreState(webViewState)
                it.scrollX = savedInstanceState.getInt(BUNDLE_WEB_VIEW_SCROLL_X)
                it.scrollY = savedInstanceState.getInt(BUNDLE_WEB_VIEW_SCROLL_Y)
            }
        } else {
            val url = arguments?.getString(ARG_URL)
            if (!url.isNullOrBlank()) {
                webView?.loadUrl(url)
            } else {
                finishActivity()
            }
        }

        storeUserCredentialsDelegate = StoreUserCredentialsDelegate(requireContext(), viewModel,
            stationRepository, userRepository, mediaPlayerStateObserver, playbackManager, this::finishActivity)
        viewLifecycleOwner.lifecycle.addObserver(storeUserCredentialsDelegate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.let {
            val webViewState = Bundle()
            it.saveState(webViewState)
            outState.putBundle(BUNDLE_WEB_VIEW_STATE, webViewState)
            outState.putInt(BUNDLE_WEB_VIEW_SCROLL_X, it.scrollX)
            outState.putInt(BUNDLE_WEB_VIEW_SCROLL_Y, it.scrollY)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupBackPressCallback() {
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                webView?.goBack()
            }
        }.also {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, it)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = WebViewBinding.inflate(layoutInflater, binding.webViewContainer, true).webview
            .also {
                this.webView = it
            }

        val customWebViewClient = CustomWebViewClient(
            callback = object : CustomWebViewClient.Callback {
                override fun pageTitleChanged(title: String) {
                    pageTitleChangedCallback?.invoke(title)
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                    backPressedCallback?.isEnabled = view.canGoBack()
                }

                override fun shouldOverrideUrlLoading(url: String): Boolean {
                    if (url.startsWith("rw://")) {
                        storeUserCredentialsDelegate.process(url.toUri())
                        return true
                    }
                    return false
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    Timber.d("Render process crashed: %s", detail.didCrash())
                    webViewStateToRestore = Bundle().apply { webView.saveState(this) }

                    binding.webViewContainer.removeView(webView)
                    webView.destroy()
                    this@WebViewFragment.webView = null

                    // Check the lifecycle state of the Activity before recovering
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        recoverWebView()
                    } else {
                        // The app is in the background. Mark it for recovery later!
                        isWebViewPendingRecovery = true
                    }
                    return true
                }
            }
        )

        val autofill = Autofill(requireContext())
        webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    autofill.requestAutofill(v)
                }
            }

            webViewClient = customWebViewClient
            webChromeClient = CustomWebChromeClient(binding.progressBar)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
            }
            webViewStateToRestore?.let {
                restoreState(it)
                webViewStateToRestore = null
            }
        }
    }

    private fun recoverWebView() {
        if (crashCount < MAX_CRASHES) {
            crashCount++
            setupWebView()
        } else {
            finishActivity()
        }
    }

    private fun finishActivity() {
        requireActivity().finish()
    }

    companion object {
        private const val MAX_CRASHES = 3
        const val ARG_URL = "arg_url"
        private const val BUNDLE_WEB_VIEW_STATE = "bundle_web_view_state"
        private const val BUNDLE_WEB_VIEW_SCROLL_X = "bundle_web_view_scroll_x"
        private const val BUNDLE_WEB_VIEW_SCROLL_Y = "bundle_web_view_scroll_y"
    }
}
