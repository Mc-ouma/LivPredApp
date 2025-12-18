package com.soccertips.predictx.update

import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import androidx.core.content.edit

data class UpdateState(
    val isAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val isMandatory: Boolean = false,
    val stalenessDays: Int = 0,
    val updateType: Int = AppUpdateType.FLEXIBLE
)

@Singleton
class CustomAppUpdateManager
@Inject
constructor(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val updateAnalytics: UpdateAnalytics,
    private val updateRetryManager: UpdateRetryManager
) : DefaultLifecycleObserver {

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _updateState.value = _updateState.value.copy(isDownloading = true)
                Timber.d("Update downloading")
            }

            InstallStatus.DOWNLOADED -> {
                _updateState.value =
                    _updateState.value.copy(isDownloading = false, isDownloaded = true)
                Timber.d("Update downloaded, ready to install")
            }

            InstallStatus.INSTALLING -> {
                Timber.d("Update installing")
            }

            InstallStatus.INSTALLED -> {
                _updateState.value = UpdateState() // Reset state
                Timber.d("Update installed successfully")
                updateAnalytics.logUpdateCompleted(
                    updateType = if (_updateState.value.isMandatory) "IMMEDIATE" else "FLEXIBLE",
                    duration = 0L // Duration tracking would need additional state
                )
            }

            InstallStatus.FAILED -> {
                _updateState.value = UpdateState() // Reset state
                Timber.e("Update failed: ${state.installErrorCode()}")
                updateAnalytics.logUpdateFailed(
                    updateType = if (_updateState.value.isMandatory) "IMMEDIATE" else "FLEXIBLE",
                    errorCode = state.installErrorCode(),
                    reason = "Install failed"
                )
                // Reset check timer for retry
                sharedPrefs.edit { putLong("last_update_check", 0) }
            }

            else -> Timber.d("Update status: ${state.installStatus()}")
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        appUpdateManager.registerListener(installStateListener)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        appUpdateManager.unregisterListener(installStateListener)
    }

    suspend fun checkForUpdatesWithRetry() {
        val success = updateRetryManager.executeWithRetry {
            try {
                checkForUpdates()
                true
            } catch (e: Exception) {
                Timber.e(e, "Update check failed")
                false
            }
        }

        if (success) {
            Timber.d("Update check completed successfully")
        } else {
            Timber.w("Update check failed after retries")
            updateAnalytics.logUpdateFailed(
                updateType = "CHECK",
                errorCode = null,
                reason = "Failed after retries"
            )
        }
    }

    fun checkForUpdates() {
        if (!shouldCheckForUpdates()) return

        // Use retry manager for robust update checking
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                sharedPrefs
                    .edit {
                        putLong("last_update_check", System.currentTimeMillis())
                    }

                val isUpdateAvailable =
                    appUpdateInfo.updateAvailability() ==
                            UpdateAvailability.UPDATE_AVAILABLE
                val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0
                val isMandatory = stalenessDays > 5
                val updateType =
                    if (isMandatory) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE

                _updateState.value =
                    UpdateState(
                        isAvailable =
                            isUpdateAvailable &&
                                    appUpdateInfo.isUpdateTypeAllowed(updateType),
                        isMandatory = isMandatory,
                        stalenessDays = stalenessDays,
                        updateType = updateType
                    )

                // Log analytics if update is available
                if (_updateState.value.isAvailable) {
                    updateAnalytics.logUpdateAvailable(
                        stalenessDays = stalenessDays,
                        updateType = if (isMandatory) "IMMEDIATE" else "FLEXIBLE"
                    )
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to check for updates")
                // Reset the last check time to allow retry sooner
                sharedPrefs.edit { putLong("last_update_check", 0) }
            }
    }

    fun startUpdateFlow(activity: Activity): Boolean {
        val currentState = _updateState.value
        if (!currentState.isAvailable) return false

        // Always fetch a fresh AppUpdateInfo to avoid SendIntentException
        // due to FLAG_ONE_SHOT on the underlying PendingIntent
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                try {
                    val updateOptions =
                        AppUpdateOptions.newBuilder(currentState.updateType)
                            .setAllowAssetPackDeletion(true)
                            .build()

                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        updateOptions,
                        UPDATE_REQUEST_CODE
                    )

                    // Log analytics when update flow starts
                    updateAnalytics.logUpdateStarted(
                        updateType = if (currentState.isMandatory) "IMMEDIATE" else "FLEXIBLE",
                        userInitiated = true
                    )
                } catch (e: SendIntentException) {
                    // This occurs when the PendingIntent has already been consumed
                    // (e.g., after orientation change or multiple calls with same AppUpdateInfo)
                    Timber.w(e, "SendIntentException: Update intent already consumed, will retry with fresh info")
                    updateAnalytics.logUpdateFailed(
                        updateType = if (currentState.isMandatory) "IMMEDIATE" else "FLEXIBLE",
                        errorCode = null,
                        reason = "SendIntentException: Intent already consumed"
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start update flow")
                    updateAnalytics.logUpdateFailed(
                        updateType = if (currentState.isMandatory) "IMMEDIATE" else "FLEXIBLE",
                        errorCode = null,
                        reason = e.message
                    )
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to get AppUpdateInfo for update flow")
                updateAnalytics.logUpdateFailed(
                    updateType = if (currentState.isMandatory) "IMMEDIATE" else "FLEXIBLE",
                    errorCode = null,
                    reason = exception.message
                )
            }
        return true
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun resumeUpdateIfNeeded(activity: Activity) {
        // Always fetch fresh AppUpdateInfo to avoid SendIntentException
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    try {
                        val updateOptions =
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                .setAllowAssetPackDeletion(true)
                                .build()

                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            updateOptions,
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: SendIntentException) {
                        // Intent was already consumed, log and ignore
                        // A fresh check will be triggered on next app resume
                        Timber.w(e, "SendIntentException while resuming update: Intent already consumed")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to resume update")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to get AppUpdateInfo while resuming update")
            }
    }

    private fun shouldCheckForUpdates(): Boolean {
        val lastCheck = sharedPrefs.getLong("last_update_check", 0)
        val now = System.currentTimeMillis()
        return now - lastCheck > TimeUnit.DAYS.toMillis(1)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 100
    }
}
