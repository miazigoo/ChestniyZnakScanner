package ru.devandprod.chestniyznak.domain.auth

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object AuthTokenExtractor {

    fun extract(rawValue: String): String? {
        val normalized = rawValue.trim().trim('"', '\'')
        if (normalized.isBlank()) return null

        extractJsonToken(normalized)?.let { return it }
        extractQueryToken(normalized)?.let { return it }

        return normalized.takeIf(String::isNotBlank)
    }

    private fun extractJsonToken(value: String): String? {
        val match = TOKEN_JSON_REGEX.find(value) ?: return null
        return match.groupValues.getOrNull(1)
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }

    private fun extractQueryToken(value: String): String? {
        TOKEN_QUERY_REGEX.find(value)?.groupValues?.getOrNull(1)?.let { encoded ->
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8)
                .trim()
                .takeIf(String::isNotBlank)
        }
        return null
    }

    private val TOKEN_JSON_REGEX = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"")
    private val TOKEN_QUERY_REGEX = Regex("(?:^|[?&#\\s])token=([^&#\\s]+)")
}
