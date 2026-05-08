package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackingDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `open box request serializes count_in_packing`() {
        val payload = OpenBoxRequestDto(
            deviceId = "tsd-1",
            countInPacking = false,
        )

        val encoded = json.encodeToString(OpenBoxRequestDto.serializer(), payload)

        assertTrue(encoded.contains("\"count_in_packing\":false"))
        assertTrue(encoded.contains("\"device_id\":\"tsd-1\""))
    }

    @Test
    fun `current box response maps count_in_packing to domain`() {
        val response = CurrentBoxResponseDto(
            boxId = 10,
            name = null,
            orderName = "26-0666/6938",
            sscc = "046306261900000012",
            capacity = 60,
            filled = 5,
            countInPacking = false,
            allowDuplicateScans = false,
            isClosed = false,
            isEditMode = false,
            items = emptyList(),
        )

        val detail = response.toDomain()

        assertEquals(10L, detail.box.boxId)
        assertFalse(detail.box.countInPacking)
        assertEquals("26-0666/6938", detail.box.orderName)
    }
}
