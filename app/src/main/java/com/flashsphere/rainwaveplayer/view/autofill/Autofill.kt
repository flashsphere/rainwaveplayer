package com.flashsphere.rainwaveplayer.view.autofill

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.view.View
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class Autofill(context: Context) {
    private val impl = if (Build.VERSION.SDK_INT >= O) {
        AutofillApi26(context)
    } else {
        AutofillNoOp()
    }

    fun requestAutofill(view: View) {
        impl.requestAutofill(view)
    }
}

interface AutofillImpl {
    fun requestAutofill(view: View)
}

// No-op for API < 26
private class AutofillNoOp : AutofillImpl {
    override fun requestAutofill(view: View) {}
}

@RequiresApi(O)
private class AutofillApi26(context: Context) : AutofillImpl {
    private val autofillManager = ContextCompat.getSystemService(context, AutofillManager::class.java)

    override fun requestAutofill(view: View) {
        if (autofillManager != null && autofillManager.isEnabled) {
            autofillManager.requestAutofill(view)
        }
    }
}
