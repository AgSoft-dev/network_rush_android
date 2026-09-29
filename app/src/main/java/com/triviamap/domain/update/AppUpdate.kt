package com.triviamap.domain.update

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

sealed interface UpdateState {
    data object None : UpdateState
    /** A newer version is on Google Play. */
    data class Available(val versionCode: Int) : UpdateState
    data object Downloading : UpdateState
    /** Downloaded in the background; installing restarts the app. */
    data object ReadyToInstall : UpdateState
}

/** Google Play in-app updates (flexible flow) behind an interface. Only reports updates for builds installed from Play. */
interface AppUpdateChecker {
    val state: StateFlow<UpdateState>
    /** Asks Play for a newer version or an already downloaded one; safe to call on every resume. */
    fun check()
    /** Starts the background download; Play shows its own confirmation dialog. */
    fun startUpdate(activity: Activity)
    /** Installs a downloaded update and restarts the app. */
    fun install()
}

object UpdatePolicy {
    /** An available update is shown once per version after a dismissal; download progress and "restart" always show. */
    fun shouldShow(state: UpdateState, dismissedVersion: Int): Boolean = when (state) {
        UpdateState.None -> false
        is UpdateState.Available -> state.versionCode != dismissedVersion
        UpdateState.Downloading, UpdateState.ReadyToInstall -> true
    }
}
