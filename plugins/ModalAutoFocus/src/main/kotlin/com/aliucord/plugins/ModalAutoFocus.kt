package com.aliucord.plugins

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after

@AliucordPlugin
class ModalAutoFocus : Plugin() {
    override fun start(context: Context) {
        patcher.after<Fragment>("onResume") {
            val fragment = it.thisObject as? Fragment ?: return@after
            val root = fragment.view ?: return@after
            val className = fragment.javaClass.name

            if (!className.contains("Modal", ignoreCase = true) && !hasModalBanner(root)) {
                return@after
            }

            fragment.activity?.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )

            root.post {
                focusModalInput(root, retriesLeft = 10)
            }
        }
    }

    private fun focusModalInput(root: View, retriesLeft: Int) {
        val editText = findFirstEditText(root)
        if (editText != null && editText.isAttachedToWindow) {
            editText.isFocusable = true
            editText.isFocusableInTouchMode = true
            editText.requestFocus()
            editText.setSelection(editText.text?.length ?: 0)

            editText.post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    editText.windowInsetsController?.show(WindowInsets.Type.ime())
                }
                val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        } else if (retriesLeft > 0) {
            root.postDelayed({
                focusModalInput(root, retriesLeft - 1)
            }, 25L)
        }
    }

    private fun hasModalBanner(view: View): Boolean {
        if (view is TextView) {
            val text = view.text?.toString() ?: ""
            if (text.contains("will be submitted to", ignoreCase = true)) {
                return true
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (hasModalBanner(view.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun findFirstEditText(view: View): EditText? {
        if (view is EditText && view.isEnabled && view.visibility == View.VISIBLE) {
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
