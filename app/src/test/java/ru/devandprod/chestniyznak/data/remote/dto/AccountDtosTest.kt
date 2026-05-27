package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `saas login request serializes device uid`() {
        val payload = SaasTokenLoginRequestDto(
            token = "login-token",
            deviceUid = "android-1",
        )

        val encoded = json.encodeToString(SaasTokenLoginRequestDto.serializer(), payload)

        assertTrue(encoded.contains("\"token\":\"login-token\""))
        assertTrue(encoded.contains("\"device_uid\":\"android-1\""))
    }

    @Test
    fun `tsd me envelope decodes user and plant context`() {
        val decoded = json.decodeFromString(
            ApiEnvelopeDto.serializer(TsdMeDto.serializer()),
            """
            {
              "data": {
                "user": {"id": "user-1", "display_name": "Оператор"},
                "context": {"plant_id": "plant-1", "device_id": "device-1"}
              }
            }
            """.trimIndent(),
        )

        assertEquals("Оператор", decoded.data?.user?.displayName)
        assertEquals("plant-1", decoded.data?.context?.plantId)
        assertEquals("device-1", decoded.data?.context?.deviceId)
    }

    @Test
    fun `tsd bootstrap envelope decodes app context`() {
        val decoded = json.decodeFromString(
            ApiEnvelopeDto.serializer(TsdBootstrapDto.serializer()),
            """
            {
              "data": {
                "authenticated": true,
                "user": {"id": "user-1", "display_name": "Оператор"},
                "supplier": {"id": "supplier-1", "name": "Поставщик"},
                "plant": {"id": "plant-1", "name": "Завод"},
                "device": {"id": "device-1", "device_uid": "android-1"},
                "context": {
                  "supplier_id": "supplier-1",
                  "plant_id": "plant-1",
                  "device_id": "device-1",
                  "client_device_id": "android-1"
                },
                "subscription": {"status": "active", "plan_code": "trial"}
              }
            }
            """.trimIndent(),
        )

        assertEquals("Оператор", decoded.data?.user?.displayName)
        assertEquals("Поставщик", decoded.data?.supplier?.name)
        assertEquals("Завод", decoded.data?.plant?.name)
        assertEquals("android-1", decoded.data?.context?.clientDeviceId)
        assertEquals("active", decoded.data?.subscription?.status)
    }
}
