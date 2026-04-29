package ru.devandprod.chestniyznak.domain.parser

class ChestniyZnakParseException(
    override val message: String,
) : IllegalArgumentException(message)
