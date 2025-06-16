package com.flashsphere.rainwaveplayer.cast

import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

interface DefaultSessionManagerListener : SessionManagerListener<CastSession> {
    override fun onSessionStarting(castSession: CastSession) {
    }

    override fun onSessionStarted(castSession: CastSession, sessionId: String) {
    }

    override fun onSessionStartFailed(castSession: CastSession, error: Int) {
    }

    override fun onSessionEnding(castSession: CastSession) {
    }

    override fun onSessionEnded(castSession: CastSession, error: Int) {
    }

    override fun onSessionResuming(castSession: CastSession, sessionId: String) {
    }

    override fun onSessionResumed(castSession: CastSession, wasSuspended: Boolean) {
    }

    override fun onSessionResumeFailed(castSession: CastSession, error: Int) {
    }

    override fun onSessionSuspended(castSession: CastSession, reason: Int) {
    }
}
