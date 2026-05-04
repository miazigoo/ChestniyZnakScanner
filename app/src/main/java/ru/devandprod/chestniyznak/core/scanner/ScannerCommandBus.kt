package ru.devandprod.chestniyznak.core.scanner

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface ScannerCommand {
    data object SwitchToCamera : ScannerCommand
    data object SwitchToTsd : ScannerCommand
    data object OpenBox : ScannerCommand
}

object ScannerCommandBus {
    private val commands = MutableSharedFlow<ScannerCommand>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun commands(): SharedFlow<ScannerCommand> = commands.asSharedFlow()

    fun send(command: ScannerCommand) {
        commands.tryEmit(command)
    }
}
