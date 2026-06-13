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
    fun `parse supports short 44 code without gs`() {
        val result = parser.parse("010460700123456721SERIAL1234567890123493ABCD")

        assertEquals("04607001234567", result.gtin)
        assertEquals("SERIAL12345678901234", result.serial)
        assertEquals("ABCD", result.aiParts["93"])
        assertEquals("010460700123456721SERIAL12345678901234<GS>93ABCD", result.visibleCode)
        assertTrue(result.gsRestored)
    }

    @Test
    fun `parse supports long crypto tail without gs`() {
        val result = parser.parse("010460700123456721SERIAL1234567890123491KEY192" + "X".repeat(44))

        assertEquals("SERIAL12345678901234", result.serial)
        assertEquals("KEY1", result.aiParts["91"])
        assertEquals("X".repeat(44), result.aiParts["92"])
        assertEquals(
            "010460700123456721SERIAL12345678901234<GS>91KEY1<GS>92" + "X".repeat(44),
            result.visibleCode,
        )
        assertTrue(result.gsRestored)
    }

    @Test
    fun `parse supports longer crypto tail without gs`() {
        val result = parser.parse("010460700123456721SERIAL1234567890123491KEY192" + "Y".repeat(49))

        assertEquals("SERIAL12345678901234", result.serial)
        assertEquals("KEY1", result.aiParts["91"])
        assertEquals("Y".repeat(49), result.aiParts["92"])
        assertTrue(result.visibleCode.contains("<GS>91KEY1<GS>92"))
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

    @Test
    fun `parse supports visible gs aliases`() {
        val result = parser.parse("\u001D010460123456789021SERIAL0000000000001{GS}91ABCD\\03592TAIL")

        assertEquals("SERIAL0000000000001", result.serial)
        assertEquals("ABCD", result.aiParts["91"])
        assertEquals("TAIL", result.aiParts["92"])
        assertTrue(result.visibleCode.contains("<GS>91ABCD<GS>92TAIL"))
    }

    @Test
    fun `parse supports bare x1d scanner separator`() {
        val result = parser.parse("010460123456789121A200000001x1d91PH1Vx1d92CRYPTO")

        assertEquals("04601234567891", result.gtin)
        assertEquals("A200000001", result.serial)
        assertEquals("PH1V", result.aiParts["91"])
        assertEquals("CRYPTO", result.aiParts["92"])
        assertTrue(result.visibleCode.contains("<GS>91PH1V<GS>92CRYPTO"))
        assertEquals(
            "010460123456789121A200000001\u001D91PH1V\u001D92CRYPTO",
            result.rawCode,
        )
    }
}
