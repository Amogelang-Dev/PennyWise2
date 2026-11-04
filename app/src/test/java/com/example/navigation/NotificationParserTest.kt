package com.example.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationParserTest {

    @Test
    fun testExtractAmount_StandardFormat() {
        val text = "FNB: R 150.00 spent at Checkers"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(150.00f, amount)
    }

    @Test
    fun testExtractAmount_NoSpaceFormat() {
        val text = "Capitec: R200.50 paid to Nedbank"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(200.50f, amount)
    }

    @Test
    fun testExtractAmount_ThousandsSeparatorComma() {
        val text = "Standard Bank: R 1,234.56 payment received"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(1234.56f, amount)
    }

    @Test
    fun testExtractAmount_ThousandsSeparatorSpace() {
        val text = "ABSA: R 2 500.00 withdrawn"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(2500.00f, amount)
    }

    @Test
    fun testExtractAmount_DecimalComma() {
        val text = "Nedbank: R 99,99 purchase"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(99.99f, amount)
    }

    @Test
    fun testExtractAmount_MultipleDots() {
        // Some systems might format as 1.000.00 which is weird but possible
        val text = "Bank: R 1.000.50"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(1000.50f, amount)
    }

    @Test
    fun testExtractAmount_NoAmount() {
        val text = "Your account is active"
        val amount = NotificationParser.extractAmount(text)
        assertEquals(0f, amount)
    }
}
