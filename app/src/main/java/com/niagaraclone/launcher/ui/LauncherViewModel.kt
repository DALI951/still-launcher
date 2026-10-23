package com.niagaraclone.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niagaraclone.launcher.data.AppRepository
import com.niagaraclone.launcher.data.AppTimerManager
import com.niagaraclone.launcher.data.ActiveTimerSession
import com.niagaraclone.launcher.data.AppUpdateRepository
import com.niagaraclone.launcher.data.DailyUsageSummary
import com.niagaraclone.launcher.data.InstallResult
import com.niagaraclone.launcher.data.LauncherPermissionsState
import com.niagaraclone.launcher.data.PermissionsManager
import com.niagaraclone.launcher.data.PreferencesRepository
import com.niagaraclone.launcher.data.ReleaseInfo
import com.niagaraclone.launcher.data.ScreenTimeTracker
import com.niagaraclone.launcher.data.SystemStatusProvider
import com.niagaraclone.launcher.data.UpdateDownloadState
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.AppLaunchRule
import com.niagaraclone.launcher.data.model.FocusProfile
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.SystemStatus
import com.niagaraclone.launcher.data.model.YouTubeHistoryItem
import com.niagaraclone.launcher.ui.components.FrictionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
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
 * UI State representing the complete Still launcher screen.
 */
data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val favoriteApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val systemStatus: SystemStatus = SystemStatus(),
    val settings: LauncherSettings = LauncherSettings(),
    val searchQuery: String = "",
    val filteredApps: List<AppInfo> = emptyList(),
    val isSearchActive: Boolean = false,
    val isSettingsSheetActive: Boolean = false,
    val isYouTubeGateActive: Boolean = false,
    val directPlayVideoId: String? = null,
    val directPlayStartSeconds: Int = 0,
    val pendingFrictionApp: AppInfo? = null,
    val pendingFrictionType: FrictionType? = null,
    val pendingTimerApp: AppInfo? = null,
    val isUsageSheetActive: Boolean = false,
    val dailyUsageSummary: DailyUsageSummary = DailyUsageSummary(),
    val activeTimerSession: ActiveTimerSession? = null,
    val selectedAppForMenu: AppInfo? = null,
    val youtubeHistory: List<YouTubeHistoryItem> = emptyList(),
    val updateState: UpdateUiState = UpdateUiState.Idle,
    val permissionsState: LauncherPermissionsState = LauncherPermissionsState()
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    val preferencesRepository = PreferencesRepository(application)
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
        val isSearchActive: Boolean = false,
        val isSettingsSheetActive: Boolean = false,
        val isYouTubeGateActive: Boolean = false,
        val directPlayVideoId: String? = null,
        val directPlayStartSeconds: Int = 0,
        val pendingFrictionApp: AppInfo? = null,
        val pendingFrictionType: FrictionType? = null,
        val pendingTimerApp: AppInfo? = null,
        val isUsageSheetActive: Boolean = false,
        val selectedAppForMenu: AppInfo? = null
    )

    val timerManager = AppTimerManager.getInstance(application)
    val screenTimeTracker = ScreenTimeTracker.getInstance(application)
    private val _dailyUsageSummary = MutableStateFlow(screenTimeTracker.getDailyUsageSummary())

    private val _transientState = MutableStateFlow(TransientState())

    private data class AppsPreferencesState(
        val installedApps: List<AppInfo>,
        val favoriteKeys: List<String>,
        val hiddenKeys: Set<String>,
        val isInitialSeeded: Boolean,
        val rules: Map<String, AppLaunchRule>,
        val youtubeHistory: List<YouTubeHistoryItem>
    )

    // Combine data layer flows in safe tuples
    private val appsDataFlow = combine(
        appRepository.installedAppsFlow,
        preferencesRepository.favoriteKeysFlow,
        preferencesRepository.hiddenKeysFlow
    ) { installed, favs, hidden ->
        Triple(installed, favs, hidden)
    }

    private val userPrefsFlow = combine(
        preferencesRepository.isInitialSeededFlow,
        preferencesRepository.appLaunchRulesFlow,
        preferencesRepository.youtubeHistoryFlow
    ) { seeded, rules, history ->
        Triple(seeded, rules, history)
    }

    private val appsPreferencesFlow = combine(
        appsDataFlow,
        userPrefsFlow
    ) { (installed, favs, hidden), (seeded, rules, history) ->
        AppsPreferencesState(installed, favs, hidden, seeded, rules, history)
    }

    private data class TransientTuple(
        val transient: TransientState,
        val update: UpdateUiState,
        val perms: LauncherPermissionsState,
        val usage: DailyUsageSummary,
        val activeTimer: ActiveTimerSession?
    )

    // Combine UI transient flows
    private val uiTransientFlow = combine(
        _transientState,
        _updateState,
        _permissionsState,
        _dailyUsageSummary,
        timerManager.activeSession
    ) { transient, update, perms, usage, activeTimer ->
        TransientTuple(transient, update, perms, usage, activeTimer)
    }

    // Combine top-level flows
    val uiState: StateFlow<LauncherUiState> = combine(
        appsPreferencesFlow,
        systemStatusProvider.systemStatusFlow,
        preferencesRepository.settingsFlow,
        uiTransientFlow
    ) { data, systemStatus, settings, tuple ->

        // Seed initial default favorites safely guarded against race conditions
        if (!data.isInitialSeeded && data.installedApps.isNotEmpty() && hasTriggeredSeed.compareAndSet(false, true)) {
            seedDefaultFavorites(data.installedApps)
        }

        // Decorate apps with favorite, hidden, and launch rule metadata
        val favoriteKeySet = data.favoriteKeys.toSet()
        val allDecorated = data.installedApps.map { app ->
            val isFav = favoriteKeySet.contains(app.uniqueKey)
            val rank = if (isFav) data.favoriteKeys.indexOf(app.uniqueKey) else -1
            val isHidden = data.hiddenKeys.contains(app.uniqueKey)
            val rule = data.rules[app.uniqueKey] ?: AppLaunchRule.NORMAL
            app.copy(
                isFavorite = isFav,
                favoriteRank = rank,
                isHidden = isHidden,
                launchRule = rule
            )
        }

        // Split into Visible and Hidden
        val visibleApps = allDecorated.filter { !it.isHidden }
        val hiddenApps = allDecorated.filter { it.isHidden }

        // Build Favorites list ordered according to stored favoriteKeys
        val favoriteMap = visibleApps.filter { it.isFavorite }.associateBy { it.uniqueKey }
        val orderedFavorites = data.favoriteKeys.mapNotNull { favoriteMap[it] }

        // Search filtering: Prefix matches first, then contains matches
        val query = tuple.transient.searchQuery.trim()
        val filtered = if (query.isEmpty()) {
            visibleApps
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
            searchQuery = tuple.transient.searchQuery,
            filteredApps = filtered,
            isSearchActive = tuple.transient.isSearchActive,
            isSettingsSheetActive = tuple.transient.isSettingsSheetActive,
            isYouTubeGateActive = tuple.transient.isYouTubeGateActive,
            directPlayVideoId = tuple.transient.directPlayVideoId,
            directPlayStartSeconds = tuple.transient.directPlayStartSeconds,
            pendingFrictionApp = tuple.transient.pendingFrictionApp,
            pendingFrictionType = tuple.transient.pendingFrictionType,
            pendingTimerApp = tuple.transient.pendingTimerApp,
            isUsageSheetActive = tuple.transient.isUsageSheetActive,
            dailyUsageSummary = tuple.usage,
            activeTimerSession = tuple.activeTimer,
            selectedAppForMenu = tuple.transient.selectedAppForMenu,
            youtubeHistory = data.youtubeHistory,
            updateState = tuple.update,
            permissionsState = tuple.perms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState()
    )

    private fun seedDefaultFavorites(installed: List<AppInfo>) {
        viewModelScope.launch {
            val essentialPackages = listOf(
                "dialer", "phone", "contacts",
                "messaging", "mms", "message",
                "chrome", "browser",
                "camera"
            )

            val seededKeys = mutableListOf<String>()
            for (keyword in essentialPackages) {
                if (seededKeys.size >= 5) break
                val match = installed.firstOrNull { app ->
                    val pkg = app.packageName.lowercase()
                    val lbl = app.label.lowercase()
                    (pkg.contains(keyword) || lbl.contains(keyword)) &&
                            !seededKeys.contains(app.uniqueKey)
                }
                if (match != null) {
                    seededKeys.add(match.uniqueKey)
                }
            }

            if (seededKeys.isNotEmpty()) {
                preferencesRepository.saveFavorites(seededKeys)
            }
            preferencesRepository.setInitialSeeded(true)
        }
    }

    /**
     * Centralized App Launch Interceptor.
     * Enforces Intentional YouTube Gate, Active Focus Profiles, and Mindful Breathing Delays.
     */
    fun requestLaunchApp(app: AppInfo) {
        val settings = uiState.value.settings

        // 1. YouTube Intentional Gate Interception
        if (app.isYouTube && settings.interceptYouTube) {
            setYouTubeGateActive(true)
            return
        }

        // 2. Essential Utilities (Phone, SMS, Clock, Settings) bypass all restrictions
        if (app.isEssentialUtility) {
            launchApp(app)
            return
        }

        // 3. Focus Profile and Schedule Restrictions
        if (settings.activeFocusProfile != FocusProfile.DEFAULT) {
            val isRestrictedBySchedule = when (settings.activeFocusProfile) {
                FocusProfile.STUDY -> isWithinSchedule(settings.studyStartHour, settings.studyEndHour)
                FocusProfile.SLEEP -> isWithinSchedule(settings.sleepStartHour, settings.sleepEndHour)
                FocusProfile.CUSTOM -> true
                else -> false
            }

            if (isRestrictedBySchedule && (app.launchRule == AppLaunchRule.SCHEDULED || app.launchRule == AppLaunchRule.BLOCKED)) {
                _transientState.update {
                    it.copy(
                        pendingFrictionApp = app,
                        pendingFrictionType = FrictionType.BlockedByFocus(settings.activeFocusProfile)
                    )
                }
                return
            }
        }

        // 4. Strict Blocked Check
        if (app.launchRule == AppLaunchRule.BLOCKED) {
            _transientState.update {
                it.copy(
                    pendingFrictionApp = app,
                    pendingFrictionType = FrictionType.BlockedByFocus(FocusProfile.STUDY)
                )
            }
            return
        }

        // 5. Mindful Breathing Pause Delay
        if (app.launchRule == AppLaunchRule.DELAYED) {
            _transientState.update {
                it.copy(
                    pendingFrictionApp = app,
                    pendingFrictionType = FrictionType.BreathingDelay(settings.breathingDelaySeconds)
                )
            }
            return
        }

        // 6. Active Cooldown Check
        if (timerManager.isAppInCooldown(app.packageName)) {
            val remainingSecs = timerManager.getRemainingCooldownSeconds(app.packageName)
            _transientState.update {
                it.copy(
                    pendingFrictionApp = app,
                    pendingFrictionType = FrictionType.BreathingDelay(remainingSecs.coerceAtLeast(3))
                )
            }
            return
        }

        // 7. Mindful In-App Usage Timer selection prompt
        if (settings.appUsageTimersEnabled && settings.askTimerOnAppLaunch && !app.isEssentialUtility) {
            _transientState.update {
                it.copy(
                    pendingTimerApp = app
                )
            }
            return
        }

        // Default: Immediate standard launch
        launchApp(app)
    }

    private fun isWithinSchedule(startHour: Int, endHour: Int): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            // Crosses midnight, e.g. 22:00 to 07:00
            currentHour >= startHour || currentHour < endHour
        }
    }

    fun confirmFrictionLaunch() {
        val app = _transientState.value.pendingFrictionApp
        _transientState.update {
            it.copy(
                pendingFrictionApp = null,
                pendingFrictionType = null
            )
        }
        if (app != null) {
            launchApp(app)
        }
    }

    fun dismissFriction() {
        _transientState.update {
            it.copy(
                pendingFrictionApp = null,
                pendingFrictionType = null
            )
        }
    }

    fun launchApp(app: AppInfo) {
        val success = appRepository.launchApp(app)
        if (success) {
            _transientState.update {
                it.copy(
                    isSearchActive = false,
                    searchQuery = ""
                )
            }
        }
    }

    fun setYouTubeGateActive(active: Boolean) {
        _transientState.update {
            it.copy(
                isYouTubeGateActive = active,
                directPlayVideoId = if (!active) null else it.directPlayVideoId,
                directPlayStartSeconds = if (!active) 0 else it.directPlayStartSeconds
            )
        }
    }

    fun openYouTubeVideoDirectly(videoId: String, startSeconds: Int = 0, url: String? = null) {
        _transientState.update {
            it.copy(
                isYouTubeGateActive = true,
                directPlayVideoId = videoId,
                directPlayStartSeconds = startSeconds,
                isSearchActive = false,
                isSettingsSheetActive = false,
                pendingFrictionApp = null,
                selectedAppForMenu = null
            )
        }
        if (uiState.value.settings.youtubeHistoryEnabled) {
            saveYouTubeHistory(
                YouTubeHistoryItem(
                    id = videoId,
                    title = "Video ($videoId)",
                    url = url ?: "https://www.youtube.com/watch?v=$videoId"
                )
            )
        }
    }

    fun launchWithAppTimer(app: AppInfo, minutes: Int) {
        _transientState.update { it.copy(pendingTimerApp = null) }
        timerManager.startTimer(app.packageName, app.label, minutes)
        launchApp(app)
    }

    fun launchWithoutAppTimer(app: AppInfo) {
        _transientState.update { it.copy(pendingTimerApp = null) }
        launchApp(app)
    }

    fun dismissTimerSheet() {
        _transientState.update { it.copy(pendingTimerApp = null) }
    }

    fun setUsageSheetActive(active: Boolean) {
        _transientState.update { it.copy(isUsageSheetActive = active) }
        if (active) {
            refreshDailyUsage()
        }
    }

    fun refreshDailyUsage() {
        _dailyUsageSummary.value = screenTimeTracker.getDailyUsageSummary()
    }

    fun updateDailyIntention(text: String) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { s -> s.copy(dailyIntentionText = text.trim()) }
        }
    }

    fun setSettingsSheetActive(active: Boolean) {
        _transientState.update { it.copy(isSettingsSheetActive = active) }
    }

    fun onHomeIntentReceived() {
        _transientState.update {
            it.copy(
                isSearchActive = false,
                isSettingsSheetActive = false,
                isYouTubeGateActive = false,
                directPlayVideoId = null,
                directPlayStartSeconds = 0,
                pendingFrictionApp = null,
                pendingFrictionType = null,
                pendingTimerApp = null,
                isUsageSheetActive = false,
                selectedAppForMenu = null,
                searchQuery = ""
            )
        }
        refreshDailyUsage()
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

    fun openAppContextMenu(app: AppInfo) {
        _transientState.update { it.copy(selectedAppForMenu = app) }
    }

    fun closeAppContextMenu() {
        _transientState.update { it.copy(selectedAppForMenu = null) }
    }

    fun addFavorite(app: AppInfo) {
        viewModelScope.launch {
            preferencesRepository.addFavorite(app.uniqueKey)
            closeAppContextMenu()
        }
    }

    fun removeFavorite(app: AppInfo) {
        viewModelScope.launch {
            preferencesRepository.removeFavorite(app.uniqueKey)
            closeAppContextMenu()
        }
    }

    fun moveFavorite(app: AppInfo, delta: Int) {
        viewModelScope.launch {
            val favorites = uiState.value.favoriteApps
            val index = favorites.indexOfFirst { it.uniqueKey == app.uniqueKey }
            if (index != -1) {
                val targetIndex = index + delta
                if (targetIndex in favorites.indices) {
                    preferencesRepository.reorderFavorite(index, targetIndex)
                }
            }
            closeAppContextMenu()
        }
    }

    fun reorderFavorite(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            preferencesRepository.reorderFavorite(fromIndex, toIndex)
        }
    }

    fun setAppLaunchRule(app: AppInfo, rule: AppLaunchRule) {
        viewModelScope.launch {
            preferencesRepository.setAppLaunchRule(app.uniqueKey, rule)
        }
    }

    fun saveYouTubeHistory(item: YouTubeHistoryItem) {
        viewModelScope.launch {
            preferencesRepository.addYouTubeHistoryItem(item)
        }
    }

    fun removeYouTubeHistoryItem(id: String) {
        viewModelScope.launch {
            preferencesRepository.removeYouTubeHistoryItem(id)
        }
    }

    fun clearYouTubeHistory() {
        viewModelScope.launch {
            preferencesRepository.clearYouTubeHistory()
        }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch {
            preferencesRepository.updateSettings(transform)
        }
    }

    fun openAppDetails(app: AppInfo) {
        appRepository.openAppDetails(app)
        closeAppContextMenu()
    }

    fun requestUninstall(app: AppInfo) {
        appRepository.requestUninstall(app)
        closeAppContextMenu()
    }

    // Updater methods
    fun checkForUpdates(force: Boolean = true) {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            val result = updateRepository.checkLatestRelease()
            result.onSuccess { relInfo ->
                if (relInfo.isNewer) {
                    _updateState.value = UpdateUiState.UpdateAvailable(relInfo)
                } else {
                    _updateState.value = UpdateUiState.UpToDate(relInfo)
                }
            }.onFailure { err ->
                _updateState.value = UpdateUiState.Error(err.message ?: "Failed to check for updates")
            }
        }
    }

    fun startUpdateDownload(releaseInfo: ReleaseInfo) {
        viewModelScope.launch {
            updateRepository.downloadApk(releaseInfo).collect { dlState ->
                when (dlState) {
                    is UpdateDownloadState.Idle -> {
                        _updateState.value = UpdateUiState.Idle
                    }
                    is UpdateDownloadState.Downloading -> {
                        _updateState.value = UpdateUiState.Downloading(
                            progressPercent = dlState.progressPercent,
                            downloadedMb = dlState.downloadedMb,
                            totalMb = dlState.totalMb,
                            releaseInfo = releaseInfo
                        )
                    }
                    is UpdateDownloadState.Complete -> {
                        pendingInstallApk = dlState.apkFile
                        _updateState.value = UpdateUiState.ReadyToInstall(dlState.apkFile, releaseInfo)
                    }
                    is UpdateDownloadState.Error -> {
                        _updateState.value = UpdateUiState.Error(dlState.message)
                    }
                }
            }
        }
    }

    fun installPendingUpdate(): InstallResult {
        val apk = pendingInstallApk ?: return InstallResult.Error("No APK file found")
        return updateRepository.installApk(apk)
    }

    fun refreshPermissionsState() {
        _permissionsState.value = permissionsManager.checkPermissionsState()
    }

    fun refreshPermissionsAndPendingInstall() {
        refreshPermissionsState()
        val current = _updateState.value
        if (current is UpdateUiState.ReadyToInstall && permissionsManager.checkPermissionsState().canInstallPackages) {
            installPendingUpdate()
        }
    }
}
