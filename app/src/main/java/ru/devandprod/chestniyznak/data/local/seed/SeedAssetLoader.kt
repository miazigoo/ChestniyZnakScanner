package ru.devandprod.chestniyznak.data.local.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

class SeedAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    fun loadCodes(): List<SeedMarkingCodeAsset> {
        val rawJson = context.assets
            .open(ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }
        return json.decodeFromString(rawJson)
    }

    private companion object {
        const val ASSET_PATH = "seed/chestniy_znak_codes.json"
    }
}
