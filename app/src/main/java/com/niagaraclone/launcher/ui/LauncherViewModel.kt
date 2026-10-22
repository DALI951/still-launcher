package com.niagaraclone.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.data.AppRepository
import com.niagaraclone.launcher.data.AppUpdateRepository
import com.niagaraclone.launcher.data.InstallResult
import com.niagaraclone.launcher.data.LauncherPermissionsState
import com.niagaraclone.launcher.data.PermissionsManager
import com.niagaraclone.launcher.data.PreferencesRepository
import com.niagaraclone.launcher.data.ReleaseInfo
import com.niagaraclone.launcher.data.SystemStatusProvider
import com.niagaraclone.launcher.data.UpdateDownloadState
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.SystemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(val releaseInfo: ReleaseInfo) : UpdateUiState()
    data class UpToDate(val latestRelease: ReleaseInfo? = null) : UpdateUiState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedMb: Float,
        val totalMb: Float,
        val releaseInfo: ReleaseInfo
    ) : UpdateUiState()
    data class ReadyToInstall(val apkFile: File, val releaseInfo: ReleaseInfo) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

/**
 * UI State representing the complete launcher screen.
 */
data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val favoriteApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val systemStatus: SystemStatus = SystemStatus(),
    val settings: LauncherSettings = LauncherSettings(),
    val searchQuery: String = "",
    val filteredApps: List<AppInfo> = emptyList(),
    val selectedSectionSymbol: Char? = null,
    val isScrubbingWave: Boolean = false,
    val isAllAppsVisible: Boolean = false,
    val isSearchActive: Boolean = false,
    val isManageAppsActive: Boolean = false,
    val isUpdateSheetActive: Boolean = false,
    val isSettingsSheetActive: Boolean = false,
    val isDefaultPromptDialogVisible: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val alphabetSymbols: List<Char> = listOf('★', '#') + ('A'..'Z').toList() + listOf('⚙'),
    val availableSymbols: Set<Char> = emptySet(),
    val letterSectionIndices: Map<Char, Int> = emptyMap(),
    val updateState: UpdateUiState = UpdateUiState.Idle,
    val permissionsState: LauncherPermissionsState = LauncherPermissionsState()
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val systemStatusProvider = SystemStatusProvider(application)
    val updateRepository = AppUpdateRepository(application)
    val permissionsManager = PermissionsManager(application)

    private val hasTriggeredSeed = AtomicBoolean(false)
    private var pendingInstallApk: File? = null

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _permissionsState = MutableStateFlow(permissionsManager.checkPermissionsState())
    val permissionsState: StateFlow<LauncherPermissionsState> = _permissionsState.asStateFlow()

    private data class TransientState(
        val searchQuery: String = "",
        val selectedSectionSymbol: Char? = null,
        val isScrubbingWave: Boolean = false,
        val isAllAppsVisible: Boolean = false,
        val isSearchActive: Boolean = false,
        val isManageAppsActive: Boolean = false,
        val isUpdateSheetActive: Boolean = false,
        val isSettingsSheetActive: Boolean = false,
        val isDefaultPromptDialogVisible: Boolean = false,
        val selectedAppForMenu: AppInfo? = null
    )

    private val _transientState = MutableStateFlow(TransientState())

    private data class AppsPreferencesState(
        val installedApps: List<AppInfo>,
        val favoriteKeys: List<String>,
        val hiddenKeys: Set<String>,
        val isInitialSeeded: Boolean
    )

    // Combine 4 data layer flows
    private val appsPreferencesFlow = combine(
        appRepository.installedAppsFlow,
        preferencesRepository.favoriteKeysFlow,
        preferencesRepository.hiddenKeysFlow,
        preferencesRepository.isInitialSeededFlow
    ) { installedRaw, favoriteKeys, hiddenKeys, isInitialSeeded ->
        AppsPreferencesState(installedRaw, favoriteKeys, hiddenKeys, isInitialSeeded)
    }

    // Combine UI transient flows (3 flows)
    private val uiTransientFlow = combine(
        _transientState,
        _updateState,
        _permissionsState
    ) { transient, update, perms ->
        Triple(transient, update, perms)
    }

    // Combine 4 top-level flows
    val uiState: StateFlow<LauncherUiState> = combine(
        appsPreferencesFlow,
        systemStatusProvider.systemStatusFlow,
        preferencesRepository.settingsFlow,
        uiTransientFlow
    ) { data, systemStatus, settings, (transient, update, perms) ->

        // Seed initial default favorites safely guarded against race conditions
        if (!data.isInitialSeeded && data.installedApps.isNotEmpty() && hasTriggeredSeed.compareAndSet(false, true)) {
            seedDefaultFavorites(data.installedApps)
        }

        // Decorate apps with favorite & hidden metadata
        val favoriteKeySet = data.favoriteKeys.toSet()
        val allDecorated = data.installedApps.map { app ->
            val isFav = favoriteKeySet.contains(app.uniqueKey)
            val rank = if (isFav) data.favoriteKeys.indexOf(app.uniqueKey) else -1
            val isHidden = data.hiddenKeys.contains(app.uniqueKey)
            app.copy(
                isFavorite = isFav,
                favoriteRank = rank,
                isHidden = isHidden
            )
        }

        // Split into Visible and Hidden
        val visibleApps = allDecorated.filter { !it.isHidden }
        val hiddenApps = allDecorated.filter { it.isHidden }

        // Build Favorites list ordered according to stored favoriteKeys
        val favoriteMap = visibleApps.filter { it.isFavorite }.associateBy { it.uniqueKey }
        val orderedFavorites = data.favoriteKeys.mapNotNull { favoriteMap[it] }

        // Set of section symbols that currently have apps
        val availableSymbolSet = visibleApps.map { it.sectionLetter }.toSet()

        // Index map for fast scrolling by section letter
        val sectionIndices = mutableMapOf<Char, Int>()
        visibleApps.forEachIndexed { index, app ->
            if (!sectionIndices.containsKey(app.sectionLetter)) {
                sectionIndices[app.sectionLetter] = index
            }
        }

        // Search filtering: Prefix matches first, then contains matches
        val query = transient.searchQuery.trim()
        val filtered = if (query.isEmpty()) {
            emptyList()
        } else {
            val prefixMatches = mutableListOf<AppInfo>()
            val containsMatches = mutableListOf<AppInfo>()
            for (app in visibleApps) {
                if (app.label.startsWith(query, ignoreCase = true)) {
                    prefixMatches.add(app)
                } else if (app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                ) {
                    containsMatches.add(app)
                }
            }
            prefixMatches + containsMatches
        }

        LauncherUiState(
            allApps = visibleApps,
            favoriteApps = orderedFavorites,
            hiddenApps = hiddenApps,
            systemStatus = systemStatus,
            settings = settings,
            searchQuery = transient.searchQuery,
            filteredApps = filtered,
            selectedSectionSymbol = transient.selectedSectionSymbol,
            isScrubbingWave = transient.isScrubbingWave,
            isAllAppsVisible = transient.isAllAppsVisible,
            isSearchActive = transient.isSearchActive,
            isManageAppsActive = transient.isManageAppsActive,
            isUpdateSheetActive = transient.isUpdateSheetActive,
            isSettingsSheetActive = transient.isSettingsSheetActive,
            isDefaultPromptDialogVisible = transient.isDefaultPromptDialogVisible,
            selectedAppForMenu = transient.selectedAppForMenu,
            availableSymbols = availableSymbolSet,
            letterSectionIndices = sectionIndices,
            updateState = update,
            permissionsState = perms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    init {
        // Automatically check for GitHub updates on app launch
        checkForUpdates(silent = true)
    }

    private fun seedDefaultFavorites(installed: List<AppInfo>) {
        viewModelScope.launch {
            val candidateKeywords = listOf(
                listOf("dialer", "phone", "contacts"),
                listOf("messaging", "message", "sms", "mms"),
                listOf("browser", "chrome", "firefox"),
                listOf("camera"),
                listOf("gallery", "photos"),
                listOf("settings")
            )

            val chosenKeys = mutableListOf<String>()
            for (keywords in candidateKeywords) {
                val matched = installed.firstOrNull { app ->
                    val pkg = app.packageName.lowercase()
                    val label = app.label.lowercase()
                    keywords.any { kw -> pkg.contains(kw) || label.contains(kw) } &&
                            !chosenKeys.contains(app.uniqueKey)
                }
                if (matched != null) {
                    chosenKeys.add(matched.uniqueKey)
                }
            }

            if (chosenKeys.size < 4) {
                installed.take(6).forEach { app ->
                    if (!chosenKeys.contains(app.uniqueKey)) {
                        chosenKeys.add(app.uniqueKey)
                    }
                }
            }

            preferencesRepository.setFavorites(chosenKeys.take(8))
            preferencesRepository.markInitialSeeded()
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _updateState.value = UpdateUiState.Checking
            }
            val result = updateRepository.checkLatestRelease()
            result.onSuccess { release ->
                if (release.isNewer) {
                    _updateState.value = UpdateUiState.UpdateAvailable(release)
                } else {
                    _updateState.value = UpdateUiState.UpToDate(release)
                }
            }.onFailure { error ->
                if (!silent) {
                    _updateState.value = UpdateUiState.Error(error.localizedMessage ?: "Failed to check updates")
                }
            }
        }
    }

    fun downloadUpdate(releaseInfo: ReleaseInfo) {
        viewModelScope.launch {
            updateRepository.downloadApk(releaseInfo).collect { downloadState ->
                when (downloadState) {
                    is UpdateDownloadState.Idle -> {
                        _updateState.value = UpdateUiState.Idle
                    }
                    is UpdateDownloadState.Downloading -> {
                        _updateState.value = UpdateUiState.Downloading(
                            progressPercent = downloadState.progressPercent,
                            downloadedMb = downloadState.downloadedMb,
                            totalMb = downloadState.totalMb,
                            releaseInfo = releaseInfo
                        )
                    }
                    is UpdateDownloadState.Complete -> {
                        _updateState.value = UpdateUiState.ReadyToInstall(
                            apkFile = downloadState.apkFile,
                            releaseInfo = downloadState.releaseInfo
                        )
                        installDownloadedApk(downloadState.apkFile)
                    }
                    is UpdateDownloadState.Error -> {
                        _updateState.value = UpdateUiState.Error(downloadState.message)
                    }
                }
            }
        }
    }

    fun installDownloadedApk(apkFile: File) {
        val result = updateRepository.installApk(apkFile)
        when (result) {
            is InstallResult.Success -> {
                pendingInstallApk = null
            }
            is InstallResult.NeedsUnknownSourcesPermission -> {
                pendingInstallApk = apkFile
            }
            is InstallResult.Error -> {
                pendingInstallApk = null
                _updateState.value = UpdateUiState.Error(result.message)
            }
        }
    }

    fun refreshPermissionsAndPendingInstall() {
        val updated = permissionsManager.checkPermissionsState()
        _permissionsState.value = updated

        if (updated.canInstallPackages && pendingInstallApk != null && pendingInstallApk!!.exists()) {
            val fileToInstall = pendingInstallApk!!
            pendingInstallApk = null
            installDownloadedApk(fileToInstall)
        }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch {
            preferencesRepository.updateSettings(transform)
        }
    }

    fun resetSettingsToDefault() {
        viewModelScope.launch {
            preferencesRepository.resetSettingsToDefault()
        }
    }

    fun setAllAppsVisible(visible: Boolean) {
        _transientState.update { it.copy(isAllAppsVisible = visible) }
    }

    fun setSettingsSheetActive(active: Boolean) {
        _transientState.update { it.copy(isSettingsSheetActive = active) }
    }

    fun setDefaultPromptDialogVisible(visible: Boolean) {
        _transientState.update { it.copy(isDefaultPromptDialogVisible = visible) }
    }

    fun onHomeIntentReceived() {
        _transientState.update {
            it.copy(
                isSearchActive = false,
                isManageAppsActive = false,
                isUpdateSheetActive = false,
                isSettingsSheetActive = false,
                isDefaultPromptDialogVisible = false,
                isAllAppsVisible = false,
                selectedAppForMenu = null,
                searchQuery = ""
            )
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _transientState.update { it.copy(searchQuery = newQuery) }
    }

    fun setSearchActive(active: Boolean) {
        _transientState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
    }

    fun setManageAppsActive(active: Boolean) {
        _transientState.update { it.copy(isManageAppsActive = active) }
    }

    fun setUpdateSheetActive(active: Boolean) {
        _transientState.update { it.copy(isUpdateSheetActive = active) }
    }

    fun openAppContextMenu(app: AppInfo) {
        _transientState.update { it.copy(selectedAppForMenu = app) }
    }

    fun closeAppContextMenu() {
        _transientState.update { it.copy(selectedAppForMenu = null) }
    }

    fun toggleFavorite(app: AppInfo) {
        viewModelScope.launch {
            if (app.isFavorite) {
                preferencesRepository.removeFavorite(app.uniqueKey)
            } else {
                preferencesRepository.addFavorite(app.uniqueKey)
            }
            closeAppContextMenu()
        }
    }

    fun moveFavorite(app: AppInfo, delta: Int) {
        viewModelScope.launch {
            preferencesRepository.moveFavorite(app.uniqueKey, delta)
            closeAppContextMenu()
        }
    }

    fun reorderFavorites(newKeys: List<String>) {
        viewModelScope.launch {
            preferencesRepository.setFavorites(newKeys)
        }
    }

    fun toggleHideApp(app: AppInfo) {
        viewModelScope.launch {
            if (app.isHidden) {
                preferencesRepository.unhideApp(app.uniqueKey)
            } else {
                preferencesRepository.hideApp(app.uniqueKey)
            }
            closeAppContextMenu()
        }
    }

    fun onWaveScrubStarted() {
        _transientState.update { it.copy(isScrubbingWave = true) }
    }

    fun onWaveScrubSymbolChanged(symbol: Char) {
        if (symbol == '★') {
            _transientState.update { it.copy(selectedSectionSymbol = symbol, isAllAppsVisible = false) }
        } else {
            _transientState.update { it.copy(selectedSectionSymbol = symbol, isAllAppsVisible = true) }
        }
    }

    fun onWaveScrubEnded() {
        _transientState.update { it.copy(isScrubbingWave = false, selectedSectionSymbol = null) }
    }

    fun launchApp(app: AppInfo): Boolean {
        return appRepository.launchApp(app)
    }

    fun openAppDetails(app: AppInfo) {
        appRepository.openAppDetails(app)
        closeAppContextMenu()
    }

    fun requestUninstall(app: AppInfo) {
        appRepository.requestUninstall(app)
        closeAppContextMenu()
    }
}
