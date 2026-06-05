package ru.devandprod.chestniyznak.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.data.remote.api.PackingApi
import ru.devandprod.chestniyznak.data.remote.auth.RemoteAuthRepository
import ru.devandprod.chestniyznak.data.remote.auth.RemoteErrorParser
import ru.devandprod.chestniyznak.data.remote.dto.CloseBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.ClientPrinterSelectionRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.CountInPackingRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.CurrentBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.EditBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.OpenBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.OpenBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.PackageLabelPrintRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.PackageLabelPrintResultRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.RemoveBoxItemRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.ScanToBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.ScanToBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.toDomain
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackageLabelPrintResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxActionResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.model.PrintJob
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

@Singleton
class RemotePackingRepository @Inject constructor(
    private val packingApi: PackingApi,
    private val authRepository: RemoteAuthRepository,
    private val errorParser: RemoteErrorParser,
    private val strings: AppStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PackingRepository {
    override suspend fun getClientPrinterSelection(deviceId: String): ClientPrinterSelection = withContext(ioDispatcher) {
        val response = runCatching { packingApi.clientPrinters(deviceId = deviceId) }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.printer_load_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun setClientPrinterSelection(
        deviceId: String,
        printerId: Long,
    ): ClientPrinterSelection = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.setClientPrinterSelection(
                ClientPrinterSelectionRequestDto(
                    deviceId = deviceId,
                    printerId = printerId,
                ),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.printer_save_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun listBoxes(
        status: String,
        query: String,
        limit: Int,
        offset: Int,
    ): PackingBoxPage = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.listBoxes(
                query = query,
                status = status,
                limit = limit,
                offset = offset,
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.boxes_load_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun getCurrentBox(): PackingBoxDetail? = withContext(ioDispatcher) {
        val response = runCatching { packingApi.currentBox() }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_get_current_failed))
        }

        when {
            response.isSuccessful && response.body() != null -> response.body()!!.toDomain()
            response.code() == 404 -> null
            response.code() == 401 || response.code() == 403 -> {
                authRepository.invalidateSession()
                throw RuntimeException(strings.get(R.string.common_session_expired))
            }
            else -> throw RuntimeException(errorParser.message(response))
        }
    }

    override suspend fun getBox(boxId: Long): PackingBoxDetail = withContext(ioDispatcher) {
        val response = runCatching { packingApi.getBox(boxId) }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_get_box_failed))
        }
        mapResponse(response)?.toDomain()
            ?.box
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun openBox(
        deviceId: String,
        countInPacking: Boolean,
        orderId: String?,
        orderLineId: String?,
        codeValue: String?,
        sscc: String?,
    ): OpenPackingBoxResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.openBox(
                OpenBoxRequestDto(
                    deviceId = deviceId,
                    countInPacking = countInPacking,
                    orderId = orderId,
                    orderLineId = orderLineId,
                    codeValue = codeValue,
                    sscc = sscc,
                ),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_open_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun setBoxCountInPacking(
        boxId: Long,
        countInPacking: Boolean,
    ): PackingBoxActionResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.setBoxCountInPacking(
                boxId = boxId,
                request = CountInPackingRequestDto(countInPacking = countInPacking),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_count_mode_update_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun openBoxEdit(boxId: Long, reason: String): PackingBoxActionResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.openBoxEdit(
                boxId = boxId,
                request = EditBoxRequestDto(reason = reason),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.box_detail_edit_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun removeBoxItem(boxId: Long, itemId: Long): PackingBoxActionResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.removeBoxItem(
                boxId = boxId,
                request = RemoveBoxItemRequestDto(itemId = itemId),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_remove_code_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun clearBox(boxId: Long): PackingBoxActionResult = withContext(ioDispatcher) {
        val response = runCatching { packingApi.clearBox(boxId) }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_clear_box_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun deleteEmptyBox(boxId: Long): PackingBoxActionResult = withContext(ioDispatcher) {
        val response = runCatching { packingApi.deleteEmptyBox(boxId) }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_delete_empty_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun scanCodeToBox(
        boxId: Long,
        rawCode: String,
        scannerId: String,
    ): PackingScanResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.scanToBox(
                boxId = boxId,
                request = ScanToBoxRequestDto(
                    code = rawCode,
                    scannerId = scannerId,
                ),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_add_code_failed))
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun closeBox(boxId: Long, deviceId: String): ClosePackingBoxResult = withContext(ioDispatcher) {
        val response = runCatching { packingApi.closeBox(boxId, deviceId) }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.packing_close_box_failed))
        }

        mapResponse(response)
            ?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun printBoxLabel(boxId: Long, deviceId: String): PackageLabelPrintResult = withContext(ioDispatcher) {
        val selection = selectionWithSinglePrinterAutoselect(deviceId)
        val prepared = prepareBoxLabel(
            boxId = boxId,
            deviceId = deviceId,
            printerId = selection.selectedPrinterId,
        )
        val job = prepared.printJob ?: return@withContext prepared
        val (printOk, printError) = sendPrintJob(job)
        val printerId = job.printer?.id
        runCatching {
            reportBoxLabelPrint(
                boxId = boxId,
                deviceId = deviceId,
                printerId = printerId,
                printOk = printOk,
                printError = printError,
            )
        }.getOrElse { error ->
            if (printOk) {
                throw RuntimeException(error.message ?: strings.get(R.string.printer_report_failed))
            }
            PackageLabelPrintResult(
                ok = false,
                reasonCode = "label_print_failed",
                printStatus = "failed",
                printOk = false,
                printErrorCode = "printer_job_failed",
                printError = printError.ifBlank {
                    error.message ?: strings.get(R.string.printer_report_failed)
                },
                printer = job.printer,
                printJob = job,
                box = prepared.box,
            )
        }
    }

    private suspend fun selectionWithSinglePrinterAutoselect(deviceId: String): ClientPrinterSelection {
        val selection = getClientPrinterSelection(deviceId)
        return if (selection.selectedPrinterId == null && selection.printers.size == 1) {
            setClientPrinterSelection(deviceId, selection.printers.first().id)
        } else {
            selection
        }
    }

    private suspend fun prepareBoxLabel(
        boxId: Long,
        deviceId: String,
        printerId: Long?,
    ): PackageLabelPrintResult {
        val response = runCatching {
            packingApi.printBoxLabel(
                boxId = boxId,
                request = PackageLabelPrintRequestDto(
                    deviceId = deviceId,
                    printerId = printerId,
                ),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.printer_print_failed))
        }

        return mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    private suspend fun reportBoxLabelPrint(
        boxId: Long,
        deviceId: String,
        printerId: Long?,
        printOk: Boolean,
        printError: String,
    ): PackageLabelPrintResult {
        val response = runCatching {
            packingApi.reportBoxLabelPrintResult(
                boxId = boxId,
                request = PackageLabelPrintResultRequestDto(
                    deviceId = deviceId,
                    printerId = printerId,
                    printOk = printOk,
                    printError = printError,
                ),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.printer_report_failed))
        }

        return mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    private fun sendPrintJob(job: PrintJob): Pair<Boolean, String> {
        if (job.transport != "raw_tcp") {
            return false to strings.get(R.string.printer_unsupported_transport, job.transport.ifBlank { "-" })
        }
        val printer = job.printer ?: return false to strings.get(R.string.printer_job_missing_printer)
        if (job.payload.isBlank()) {
            return false to strings.get(R.string.printer_empty_job)
        }
        return runCatching {
            val data = job.payload.toByteArray(Charset.forName(job.encoding.ifBlank { "utf-8" }))
            Socket().use { socket ->
                socket.connect(InetSocketAddress(printer.ipAddress, printer.port), PRINT_TIMEOUT_MS)
                socket.soTimeout = PRINT_TIMEOUT_MS
                socket.getOutputStream().use { output ->
                    output.write(data)
                    output.flush()
                }
            }
            true to ""
        }.getOrElse { false to (it.message ?: it.javaClass.simpleName) }
    }

    private fun <T> mapResponse(response: retrofit2.Response<T>): T? {
        if (response.code() == 401 || response.code() == 403) {
            authRepository.invalidateSession()
            throw RuntimeException(strings.get(R.string.common_session_expired))
        }
        return if (response.isSuccessful) response.body() else null
    }

    private companion object {
        const val PRINT_TIMEOUT_MS = 5_000
    }
}
