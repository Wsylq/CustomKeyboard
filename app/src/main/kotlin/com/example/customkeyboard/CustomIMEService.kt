package com.example.customkeyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.customkeyboard.controller.InputActions
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.view.KeyboardView

class CustomIMEService : InputMethodService() {

    private lateinit var controller: KeyboardController
    private var keyboardView: KeyboardView? = null

    override fun onCreateInputView(): View {
        val inputActions = object : InputActions {
            override fun commitText(text: String) {
                currentInputConnection?.commitText(text, 1)
            }
            override fun deleteCharBefore() {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            override fun performEditorAction(actionCode: Int) {
                currentInputConnection?.performEditorAction(actionCode)
            }
        }

        val kbView = KeyboardView(this)
        keyboardView = kbView

        controller = KeyboardController(inputActions, kbView)
        kbView.setController(controller)

        return kbView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        controller.onStartInput(info.imeOptions)
    }
}
