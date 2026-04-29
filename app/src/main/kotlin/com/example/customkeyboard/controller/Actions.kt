package com.example.customkeyboard.controller

import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.KeyboardLayer
import com.example.customkeyboard.model.ShiftState

/**
 * Abstracts all text-input operations performed on the [android.view.inputmethod.InputConnection].
 *
 * Decoupling these calls from [KeyboardController] allows the controller to be tested on the
 * plain JVM without any Android framework dependencies.
 */
interface InputActions {
    /** Commits [text] to the current input field. */
    fun commitText(text: String)

    /** Deletes the character immediately before the cursor. */
    fun deleteCharBefore()

    /** Sends an editor action (e.g. Done, Search, Send) identified by [actionCode]. */
    fun performEditorAction(actionCode: Int)
}

/**
 * Abstracts all view-update operations that [KeyboardController] needs to trigger on the UI.
 *
 * Decoupling these calls from [KeyboardController] allows the controller to be tested on the
 * plain JVM without any Android framework dependencies.
 */
interface ViewActions {
    /** Updates the visual indicator on the Shift key to reflect [state]. */
    fun updateShiftIndicator(state: ShiftState)

    /** Switches the visible keyboard layer to [layer]. */
    fun switchLayer(layer: KeyboardLayer)

    /** Displays a popup preview above the tapped [key]. */
    fun showKeyPreview(key: Key)

    /** Hides any currently visible key popup preview. */
    fun dismissKeyPreview()

    /**
     * Updates the Enter key label/icon to match the current [imeOptions] flags
     * (e.g. [android.view.inputmethod.EditorInfo.IME_ACTION_DONE]).
     */
    fun updateEnterLabel(imeOptions: Int)
}
