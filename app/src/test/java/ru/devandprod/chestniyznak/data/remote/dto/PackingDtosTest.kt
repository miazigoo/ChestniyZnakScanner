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
    fun `open box request serializes explicit order selection`() {
        val payload = OpenBoxRequestDto(
            deviceId = "tsd-1",
            orderId = "order-uuid",
            orderLineId = "line-uuid",
            codeValue = "BOX-001",
        )

        val encoded = json.encodeToString(OpenBoxRequestDto.serializer(), payload)

        assertTrue(encoded.contains("\"order_id\":\"order-uuid\""))
        assertTrue(encoded.contains("\"order_line_id\":\"line-uuid\""))
        assertTrue(encoded.contains("\"code_value\":\"BOX-001\""))
    }

    @Test
    fun `current box response maps count_in_packing to domain`() {
        val response = CurrentBoxResponseDto(
            boxId = 10,
            orderUuid = "1b8f62c3-8c8f-48b0-a28c-8169b9e2af16",
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
        assertEquals("1b8f62c3-8c8f-48b0-a28c-8169b9e2af16", detail.box.orderUuid)
        assertFalse(detail.box.countInPacking)
        assertEquals("26-0666/6938", detail.box.orderName)
    }

    @Test
    fun `close box response maps simple server result`() {
        val response = json.decodeFromString(
            CloseBoxResponseDto.serializer(),
            """
            {
              "ok": true,
              "reason_code": "ok",
              "box": {
                "box_id": 10,
                "capacity": 20,
                "filled": 20,
                "allow_duplicate_scans": false,
                "is_closed": true,
                "is_edit_mode": false
              }
            }
            """.trimIndent(),
        )

        val result = response.toDomain()

        assertTrue(result.ok)
        assertEquals(10L, result.box.boxId)
        assertTrue(result.box.isClosed)
    }
}
