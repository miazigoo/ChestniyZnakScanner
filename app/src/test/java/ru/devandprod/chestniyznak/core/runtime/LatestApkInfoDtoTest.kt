package ru.devandprod.chestniyznak.core.runtime

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestApkInfoDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `no update response decodes from backend contract`() {
        val decoded = json.decodeFromString(
            LatestApkInfoDto.serializer(),
            """
            {
              "available": false,
              "version": "",
              "original_filename": "",
              "file_size": 0,
              "download_url": ""
            }
            """.trimIndent(),
        )

        assertFalse(decoded.available)
        assertEquals("", decoded.version)
        assertEquals("", decoded.originalFilename)
        assertEquals(0L, decoded.fileSize)
        assertEquals("", decoded.downloadUrl)
    }

    @Test
    fun `available update response decodes snake case fields`() {
        val decoded = json.decodeFromString(
            LatestApkInfoDto.serializer(),
            """
            {
              "available": true,
              "version": "2.3.4",
              "original_filename": "chz-tsd-2.3.4.apk",
              "file_size": 123456,
              "download_url": "https://downloads.example.test/chz-tsd-2.3.4.apk"
            }
            """.trimIndent(),
        )

        assertTrue(decoded.available)
        assertEquals("2.3.4", decoded.version)
        assertEquals("chz-tsd-2.3.4.apk", decoded.originalFilename)
        assertEquals(123456L, decoded.fileSize)
        assertEquals("https://downloads.example.test/chz-tsd-2.3.4.apk", decoded.downloadUrl)
    }
}
