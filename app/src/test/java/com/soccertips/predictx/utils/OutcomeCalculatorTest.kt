package com.soccertips.predictx.utils

import org.junit.Assert.*
import org.junit.Test

class OutcomeCalculatorTest {

    @Test
    fun testBasicMatchOutcome() {
        // Test home win prediction
        assertEquals("win", OutcomeCalculator.calculateOutcome("1", "2-1", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("1", "1-2", null))

        // Test away win prediction
        assertEquals("win", OutcomeCalculator.calculateOutcome("2", "1-2", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("2", "2-1", null))

        // Test draw prediction
        assertEquals("win", OutcomeCalculator.calculateOutcome("X", "1-1", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("X", "2-1", null))
    }

    @Test
    fun testDoubleChanceOutcome() {
        // Test 1X (home win or draw)
        assertEquals("win", OutcomeCalculator.calculateOutcome("1X", "2-1", null)) // home win
        assertEquals("win", OutcomeCalculator.calculateOutcome("1X", "1-1", null)) // draw
        assertEquals("lose", OutcomeCalculator.calculateOutcome("1X", "1-2", null)) // away win

        // Test 2X (away win or draw)
        assertEquals("win", OutcomeCalculator.calculateOutcome("2X", "1-2", null)) // away win
        assertEquals("win", OutcomeCalculator.calculateOutcome("2X", "1-1", null)) // draw
        assertEquals("lose", OutcomeCalculator.calculateOutcome("2X", "2-1", null)) // home win
    }

    @Test
    fun testGoalTotalOutcome() {
        // Test Over predictions
        assertEquals("win", OutcomeCalculator.calculateOutcome("Over 1.5", "2-1", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("Over 2.5", "1-1", null))
        assertEquals("win", OutcomeCalculator.calculateOutcome("Over 2.5", "2-2", null))

        // Test Under predictions
        assertEquals("win", OutcomeCalculator.calculateOutcome("Under 2.5", "1-0", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("Under 1.5", "1-1", null))
    }

    @Test
    fun testCombinationOutcome() {
        // Test "1 and Over 1.5"
        assertEquals("win", OutcomeCalculator.calculateOutcome("1 and Over 1.5", "2-1", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("1 and Over 1.5", "1-2", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("1 and Over 1.5", "1-0", null))

        // Test "2 & Over 2.5"
        assertEquals("win", OutcomeCalculator.calculateOutcome("2 & Over 2.5", "1-3", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("2 & Over 2.5", "3-1", null))

        // Test "GG and Over 2.5" combinations
        assertEquals(
                "win",
                OutcomeCalculator.calculateOutcome("GG and Over 2.5", "2-2", null)
        ) // Both teams score + over 2.5
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("GG and Over 2.5", "2-0", null)
        ) // No GG
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("GG and Over 2.5", "1-1", null)
        ) // GG but under 2.5

        // Test "BTTS & Over 1.5" combinations
        assertEquals(
                "win",
                OutcomeCalculator.calculateOutcome("BTTS & Over 1.5", "1-1", null)
        ) // Both teams score + over 1.5
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("BTTS & Over 1.5", "3-0", null)
        ) // No BTTS
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("BTTS & Over 1.5", "0-1", null)
        ) // No BTTS

        // Test "GG-NO and Under 2.5" combinations
        assertEquals(
                "win",
                OutcomeCalculator.calculateOutcome("GG-NO and Under 2.5", "2-0", null)
        ) // No GG + under 2.5
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("GG-NO and Under 2.5", "1-1", null)
        ) // GG occurred
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("GG-NO and Under 2.5", "3-0", null)
        ) // Over 2.5

        // Test additional GG/BTTS and Over/Under combinations
        assertEquals(
                "win",
                OutcomeCalculator.calculateOutcome("GG and Over 3.5", "2-3", null)
        ) // Both score + over 3.5
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("GG and Over 3.5", "2-1", null)
        ) // Both score but under 3.5
        assertEquals(
                "win",
                OutcomeCalculator.calculateOutcome("BTTS & Under 3.5", "1-2", null)
        ) // Both score + under 3.5
        assertEquals(
                "lose",
                OutcomeCalculator.calculateOutcome("BTTS & Under 3.5", "2-2", null)
        ) // Both score but over 3.5
    }
    @Test
    fun testBTTSOutcome() {
        // Test Both Teams to Score
        assertEquals("win", OutcomeCalculator.calculateOutcome("GG (BTTS)", "2-1", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("GG (BTTS)", "2-0", null))

        // Test Both Teams NOT to Score
        assertEquals("win", OutcomeCalculator.calculateOutcome("GG-NO", "2-0", null))
        assertEquals("lose", OutcomeCalculator.calculateOutcome("GG-NO", "1-1", null))
    }

    @Test
    fun testScoreParsing() {
        // Test various score formats
        assertEquals("win", OutcomeCalculator.calculateOutcome("1", "2-1", null))
        assertEquals("win", OutcomeCalculator.calculateOutcome("1", "2:1", null))
        assertEquals("win", OutcomeCalculator.calculateOutcome("1", "3-2:(Odds:1.250)", null))
    }

    @Test
    fun testEdgeCases() {
        // Test with existing valid outcome
        assertEquals("win", OutcomeCalculator.calculateOutcome("1", "1-2", "win"))

        // Test with invalid inputs
        assertEquals("Unknown", OutcomeCalculator.calculateOutcome("1", "-", null))
        assertEquals("Unknown", OutcomeCalculator.calculateOutcome("", "2-1", null))
        assertEquals("Unknown", OutcomeCalculator.calculateOutcome("1", "", null))
    }
}
