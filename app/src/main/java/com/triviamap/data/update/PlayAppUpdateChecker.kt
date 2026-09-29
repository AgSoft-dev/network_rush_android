package com.triviamap.data.update

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.triviamap.domain.update.AppUpdateChecker
import com.triviamap.domain.update.UpdateState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayAppUpdateChecker @Inject constructor(
    @param:ApplicationContext context: Context
) : AppUpdateChecker {

    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private var info: AppUpdateInfo? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.None)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val installListener = InstallStateUpdatedListener { install ->
        when (install.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> _state.value = UpdateState.Downloading
            InstallStatus.DOWNLOADED -> _state.value = UpdateState.ReadyToInstall
            // Failed, cancelled or installed: fall back to a fresh check on the next resume
            else -> _state.value = UpdateState.None
        }
    }

    init {
        manager.registerListener(installListener)
    }

    override fun check() {
        // Failures (not installed from Play, offline, no Play services) simply mean "no update"
        manager.appUpdateInfo.addOnSuccessListener { i ->
            info = i
            _state.value = when {
                i.installStatus() == InstallStatus.DOWNLOADED -> UpdateState.ReadyToInstall
                i.installStatus() == InstallStatus.DOWNLOADING || i.installStatus() == InstallStatus.PENDING -> UpdateState.Downloading
                i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> UpdateState.Available(i.availableVersionCode())
                else -> UpdateState.None
            }
        }
    }

    override fun startUpdate(activity: Activity) {
        val i = info ?: return
        manager.startUpdateFlowForResult(i, activity, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(), REQUEST_CODE)
    }

    override fun install() {
        manager.completeUpdate()
    }

    private companion object {
        const val REQUEST_CODE = 4210
    }
}
