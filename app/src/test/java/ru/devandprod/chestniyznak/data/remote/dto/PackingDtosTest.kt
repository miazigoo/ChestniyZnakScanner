package ru.devandprod.chestniyznak.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackingDtosTest {

    @Test
    fun `box dto uses new name field when present`() {
        val dto = OpenBoxResponseDto(
            ok = true,
            created = true,
            box = BoxDto(
                boxId = 4,
                name = "26-0666/6938",
                orderName = null,
                capacity = 60,
                filled = 11,
                allowDuplicateScans = false,
                isClosed = false,
                isEditMode = false,
            ),
        )

        val box = dto.toDomain().box

        assertEquals("26-0666/6938", box.orderName)
    }

    @Test
    fun `box dto keeps null name for empty box`() {
        val dto = OpenBoxResponseDto(
            ok = true,
            created = true,
            box = BoxDto(
                boxId = 4,
                name = null,
                orderName = null,
                capacity = 60,
                filled = 0,
                allowDuplicateScans = false,
                isClosed = false,
                isEditMode = false,
            ),
        )

        val box = dto.toDomain().box

        assertNull(box.orderName)
    }
}
