package ru.devandprod.chestniyznak.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthTokenExtractorTest {

    @Test
    fun `returns raw token when scanner sends plain string`() {
        assertEquals("LKIC-HDDS-NK4K", AuthTokenExtractor.extract("LKIC-HDDS-NK4K"))
    }

    @Test
    fun `formats compact plain token`() {
        assertEquals("LKIC-HDDS-NK4K", AuthTokenExtractor.extract("lkichddsnk4k"))
    }

    @Test
    fun `extracts token from query string`() {
        assertEquals(
            "LKIC-HDDS-NK4K",
            AuthTokenExtractor.extract("https://example.test/login?token=LKIC-HDDS-NK4K"),
        )
    }

    @Test
    fun `extracts token from json payload`() {
        assertEquals(
            "LKIC-HDDS-NK4K",
            AuthTokenExtractor.extract("""{"token":"LKICHDDSNK4K"}"""),
        )
    }

    @Test
    fun `extracts token from activation code json payload`() {
        assertEquals(
            "LKIC-HDDS-NK4K",
            AuthTokenExtractor.extract("""{"activation_code":"LKIC-HDDS-NK4K"}"""),
        )
    }

    @Test
    fun `returns null for blank token`() {
        assertNull(AuthTokenExtractor.extract("   "))
    }

    @Test
    fun `returns null for short hid noise`() {
        assertNull(AuthTokenExtractor.extract("L"))
        assertNull(AuthTokenExtractor.extract("LKIC"))
    }
}
