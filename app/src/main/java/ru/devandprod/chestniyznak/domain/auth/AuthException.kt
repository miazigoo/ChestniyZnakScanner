package ru.devandprod.chestniyznak.domain.auth

class AuthException(
    override val message: String,
) : IllegalStateException(message)
