package ru.devandprod.chestniyznak.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrinterDtosTest {

    @Test
    fun `client printer selection maps selected printer`() {
        val dto = ClientPrinterSelectionResponseDto(
            ok = true,
            deviceId = "M3SL20",
            selectedPrinterId = 7,
            selectedPrinter = ClientPrinterDto(
                id = 7,
                name = "Printer 7",
                ipAddress = "172.16.8.121",
                section = "Line 1",
                isActive = true,
            ),
            printers = listOf(
                ClientPrinterDto(
                    id = 7,
                    name = "Printer 7",
                    ipAddress = "172.16.8.121",
                    section = "Line 1",
                    isActive = true,
                ),
            ),
        )

        val result = dto.toDomain()

        assertEquals("M3SL20", result.deviceId)
        assertEquals(7L, result.selectedPrinterId)
        assertEquals("Printer 7", result.selectedPrinter?.name)
        assertNotNull(result.printers.firstOrNull())
    }
}
