package ru.devandprod.chestniyznak.domain.parser

import javax.inject.Inject
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.model.ParsedMarkingCode

class ChestniyZnakParser private constructor(
    private val textProvider: (Int, Array<out Any>) -> String,
) {
    @Inject
    constructor(strings: AppStringProvider) : this({ resId, args -> strings.get(resId, *args) })

    constructor() : this(::fallbackText)

    fun parse(input: String): ParsedMarkingCode {
        val (normalizedCode, nativeGs, escapedGs) = normalizeScannerInput(input)
        val warnings = mutableListOf<String>()

        if (normalizedCode.isEmpty()) {
            throw ChestniyZnakParseException(text(R.string.parse_empty_code))
        }

        if (!normalizedCode.startsWith("01")) {
            throw ChestniyZnakParseException(text(R.string.parse_must_start_ai01))
        }

        if (normalizedCode.length < 18) {
            throw ChestniyZnakParseException(text(R.string.parse_too_short))
        }

        val gtin = normalizedCode.substring(2, 16)
        if (gtin.length != 14 || !gtin.all(Char::isDigit)) {
            throw ChestniyZnakParseException(text(R.string.parse_invalid_gtin, gtin))
        }

        if (normalizedCode.substring(16, 18) != "21") {
            throw ChestniyZnakParseException(text(R.string.parse_expected_ai21))
        }

        val tail = normalizedCode.substring(18)
        if (tail.isEmpty()) {
            throw ChestniyZnakParseException(text(R.string.parse_missing_serial_after_ai21))
        }

        val serial: String
        val aiParts: Map<String, String>
        var gsRestored = false

        if (tail.contains(GS)) {
            val split = tail.split(GS, limit = 2)
            serial = split[0]
            val rest = split.getOrElse(1) { "" }
            val (parsedAiParts, parsedWarnings) = parseAiTailWithGs(rest)
            aiParts = parsedAiParts
            warnings += parsedWarnings
        } else {
            val rest: String
            if (tail.length > AI21_MAX_SERIAL_LEN) {
                serial = tail.take(AI21_MAX_SERIAL_LEN)
                rest = tail.drop(AI21_MAX_SERIAL_LEN)
                gsRestored = true
                warnings += text(R.string.parse_gs_restored_warning)
            } else {
                serial = tail
                rest = ""
                warnings += text(R.string.parse_gs_missing_warning)
            }
            val (parsedAiParts, parsedWarnings) = parseAiTailWithoutGs(rest)
            aiParts = parsedAiParts
            warnings += parsedWarnings
        }

        if (serial.isEmpty()) {
            throw ChestniyZnakParseException(text(R.string.parse_empty_serial_after_ai21))
        }

        if (serial.length > AI21_MAX_SERIAL_LEN) {
            warnings += text(R.string.parse_serial_too_long, AI21_MAX_SERIAL_LEN, serial.length)
        }

        val rawCode = buildRawCode(gtin = gtin, serial = serial, aiParts = aiParts)

        return ParsedMarkingCode(
            gtin = gtin,
            serial = serial,
            aiParts = aiParts,
            rawCode = rawCode,
            visibleCode = rawCode.replace(GS, "<GS>"),
            scannerGsNative = nativeGs,
            gsRestored = gsRestored || escapedGs,
            warnings = warnings,
        )
    }

    private fun normalizeScannerInput(code: String): Triple<String, Boolean, Boolean> {
        var normalized = code
            .replace("<GS>", GS)
            .replace("[GS]", GS)
            .replace("{GS}", GS)
            .replace("\\x1d", GS)
            .replace("x1d", GS, ignoreCase = true)
            .replace("\\u001d", GS)
            .replace("\\035", GS)

        normalized = compactBracketedAi(normalized)
        val nativeGs = normalized.contains(GS)
        val scannerEscapedGs = normalized.contains(ESC_GS_SEQ)
        normalized = normalized.replace(ESC_GS_SEQ, GS).trim(' ', '\t', '\r', '\n')
        while (normalized.startsWith(GS)) {
            normalized = normalized.drop(1)
        }
        return Triple(normalized, nativeGs, scannerEscapedGs)
    }

    private fun compactBracketedAi(code: String): String = code
        .replace("(01)", "01")
        .replace("(21)", "21")
        .replace("(91)", "${GS}91")
        .replace("(92)", "${GS}92")
        .replace("(93)", "${GS}93")

    private fun parseAiTailWithGs(rest: String): Pair<Map<String, String>, List<String>> {
        val aiParts = linkedMapOf<String, String>()
        val warnings = mutableListOf<String>()

        rest.split(GS).forEach { part ->
            if (part.isEmpty()) return@forEach
            if (part.length < 2 || !part.take(2).all(Char::isDigit)) {
                val key = "unknown_${aiParts.size + 1}"
                aiParts[key] = part
                warnings += text(R.string.parse_unknown_ai_fragment, part)
            } else {
                aiParts[part.take(2)] = part.drop(2)
            }
        }
        return aiParts to warnings
    }

    private fun parseAiTailWithoutGs(rest: String): Pair<Map<String, String>, List<String>> {
        val aiParts = linkedMapOf<String, String>()
        val warnings = mutableListOf<String>()
        var remainder = rest

        while (remainder.isNotEmpty()) {
            val ai = remainder.take(2)
            if (ai.length < 2 || !ai.all(Char::isDigit)) {
                aiParts["unknown_tail"] = remainder
                warnings += text(R.string.parse_tail_without_gs_unparsed)
                break
            }

            if (ai in KNOWN_AI_FIXED_VALUE_LEN) {
                val valueLength = KNOWN_AI_FIXED_VALUE_LEN.getValue(ai)
                val valueEnd = 2 + valueLength
                if (remainder.length < valueEnd) {
                    aiParts[ai] = remainder.drop(2)
                    warnings += text(R.string.parse_ai_shorter_than_expected, ai, valueLength)
                    break
                }
                aiParts[ai] = remainder.substring(2, valueEnd)
                remainder = remainder.drop(valueEnd)
                continue
            }

            if (ai in KNOWN_AI_VARIABLE_TO_END) {
                aiParts[ai] = remainder.drop(2)
                break
            }

            aiParts["unknown_tail"] = remainder
            warnings += text(R.string.parse_unknown_ai_without_gs, ai)
            break
        }

        return aiParts to warnings
    }

    private fun buildRawCode(gtin: String, serial: String, aiParts: Map<String, String>): String {
        val prefix = "01$gtin" + "21$serial"
        if (aiParts.isEmpty()) {
            return prefix
        }

        val tailParts = aiParts.map { (ai, value) ->
            if (ai == "unknown_tail" || ai.startsWith("unknown_")) {
                value
            } else {
                ai + value
            }
        }
        return prefix + GS + tailParts.joinToString(separator = GS)
    }

    private fun text(resId: Int, vararg args: Any): String = textProvider(resId, args)

    private companion object {
        fun fallbackText(resId: Int, args: Array<out Any>): String = when (resId) {
            R.string.parse_empty_code -> "Пустой код"
            R.string.parse_must_start_ai01 -> "Код должен начинаться с AI 01"
            R.string.parse_too_short -> "Код слишком короткий для 01 + GTIN + 21"
            R.string.parse_invalid_gtin -> "Некорректный GTIN: ${args[0]}"
            R.string.parse_expected_ai21 -> "После GTIN ожидается AI 21"
            R.string.parse_missing_serial_after_ai21 -> "После AI 21 нет серийного номера"
            R.string.parse_gs_restored_warning -> "GS не пришел явно; разделитель восстановлен эвристикой после 20 символов serial"
            R.string.parse_gs_missing_warning -> "GS отсутствует; AI-хвост после serial не обнаружен"
            R.string.parse_empty_serial_after_ai21 -> "Пустой serial после AI 21"
            R.string.parse_serial_too_long -> "Serial длиннее ${args[0]} символов: ${args[1]}"
            R.string.parse_unknown_ai_fragment -> "Неизвестный AI-фрагмент: ${args[0]}"
            R.string.parse_tail_without_gs_unparsed -> "AI-хвост без GS не удалось разобрать полностью"
            R.string.parse_ai_shorter_than_expected -> "AI ${args[0]} короче ожидаемой длины ${args[1]}"
            R.string.parse_unknown_ai_without_gs -> "Неизвестный AI ${args[0]} в хвосте без GS"
            else -> resId.toString()
        }

        const val GS = "\u001D"
        const val ESC_GS_SEQ = "\u001B`\u001Bb\u001Bi"
        const val AI21_MAX_SERIAL_LEN = 20
        val KNOWN_AI_FIXED_VALUE_LEN = mapOf(
            "91" to 4,
            "93" to 4,
        )
        val KNOWN_AI_VARIABLE_TO_END = setOf("92")
    }
}
