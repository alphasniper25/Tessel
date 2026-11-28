package dev.david.tessel.core.dev
import org.junit.Assert.*
import org.junit.Test
class TapCounterTest{
    @Test fun exactlyFiveThenQuietOnly(){
        for(n in 0..15){
            val c=TapCounter();repeat(n){c.tap()}
            assertEquals(n==5,c.settle());assertFalse(c.settle())
        }
    }
    @Test fun separateBurstsCannotAccumulate(){
        val c=TapCounter();repeat(2){c.tap()};assertFalse(c.settle())
        repeat(3){c.tap()};assertFalse(c.settle())
        repeat(5){c.tap()};assertTrue(c.settle())
    }
    @Test fun accessPinHasFourDigits(){assertEquals("5144",DevAccess.CODE);assertEquals(4,DevAccess.CODE_LENGTH)}
}
