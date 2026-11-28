package dev.david.tessel.core.dev

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapCounterTest {
    private fun taps(c: TapCounter, n: Int) = repeat(n) { c.tap() }

    @Test
    fun fiveTapsThenQuietOpensIt() {
        val c = TapCounter()
        taps(c, 5)
        assertTrue(c.settle())
        assertFalse("only once", c.settle())
    }

    @Test
    fun moreThanFiveDoesNotOpenIt() {
        val c = TapCounter()
        taps(c, 6)
        assertFalse(c.settle())
    }

    @Test
    fun fewerThanFiveDoesNotOpenIt() {
        val c = TapCounter()
        taps(c, 4)
        assertFalse(c.settle())
    }

    @Test
    fun spammingPastFiveAndSettlingStartsOver() {
        val c = TapCounter()
        taps(c, 12)
        assertFalse(c.settle())
        taps(c, 5)
        assertTrue(c.settle())
    }

    @Test
    fun aPauseInTheMiddleSplitsTheRun() {
        val c = TapCounter()
        taps(c, 4)
        assertFalse("a second of quiet ended the first run", c.settle())
        taps(c, 3)
        assertFalse("three more are three, not five in all", c.settle())
    }

    @Test
    fun theCodeIsFourDigits() {
        assertTrue(DevAccess.CODE.length == DevAccess.CODE_LENGTH)
    }
}
