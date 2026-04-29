package ru.devandprod.chestniyznak.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChestniyZnakParserTest {

    private val parser = ChestniyZnakParser()

    @Test
    fun `parse keeps gs tail and ai parts`() {
        val result = parser.parse("010460123456789021SERIAL0000000000001\u001D91ABCD\u001D92WXYZ9876")

        assertEquals("04601234567890", result.gtin)
        assertEquals("SERIAL0000000000001", result.serial)
        assertEquals("ABCD", result.aiParts["91"])
        assertEquals("WXYZ9876", result.aiParts["92"])
        assertTrue(result.visibleCode.contains("<GS>"))
    }

    @Test
    fun `parse restores missing gs after long serial`() {
        val result = parser.parse("010460123456789021SERIAL0000000000000191ABCD92TAIL")

        assertEquals("SERIAL00000000000001", result.serial)
        assertEquals("ABCD", result.aiParts["91"])
        assertEquals("TAIL", result.aiParts["92"])
        assertTrue(result.gsRestored)
    }

    @Test
    fun `parse supports bracketed ai scanner input`() {
        val result = parser.parse("(01)04601234567890(21)SERIAL0001(91)ABCD(92)TAIL123")

        assertEquals("04601234567890", result.gtin)
        assertEquals("SERIAL0001", result.serial)
        assertEquals("ABCD", result.aiParts["91"])
        assertEquals("TAIL123", result.aiParts["92"])
        assertTrue(result.visibleCode.contains("<GS>91ABCD<GS>92TAIL123"))
        assertFalse(result.gsRestored)
    }

    @Test(expected = ChestniyZnakParseException::class)
    fun `parse fails when code does not start with ai 01`() {
        parser.parse("220460123456789021SERIAL0001")
    }

    @Test
    fun `parse keeps short serial when tail is absent`() {
        val result = parser.parse("010460123456789021SHORTSERIAL")

        assertEquals("SHORTSERIAL", result.serial)
        assertTrue(result.aiParts.isEmpty())
        assertTrue(result.warnings.contains("GS отсутствует; AI-хвост после serial не обнаружен"))
    }
}
