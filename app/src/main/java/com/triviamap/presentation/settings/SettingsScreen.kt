package com.triviamap.presentation.settings

import androidx.compose.foundation.BorderStroke
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
import android.app.Activity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import com.triviamap.R
import com.triviamap.domain.monetization.AdsController
import com.triviamap.domain.monetization.SupportEvent
import com.triviamap.domain.monetization.SupportOffer
import com.triviamap.domain.monetization.SupportRepository
import com.triviamap.domain.monetization.SupportTier
import com.triviamap.domain.repository.UserPreferencesRepository
import com.triviamap.util.findActivity
import com.triviamap.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val support: SupportRepository,
    private val ads: AdsController
) : ViewModel() {
    val offers = support.offers
    val supportEvents = support.events
    val privacyOptionsRequired = ads.privacyOptionsRequired
    val isSupporter = prefs.isSupporter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun buy(activity: Activity, tier: SupportTier) = support.purchase(activity, tier)
    fun showPrivacyOptions(activity: Activity) = ads.showPrivacyOptions(activity)

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
    val offers by vm.offers.collectAsState()
    val isSupporter by vm.isSupporter.collectAsState()
    val privacyRequired by vm.privacyOptionsRequired.collectAsState()
    val activity = LocalContext.current.findActivity()
    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(Unit) {
        vm.supportEvents.collect { event ->
            scaffoldState.snackbarHostState.showSnackbar(
                when (event) {
                    is SupportEvent.Thanks -> "Thank you so much! \u2665"
                    SupportEvent.Failed -> "Purchase unavailable right now, please try again later."
                }
            )
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
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
                title = { Text("Settings", color = OnSurface, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
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

            SupportSection(
                offers = offers,
                isSupporter = isSupporter,
                onBuy = { tier -> activity?.let { vm.buy(it, tier) } }
            )

            if (privacyRequired) {
                OutlinedButton(
                    onClick = { activity?.let(vm::showPrivacyOptions) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Privacy choices (ads)", color = OnSurfaceMed) }
            }

            LegalSection()
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Surface, border = BorderStroke(2.dp, Border), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnSurface, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(description, color = OnSurfaceMed, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Sun, checkedTrackColor = SunEdge, uncheckedThumbColor = OnSurfaceMed, uncheckedTrackColor = Border)
            )
        }
    }
}

@Composable
private fun SupportSection(offers: List<SupportOffer>, isSupporter: Boolean, onBuy: (SupportTier) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Ticket, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Support Network Rush", color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(
                if (isSupporter) "You're a supporter, thank you! The banner on the home screen is gone for good."
                else "A solo project. A tip keeps it going and removes the banner on the home screen. Nothing in the game is locked.",
                color = InkMed, fontSize = 12.sp
            )
            if (offers.isEmpty()) {
                Text("Tips are not available right now (offline, or not published on Google Play yet).", color = InkMed, fontSize = 11.sp)
            } else {
                offers.forEach { offer ->
                    Button(
                        onClick = { onBuy(offer.tier) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Sun, contentColor = Ink)
                    ) { Text("${offer.tier.emoji}  ${offer.tier.title} · ${offer.price}", fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

@Composable
private fun LegalSection() {
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)
    val dataUrl = stringResource(R.string.open_data_url)
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Surface(shape = RoundedCornerShape(18.dp), color = Surface, border = BorderStroke(2.dp, Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Legal", color = OnSurface, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(
                "Station data: \u00ab Stations de tram \u00bb, Ville et Eurom\u00e9tropole de Strasbourg, Licence Ouverte v2.0 (Etalab), data.strasbourg.eu. " +
                    "Data adapted (station order per line, schematic coordinates).",
                color = OnSurfaceMed, fontSize = 12.sp
            )
            Text(
                "Unofficial app, not affiliated with the CTS or the Eurom\u00e9tropole de Strasbourg. Do not use it to plan a trip.",
                color = OnSurfaceMed, fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { open(dataUrl) }) { Text("Data source", color = Sun) }
                if (privacyUrl.isNotBlank()) TextButton(onClick = { open(privacyUrl) }) { Text("Privacy policy", color = Sun) }
                if (termsUrl.isNotBlank()) TextButton(onClick = { open(termsUrl) }) { Text("Terms", color = Sun) }
            }
        }
    }
}
