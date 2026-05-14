package ru.devandprod.chestniyznak.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthTokenExtractorTest {

    @Test
    fun `returns raw token when scanner sends plain string`() {
        assertEquals("abc123token", AuthTokenExtractor.extract("abc123token"))
    }

    @Test
    fun `extracts token from query string`() {
        assertEquals(
            "tablet-user-token",
            AuthTokenExtractor.extract("https://example.test/login?token=tablet-user-token"),
        )
    }

    @Test
    fun `extracts token from json payload`() {
        assertEquals(
            "scanner-token",
            AuthTokenExtractor.extract("""{"token":"scanner-token"}"""),
        )
    }

    @Test
    fun `returns null for blank token`() {
        assertNull(AuthTokenExtractor.extract("   "))
    }
}
