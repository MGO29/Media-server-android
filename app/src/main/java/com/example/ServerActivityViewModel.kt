package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

data class ServerState(
    val isRunning: Boolean,
    val serverUrl: String?,
    val activeClientsCount: Int
)

data class ServerActivityUiState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val selectedFilter: String = "ALL",
    val searchQuery: String = "",
    val isPaused: Boolean = false,
    val isServerRunning: Boolean = false,
    val serverUrl: String? = null,
    val activeClientsCount: Int = 0
)

class ServerActivityViewModel : ViewModel() {

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val serverStateFlow: Flow<ServerState> = combine(
        MediaServerService.isRunning,
        MediaServerService.serverUrl,
        MediaServerService.connectedClients
    ) { isRunning, url, clients ->
        ServerState(
            isRunning = isRunning,
            serverUrl = url,
            activeClientsCount = clients.size
        )
    }

    // Real-time flow streaming logs and server state directly to the UI
    val uiState: StateFlow<ServerActivityUiState> = combine(
        MediaServerService.systemLogs,
        serverStateFlow,
        _selectedFilter,
        _searchQuery,
        _isPaused
    ) { logs, serverState, filter, query, paused ->
        val currentLogs = if (paused) emptyList() else logs
        val filtered = currentLogs.filter { log ->
            val matchesFilter = when (filter) {
                "ALL" -> true
                else -> log.level.equals(filter, ignoreCase = true)
            }
            val matchesQuery = if (query.isBlank()) true else {
                log.message.contains(query, ignoreCase = true) ||
                log.level.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }

        ServerActivityUiState(
            logs = currentLogs,
            filteredLogs = filtered,
            selectedFilter = filter,
            searchQuery = query,
            isPaused = paused,
            isServerRunning = serverState.isRunning,
            serverUrl = serverState.serverUrl,
            activeClientsCount = serverState.activeClientsCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ServerActivityUiState()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun clearLogs() {
        MediaServerService.clearLogs()
    }
}
