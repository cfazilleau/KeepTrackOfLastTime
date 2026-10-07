package dev.cfaz.timeclicker.ui.settings

import android.database.SQLException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cfaz.timeclicker.TimeClickerApplication
import dev.cfaz.timeclicker.data.AppSettings
import dev.cfaz.timeclicker.data.BackupRepository
import dev.cfaz.timeclicker.data.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** The outcome of an export or import, shown as a snackbar. */
sealed interface BackupMessage {
    data object Exported : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val tiles: Int) : BackupMessage
    data object ImportFailed : BackupMessage
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val busyState = MutableStateFlow(false)

    /** An export or import is running. */
    val busy: StateFlow<Boolean> = busyState.asStateFlow()

    private val messageEvents = Channel<BackupMessage>(Channel.BUFFERED)
    val messages: Flow<BackupMessage> = messageEvents.receiveAsFlow()

    fun update(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

    fun export(uri: Uri) = runBackup(BackupMessage.ExportFailed) {
        backupRepository.export(uri)
        BackupMessage.Exported
    }

    fun import(uri: Uri) = runBackup(BackupMessage.ImportFailed) {
        BackupMessage.Imported(backupRepository.import(uri).tiles)
    }

    private fun runBackup(failure: BackupMessage, block: suspend () -> BackupMessage) {
        viewModelScope.launch {
            busyState.value = true
            val message = try {
                block()
            } catch (e: IOException) {
                failure
            } catch (e: SecurityException) {
                failure
            } catch (e: SQLException) {
                // E.g. a hand-edited backup with duplicate ids; the import transaction is rolled back.
                failure
            } finally {
                busyState.value = false
            }
            messageEvents.send(message)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TimeClickerApplication
                SettingsViewModel(app.container.settingsRepository, app.container.backupRepository)
            }
        }
    }
}
