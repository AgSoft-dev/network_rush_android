package com.triviamap.presentation.common

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import com.triviamap.presentation.gameplay.GameplayScreen
import com.triviamap.presentation.gameplay.SprintScreen
import com.triviamap.presentation.home.HomeScreen
import com.triviamap.presentation.results.ResultsScreen
import com.triviamap.presentation.stats.StatsScreen

sealed class Route(val path: String) {
    object Home           : Route("home")
    object Gameplay       : Route("gameplay/{difficulty}") {
        fun build(difficulty: Difficulty) = "gameplay/${difficulty.name}"
    }
    object Sprint         : Route("sprint/{difficulty}") {
        fun build(difficulty: Difficulty) = "sprint/${difficulty.name}"
    }
    object Results        : Route("results/{mode}/{score}") {
        fun build(mode: GameMode, score: Int) = "results/${mode.name}/$score"
    }
    object Stats          : Route("stats")
}

@Composable
fun TriviaMapNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home.path) {

        composable(Route.Home.path) {
            HomeScreen(
                onPlay = { mode, difficulty ->
                    when(mode) {
                        GameMode.TRACE_NETWORK -> navController.navigate(Route.Gameplay.build(difficulty))
                        GameMode.STATION_SPRINT -> navController.navigate(Route.Sprint.build(difficulty))
                    }
                },
                onStats = { navController.navigate(Route.Stats.path) }
            )
        }

        composable(
            route = Route.Gameplay.path,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
        ) { backStack ->
            val difficulty = Difficulty.valueOf(backStack.arguments?.getString("difficulty") ?: Difficulty.MEDIUM.name)
            GameplayScreen(
                difficulty = difficulty,
                onFinished = { score ->
                    navController.navigate(Route.Results.build(GameMode.TRACE_NETWORK, score)) {
                        popUpTo(Route.Home.path)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Sprint.path,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
        ) { backStack ->
            val difficulty = Difficulty.valueOf(backStack.arguments?.getString("difficulty") ?: Difficulty.MEDIUM.name)
            SprintScreen(
                difficulty = difficulty,
                onFinished = { score ->
                    navController.navigate(Route.Results.build(GameMode.STATION_SPRINT, score)) {
                        popUpTo(Route.Home.path)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Results.path,
            arguments = listOf(
                navArgument("mode")  { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType }
            )
        ) { backStack ->
            val mode  = GameMode.valueOf(backStack.arguments?.getString("mode") ?: GameMode.TRACE_NETWORK.name)
            val score = backStack.arguments?.getInt("score") ?: 0
            ResultsScreen(
                lineId = if (mode == GameMode.TRACE_NETWORK) "NETWORK" else "SPRINT",
                score  = score,
                onHome  = { navController.navigate(Route.Home.path) { popUpTo(Route.Home.path) } },
                onRetry = { navController.navigate(Route.Home.path) { popUpTo(Route.Home.path) } }
            )
        }

        composable(Route.Stats.path) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
    }
}
