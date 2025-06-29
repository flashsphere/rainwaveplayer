package com.flashsphere.rainwaveplayer.view.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flashsphere.rainwaveplayer.databinding.LayoutWebViewBinding
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.okhttp.TrustedCertificateStore
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.view.activity.delegate.StoreUserCredentialsDelegate
import com.flashsphere.rainwaveplayer.view.autofill.Autofill
import com.flashsphere.rainwaveplayer.view.viewmodel.StoreUserCredentialsViewModel
import com.flashsphere.rainwaveplayer.view.webview.CustomWebChromeClient
import com.flashsphere.rainwaveplayer.view.webview.CustomWebViewClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebViewFragment : Fragment() {
    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var trustedCertificateStore: TrustedCertificateStore

    @Inject
    lateinit var playbackManager: PlaybackManager

    private lateinit var storeUserCredentialsDelegate: StoreUserCredentialsDelegate

    private val viewModel: StoreUserCredentialsViewModel by viewModels()

    private var backPressedCallback: OnBackPressedCallback? = null

    private var _binding: LayoutWebViewBinding? = null
    private val binding get() = _binding!!

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webViewState = savedInstanceState?.getBundle(BUNDLE_WEB_VIEW_STATE)
        if (webViewState != null) {
            binding.webview.restoreState(webViewState)
            binding.webview.scrollX = savedInstanceState.getInt(BUNDLE_WEB_VIEW_SCROLL_X)
            binding.webview.scrollY = savedInstanceState.getInt(BUNDLE_WEB_VIEW_SCROLL_Y)
        } else {
            val url = arguments?.getString(ARG_URL)
            if (!url.isNullOrBlank()) {
                binding.webview.loadUrl(url)
            } else {
                finishActivity()
            }
        }

        storeUserCredentialsDelegate = StoreUserCredentialsDelegate(requireContext(), viewModel,
            stationRepository, userRepository, mediaPlayerStateObserver, playbackManager,
            analytics, this::finishActivity)
        viewLifecycleOwner.lifecycle.addObserver(storeUserCredentialsDelegate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webViewState = Bundle()
        binding.webview.saveState(webViewState)
        outState.putBundle(BUNDLE_WEB_VIEW_STATE, webViewState)
        outState.putInt(BUNDLE_WEB_VIEW_SCROLL_X, binding.webview.scrollX)
        outState.putInt(BUNDLE_WEB_VIEW_SCROLL_Y, binding.webview.scrollY)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupBackPressCallback() {
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding.webview.goBack()
            }
        }.also {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, it)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val customWebViewClient = CustomWebViewClient(
            trustedCertificateStore = trustedCertificateStore,
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
            }
        )

        val autofill = Autofill(requireContext())
        binding.webview.apply {
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
        }
    }

    private fun finishActivity() {
        requireActivity().finish()
    }

    companion object {
        const val ARG_URL = "arg_url"
        private const val BUNDLE_WEB_VIEW_STATE = "bundle_web_view_state"
        private const val BUNDLE_WEB_VIEW_SCROLL_X = "bundle_web_view_scroll_x"
        private const val BUNDLE_WEB_VIEW_SCROLL_Y = "bundle_web_view_scroll_y"
    }
}
