package ru.devandprod.chestniyznak.domain.parser

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.ParsedMarkingCode

class ChestniyZnakParser @Inject constructor() {

    fun parse(input: String): ParsedMarkingCode {
        val (normalizedCode, nativeGs, escapedGs) = normalizeScannerInput(input)
        val warnings = mutableListOf<String>()

        if (normalizedCode.isEmpty()) {
            throw ChestniyZnakParseException("Пустой код")
        }

        if (!normalizedCode.startsWith("01")) {
            throw ChestniyZnakParseException("Код должен начинаться с AI 01")
        }

        if (normalizedCode.length < 18) {
            throw ChestniyZnakParseException("Код слишком короткий для 01 + GTIN + 21")
        }

        val gtin = normalizedCode.substring(2, 16)
        if (gtin.length != 14 || !gtin.all(Char::isDigit)) {
            throw ChestniyZnakParseException("Некорректный GTIN: $gtin")
        }

        if (normalizedCode.substring(16, 18) != "21") {
            throw ChestniyZnakParseException("После GTIN ожидается AI 21")
        }

        val tail = normalizedCode.substring(18)
        if (tail.isEmpty()) {
            throw ChestniyZnakParseException("После AI 21 нет серийного номера")
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
                warnings += "GS не пришел явно; разделитель восстановлен эвристикой после 20 символов serial"
            } else {
                serial = tail
                rest = ""
                warnings += "GS отсутствует; AI-хвост после serial не обнаружен"
            }
            val (parsedAiParts, parsedWarnings) = parseAiTailWithoutGs(rest)
            aiParts = parsedAiParts
            warnings += parsedWarnings
        }

        if (serial.isEmpty()) {
            throw ChestniyZnakParseException("Пустой serial после AI 21")
        }

        if (serial.length > AI21_MAX_SERIAL_LEN) {
            warnings += "Serial длиннее $AI21_MAX_SERIAL_LEN символов: ${serial.length}"
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
            .replace("\\x1d", GS)
            .replace("\\u001d", GS)

        normalized = compactBracketedAi(normalized)
        val nativeGs = normalized.contains(GS)
        val scannerEscapedGs = normalized.contains(ESC_GS_SEQ)
        normalized = normalized.replace(ESC_GS_SEQ, GS).trimEnd('\r', '\n')
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
                warnings += "Неизвестный AI-фрагмент: '$part'"
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
                warnings += "AI-хвост без GS не удалось разобрать полностью"
                break
            }

            if (ai in KNOWN_AI_FIXED_VALUE_LEN) {
                val valueLength = KNOWN_AI_FIXED_VALUE_LEN.getValue(ai)
                val valueEnd = 2 + valueLength
                if (remainder.length < valueEnd) {
                    aiParts[ai] = remainder.drop(2)
                    warnings += "AI $ai короче ожидаемой длины $valueLength"
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
            warnings += "Неизвестный AI $ai в хвосте без GS"
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

    private companion object {
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
