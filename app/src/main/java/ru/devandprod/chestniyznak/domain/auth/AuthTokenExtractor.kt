package ru.devandprod.chestniyznak.domain.auth

import java.net.URLDecoder

object AuthTokenExtractor {

    fun extract(rawValue: String): String? {
        val normalized = rawValue.trim().trim('"', '\'')
        if (normalized.isBlank()) return null

        extractJsonToken(normalized)?.let { return it }
        extractQueryToken(normalized)?.let { return it }

        return normalizeActivationToken(normalized)
    }

    private fun extractJsonToken(value: String): String? {
        val match = TOKEN_JSON_REGEX.find(value) ?: return null
        return match.groupValues.getOrNull(1)?.let(::normalizeActivationToken)
    }

    private fun extractQueryToken(value: String): String? {
        TOKEN_QUERY_REGEX.find(value)?.groupValues?.getOrNull(1)?.let { encoded ->
            return URLDecoder.decode(encoded, Charsets.UTF_8.name())
                .let(::normalizeActivationToken)
        }
        return null
    }

    private fun normalizeActivationToken(value: String): String? {
        val token = value.trim().uppercase()
        if (TOKEN_PATTERN.matches(token)) return token
        val compact = token.replace("-", "")
        if (COMPACT_TOKEN_PATTERN.matches(compact)) {
            return "${compact.substring(0, 4)}-${compact.substring(4, 8)}-${compact.substring(8, 12)}"
        }
        return null
    }

    private val TOKEN_JSON_REGEX = Regex("\"(?:token|activation_code|app_token)\"\\s*:\\s*\"([^\"]+)\"")
    private val TOKEN_QUERY_REGEX = Regex("(?:^|[?&#\\s])token=([^&#\\s]+)")
    private val TOKEN_PATTERN = Regex("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")
    private val COMPACT_TOKEN_PATTERN = Regex("[A-Z0-9]{12}")
}
