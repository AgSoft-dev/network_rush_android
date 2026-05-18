package com.triviamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.presentation.common.TriviaMapNavGraph
import com.triviamap.presentation.common.TriviaMapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tramLineRepository: TramLineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Pre-load tram data from assets
        CoroutineScope(Dispatchers.IO).launch {
            tramLineRepository.syncFromAssets()
        }

        setContent {
            TriviaMapTheme {
                val navController = rememberNavController()
                TriviaMapNavGraph(navController = navController)
            }
        }
    }
}
