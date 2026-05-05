package ru.devandprod.chestniyznak.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.devandprod.chestniyznak.domain.model.VerificationStatus

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
}
