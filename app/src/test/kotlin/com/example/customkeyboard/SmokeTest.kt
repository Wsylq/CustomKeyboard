package com.example.customkeyboard

import com.example.customkeyboard.controller.HandlerWrapper
import com.example.customkeyboard.controller.InputActions
import com.example.customkeyboard.controller.KeyboardController
import com.example.customkeyboard.controller.ViewActions
import com.example.customkeyboard.model.Key
import com.example.customkeyboard.model.KeyboardLayer
import com.example.customkeyboard.model.QwertyLayout
import com.example.customkeyboard.model.ShiftState
import com.example.customkeyboard.model.SymbolLayout
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// ---------------------------------------------------------------------------
// Fake implementations for testing (no Android framework dependencies)
// ---------------------------------------------------------------------------

class FakeInputActions : InputActions {
    val committed = mutableListOf<String>()
    var deleteCalled = 0
    var lastActionCode = -1
    override fun commitText(text: String) { committed.add(text) }
    override fun deleteCharBefore() { deleteCalled++ }
    override fun performEditorAction(actionCode: Int) { lastActionCode = actionCode }
}

class FakeViewActions : ViewActions {
    var lastShiftState: ShiftState? = null
    var lastLayer: KeyboardLayer? = null
    var previewDismissed = false
    var lastImeOptions = -1
    override fun updateShiftIndicator(state: ShiftState) { lastShiftState = state }
    override fun switchLayer(layer: KeyboardLayer) { lastLayer = layer }
    override fun showKeyPreview(key: Key) {}
    override fun dismissKeyPreview() { previewDismissed = true }
    override fun updateEnterLabel(imeOptions: Int) { lastImeOptions = imeOptions }
}

class NoOpHandlerWrapper : HandlerWrapper {
    override fun postDelayed(runnable: Runnable, delayMs: Long) {}
    override fun removeCallbacks(runnable: Runnable) {}
}

// ---------------------------------------------------------------------------
// Smoke tests
// ---------------------------------------------------------------------------

class SmokeTest : FunSpec({

    // -----------------------------------------------------------------------
    // Layout tests
    // -----------------------------------------------------------------------

    test("QwertyLayout.allLetterKeys has 26 entries") {
        QwertyLayout.allLetterKeys.size shouldBe 26
    }

    test("SymbolLayout.allSymbolKeys has 40 entries") {
        SymbolLayout.allSymbolKeys.size shouldBe 40
    }

    // -----------------------------------------------------------------------
    // KeyboardController instantiation
    // -----------------------------------------------------------------------

    test("KeyboardController can be instantiated with fake dependencies") {
        val fakeInput = FakeInputActions()
        val fakeView = FakeViewActions()
        val controller = KeyboardController(fakeInput, fakeView, clock = { 0L }, handler = NoOpHandlerWrapper())
        // Just verify it was created without throwing
        controller.shiftState shouldBe ShiftState.OFF
        controller.currentLayer shouldBe KeyboardLayer.QWERTY
    }

    // -----------------------------------------------------------------------
    // Shift state transitions
    // -----------------------------------------------------------------------

    test("Shift state transitions: OFF -> SINGLE on first tap") {
        val fakeInput = FakeInputActions()
        val fakeView = FakeViewActions()
        var time = 0L
        val controller = KeyboardController(fakeInput, fakeView, clock = { time }, handler = NoOpHandlerWrapper())

        time = 1000L
        controller.onShiftTapped()
        controller.shiftState shouldBe ShiftState.SINGLE
    }

    test("Shift state transitions: SINGLE -> OFF on second tap (slow)") {
        val fakeInput = FakeInputActions()
        val fakeView = FakeViewActions()
        var time = 0L
        val controller = KeyboardController(fakeInput, fakeView, clock = { time }, handler = NoOpHandlerWrapper())

        // First tap: OFF -> SINGLE
        time = 1000L
        controller.onShiftTapped()
        controller.shiftState shouldBe ShiftState.SINGLE

        // Second tap after >400ms: SINGLE -> OFF
        time = 2000L
        controller.onShiftTapped()
        controller.shiftState shouldBe ShiftState.OFF
    }

    test("Shift state transitions: double-tap within 400ms -> CAPS_LOCK") {
        val fakeInput = FakeInputActions()
        val fakeView = FakeViewActions()
        var time = 0L
        val controller = KeyboardController(fakeInput, fakeView, clock = { time }, handler = NoOpHandlerWrapper())

        // First tap at t=1000
        time = 1000L
        controller.onShiftTapped()

        // Second tap within 400ms -> CAPS_LOCK
        time = 1200L
        controller.onShiftTapped()
        controller.shiftState shouldBe ShiftState.CAPS_LOCK
    }

    test("Shift state transitions: CAPS_LOCK -> OFF on tap") {
        val fakeInput = FakeInputActions()
        val fakeView = FakeViewActions()
        var time = 0L
        val controller = KeyboardController(fakeInput, fakeView, clock = { time }, handler = NoOpHandlerWrapper())

        // Get to CAPS_LOCK via double-tap
        time = 1000L
        controller.onShiftTapped()
        time = 1200L
        controller.onShiftTapped()
        controller.shiftState shouldBe ShiftState.CAPS_LOCK

        // Single tap: CAPS_LOCK -> OFF
        time = 2000L
        controller.onShiftTapped()
        controller.shiftState shouldBe ShiftState.OFF
    }
})
