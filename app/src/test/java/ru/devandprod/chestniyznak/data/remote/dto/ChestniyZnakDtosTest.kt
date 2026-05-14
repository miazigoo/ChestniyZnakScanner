package ru.devandprod.chestniyznak.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.model.DefectRemovedBox

class ChestniyZnakDtosTest {

    @Test
    fun `verify exists maps top level order name`() {
        val dto = VerifyExistsResponseDto(
            ok = true,
            exists = true,
            status = "OK",
            message = "Код найден в базе",
            orderName = "26-0666/6938",
        )

        val result = dto.toDomain()

        assertEquals(VerificationStatus.OK, result.status)
        assertEquals("26-0666/6938", result.orderName)
        assertNull(result.code)
    }

    @Test
    fun `verify response maps code order name`() {
        val dto = VerifyResponseDto(
            status = "OK",
            message = "Код найден в базе",
            code = RemoteCodeDto(
                id = 10,
                gtin = "04646151697384",
                serial = "SERIAL123",
                visibleCode = "010464615169738421SERIAL123",
                orderNumber = "26-0666/6938",
                orderName = "Заказ 26-0666/6938",
            ),
        )

        val result = dto.toDomain()

        assertEquals("Заказ 26-0666/6938", result.orderName)
        assertEquals("Заказ 26-0666/6938", result.code?.orderName)
    }

    @Test
    fun `verify exists maps duplicate scan status`() {
        val dto = VerifyExistsResponseDto(
            ok = false,
            exists = false,
            status = "DUPLICATE_SCAN",
            message = "Код уже сканировали ранее",
            orderName = "26-0666/6938",
            deviceName = "Device A",
        )

        val result = dto.toDomain()

        assertEquals(VerificationStatus.DUPLICATE_SCAN, result.status)
        assertEquals("Код уже сканировали ранее", result.message)
        assertEquals("26-0666/6938", result.orderName)
        assertEquals("Device A", result.deviceName)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `defect response maps removed box and verify payload`() {
        val dto = DefectResponseDto(
            ok = true,
            reasonCode = "defect_marked",
            error = "Код отправлен в брак",
            verify = VerifyResponseDto(
                status = "OK",
                message = "Код найден в базе",
                code = RemoteCodeDto(
                    id = 77,
                    gtin = "04646151697384",
                    serial = "SERIAL123",
                    visibleCode = "010464615169738421SERIAL123",
                    orderNumber = "26-0666/6938",
                    orderName = "26-0666/6938",
                    deviceName = "Device A",
                ),
            ),
            removedFromBox = DefectRemovedBoxDto(
                boxId = 15,
                sscc = "046306261900000012",
                filled = 0,
            ),
        )

        val result = dto.toDomain()

        assertTrue(result.ok)
        assertEquals("defect_marked", result.reasonCode)
        assertEquals("26-0666/6938", result.verify?.orderName)
        assertEquals("Device A", result.verify?.deviceName)
        assertEquals(
            DefectRemovedBox(
                boxId = 15,
                sscc = "046306261900000012",
                filled = 0,
            ),
            result.removedFromBox,
        )
    }
}
