package com.flashsphere.rainwaveplayer.view.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StartPlaybackActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MediaService.play(this)
        finish()
    }

    companion object {
        fun getCallingIntent(context: Context): Intent {
            return Intent(context, StartPlaybackActivity::class.java)
        }

        fun getPendingIntent(context: Context): PendingIntent {
            val intent = getCallingIntent(context)
            return PendingIntent.getActivity(context, R.id.start_playback_activity_request_code, intent, PendingIntentUtils.getPendingIntentFlags())
        }
    }
}
