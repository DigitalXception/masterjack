package com.strategy.blackjacktrainer

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strategy.blackjacktrainer.logic.TrainerViewModel
import com.strategy.blackjacktrainer.logic.TrainerViewModelFactory
import com.strategy.blackjacktrainer.ui.screens.MistakesScreen
import com.strategy.blackjacktrainer.ui.screens.StrategyChartScreen
import com.strategy.blackjacktrainer.ui.screens.TrainerPipScreen
import com.strategy.blackjacktrainer.ui.screens.TrainerScreen
import com.strategy.blackjacktrainer.ui.theme.AppColorMode
import com.strategy.blackjacktrainer.ui.theme.BlackjackTrainerTheme
import com.strategy.blackjacktrainer.ui.theme.Emerald
import com.strategy.blackjacktrainer.ui.theme.SurfaceDark
import com.strategy.blackjacktrainer.ui.theme.TableBg
import com.strategy.blackjacktrainer.ui.theme.TextSecondary

private enum class Tab(val label: String) { TRAIN("Train"), CHART("Chart"), REVIEW("Review") }

class MainActivity : ComponentActivity() {
    private var isPip by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = getSharedPreferences("trainer_settings", MODE_PRIVATE)
        AppColorMode.blackAndWhite = settings.getBoolean("black_and_white", false)
        setContent {
            BlackjackTrainerTheme {
                val vm: TrainerViewModel = viewModel(
                    factory = TrainerViewModelFactory(applicationContext)
                )
                AppScaffold(vm, isPip) { enterTrainerPictureInPicture() }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPip = isInPictureInPictureMode
    }

    private fun enterTrainerPictureInPicture() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }
}

@Composable
private fun AppScaffold(vm: TrainerViewModel, isPip: Boolean, onPopOut: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.TRAIN) }

    if (isPip) {
        TrainerPipScreen(vm)
        return
    }

    Scaffold(
        containerColor = TableBg,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                NavigationBarItem(
                    selected = tab == Tab.TRAIN,
                    onClick = { tab = Tab.TRAIN },
                    icon = { Icon(Icons.Filled.Style, contentDescription = null) },
                    label = { Text("Train") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald,
                        selectedTextColor = Emerald,
                        indicatorColor = Emerald.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = tab == Tab.CHART,
                    onClick = { tab = Tab.CHART },
                    icon = { Icon(Icons.Filled.GridOn, contentDescription = null) },
                    label = { Text("Chart") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald,
                        selectedTextColor = Emerald,
                        indicatorColor = Emerald.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = tab == Tab.REVIEW,
                    onClick = { tab = Tab.REVIEW },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Review") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald,
                        selectedTextColor = Emerald,
                        indicatorColor = Emerald.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TableBg)
                .padding(padding)
        ) {
            when (tab) {
                Tab.TRAIN -> TrainerScreen(vm, onPopOut)
                Tab.CHART -> StrategyChartScreen(vm)
                Tab.REVIEW -> MistakesScreen(vm)
            }
        }
    }
}
