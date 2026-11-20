package com.aliucord.plugins

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after

@AliucordPlugin
class ModalAutoFocus : Plugin() {
    override fun start(context: Context) {
        patcher.after<DialogFragment>("onStart") {
            val fragment = it.thisObject as? DialogFragment ?: return@after
            val dialog = fragment.dialog ?: return@after
            val window = dialog.window ?: return@after

            window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            )
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            window.decorView.post {
                focusModalInput(window, retriesLeft = 15)
            }
        }
    }

    private fun focusModalInput(window: Window, retriesLeft: Int) {
        val editText = findFirstEditText(window.decorView)
        if (editText != null && editText.isAttachedToWindow) {
            editText.requestFocus()
            editText.post {
                val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                if (imm != null && !imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)) {
                    @Suppress("DEPRECATION")
                    imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
                }
            }
        } else if (retriesLeft > 0) {
            window.decorView.postDelayed({
                focusModalInput(window, retriesLeft - 1)
            }, 30L)
        }
    }

    private fun findFirstEditText(view: View): EditText? {
        if (view is EditText && view.isEnabled && view.isFocusable && view.visibility == View.VISIBLE) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findFirstEditText(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
