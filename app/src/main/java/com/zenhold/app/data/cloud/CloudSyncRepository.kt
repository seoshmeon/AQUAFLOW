package com.zenhold.app.data.cloud

import com.zenhold.app.domain.repository.RecordRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CloudSyncState(
    val hasProfile: Boolean = false,
    val isBusy: Boolean = false,
    val linked: Boolean = false,
    val telegramName: String? = null,
    val linkCode: String? = null,
    val linkCodeExpiresAt: Long = 0L,
    val syncedRecords: Int = 0,
    val message: String? = null,
)

@Singleton
class CloudSyncRepository @Inject constructor(
    private val api: AquaflowCloudApi,
    private val credentials: CloudCredentialStore,
    private val records: RecordRepository,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(CloudSyncState(hasProfile = credentials.read() != null))
    val state: StateFlow<CloudSyncState> = _state.asStateFlow()

    suspend fun createLinkCode() = mutex.withLock {
        runOperation {
            val account = ensureCredentials()
            val synced = syncAll(account)
            val code = api.createLinkCode(account.token)
            _state.value.copy(
                hasProfile = true,
                linkCode = code.value,
                linkCodeExpiresAt = System.currentTimeMillis() + code.expiresInSeconds * 1_000L,
                syncedRecords = synced,
                message = "Код готов. Отправьте его боту в течение 10 минут.",
            )
        }
    }

    suspend fun syncNow() = mutex.withLock {
        runOperation {
            val account = ensureCredentials()
            val count = syncAll(account)
            val status = api.linkStatus(account.token)
            statusState(status).copy(
                hasProfile = true,
                syncedRecords = count,
                message = "Синхронизировано подходов: $count",
            )
        }
    }

    suspend fun refreshStatus() = mutex.withLock {
        val account = credentials.read() ?: return@withLock
        runOperation {
            statusState(api.linkStatus(account.token)).copy(hasProfile = true)
        }
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private suspend fun ensureCredentials(): CloudCredentials {
        credentials.read()?.let { return it }
        return api.createAnonymousProfile().also(credentials::save)
    }

    private suspend fun syncAll(account: CloudCredentials): Int {
        val all = records.getAllRecords()
        all.chunked(100).forEach { api.syncRecords(account.token, it) }
        return all.size
    }

    private fun statusState(status: LinkStatus): CloudSyncState = _state.value.copy(
        linked = status.linked,
        telegramName = status.telegramUsername?.let { "@$it" } ?: status.telegramFirstName,
        linkCode = if (status.linked) null else _state.value.linkCode,
    )

    private suspend fun runOperation(block: suspend () -> CloudSyncState) {
        _state.value = _state.value.copy(isBusy = true, message = null)
        _state.value = runCatching { block() }
            .getOrElse { error -> _state.value.copy(message = error.message ?: "Не удалось подключиться") }
            .copy(isBusy = false)
    }
}
