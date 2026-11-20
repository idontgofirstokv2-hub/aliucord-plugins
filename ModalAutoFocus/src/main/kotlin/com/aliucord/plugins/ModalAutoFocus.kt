package com.aliucord.plugins

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after

@AliucordPlugin
class ModalAutoFocus : Plugin() {
    override fun start(context: Context) {
        patcher.after<Dialog>("show") {
            val dialog = it.thisObject as? Dialog ?: return@after
            val window = dialog.window ?: return@after

            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            window.decorView.post {
                focusModalInput(window, context, retriesLeft = 3)
            }
        }
    }

    private fun focusModalInput(window: Window, context: Context, retriesLeft: Int) {
        val editText = findFirstEditText(window.decorView)
        if (editText != null) {
            if (editText.requestFocus()) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        } else if (retriesLeft > 0) {
            window.decorView.postDelayed({
                focusModalInput(window, context, retriesLeft - 1)
            }, 25L)
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
