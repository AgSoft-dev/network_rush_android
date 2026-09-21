package com.triviamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import com.triviamap.presentation.common.Background
import com.triviamap.presentation.common.MaxContentWidth
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.presentation.common.TriviaMapNavGraph
import com.triviamap.presentation.common.TriviaMapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tramLineRepository: TramLineRepository
    @Inject lateinit var adsController: com.triviamap.domain.monetization.AdsController
    @Inject lateinit var supportRepository: com.triviamap.domain.monetization.SupportRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Pre-load tram data from assets (ViewModels also trigger load(); it is idempotent)
        lifecycleScope.launch { tramLineRepository.load() }

        // Consent form (if required) then AdMob; Play Billing for the optional tips
        adsController.gatherConsent(this)
        supportRepository.connect()

        setContent {
            TriviaMapTheme {
                val navController = rememberNavController()
                // API 36 ignores the portrait lock on >= 600dp displays: keep the portrait UI centred
                Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.fillMaxHeight().widthIn(max = MaxContentWidth)) {
                        TriviaMapNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
