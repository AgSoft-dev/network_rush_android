package com.triviamap.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.repository.UserPreferencesRepository
import com.triviamap.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository
) : ViewModel() {
    val leftHanded = prefs.leftHanded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val haptics = prefs.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { prefs.setHapticsEnabled(enabled) }
    }

    fun setLeftHanded(enabled: Boolean) {
        viewModelScope.launch { prefs.setLeftHanded(enabled) }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val leftHanded by vm.leftHanded.collectAsState()
    val haptics by vm.haptics.collectAsState()

    Scaffold(
        backgroundColor = Background,
        topBar = {
            TopAppBar(
                backgroundColor = Background,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                title = { Text("Settings", color = OnSurface, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingRow(
                title = "Left-handed mode",
                description = "Puts the drag handles on the left edge of the tiles in Station Sprint.",
                checked = leftHanded,
                onChange = vm::setLeftHanded
            )
            SettingRow(
                title = "Vibrations",
                description = "Feedback when dragging tiles and on correct / wrong answers.",
                checked = haptics,
                onChange = vm::setHaptics
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text(description, color = OnSurfaceMed, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary)
            )
        }
    }
}
