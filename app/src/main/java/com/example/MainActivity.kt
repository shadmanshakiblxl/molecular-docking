package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CinematicSplashScreen
import com.example.ui.screens.MainDiscoveryScreen
import com.example.ui.theme.DRUGTheme
import com.example.ui.viewmodel.DrugDiscoveryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DrugDiscoveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            val hasCompletedSplash by viewModel.hasCompletedSplash.collectAsState()
            val userEmail by viewModel.userEmail.collectAsState()

            val selectedTab by viewModel.selectedTab.collectAsState()
            val viewerMode by viewModel.viewerMode.collectAsState()
            val currentResult by viewModel.currentResult.collectAsState()

            val pdbId by viewModel.pdbIdInput.collectAsState()
            val smiles by viewModel.smilesInput.collectAsState()
            val bindingSiteMode by viewModel.bindingSiteMode.collectAsState()
            val customX by viewModel.customCenterX.collectAsState()
            val customY by viewModel.customCenterY.collectAsState()
            val customZ by viewModel.customCenterZ.collectAsState()

            val isDocking by viewModel.isDocking.collectAsState()
            val dockingStep by viewModel.dockingStep.collectAsState()
            val dockingStatus by viewModel.dockingStatusMessage.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()

            val historyRecords by viewModel.historyRecords.collectAsState()

            DRUGTheme(darkTheme = isDark) {
                Crossfade(
                    targetState = hasCompletedSplash,
                    animationSpec = tween(600),
                    label = "splash_transition"
                ) { completed ->
                    if (!completed) {
                        CinematicSplashScreen(
                            onContinueWithGmail = { viewModel.completeSplashWithGmail() },
                            onDirectAccess = { viewModel.completeSplashDirectAccess() },
                            isDark = isDark,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MainDiscoveryScreen(
                            currentResult = currentResult,
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            viewerMode = viewerMode,
                            onViewerModeSelected = { viewModel.selectViewerMode(it) },
                            pdbId = pdbId,
                            onPdbIdChange = { viewModel.setPdbId(it) },
                            smiles = smiles,
                            onSmilesChange = { viewModel.setSmiles(it) },
                            bindingSiteMode = bindingSiteMode,
                            onBindingSiteModeChange = { viewModel.setBindingSiteMode(it) },
                            customX = customX,
                            customY = customY,
                            customZ = customZ,
                            onCoordinatesChange = { x, y, z -> viewModel.setCustomCoordinates(x, y, z) },
                            onApplyTargetPreset = { id, desc -> viewModel.applyTargetPreset(id, desc) },
                            onApplyLigandPreset = { name, s -> viewModel.applyLigandPreset(name, s) },
                            onRunAnalysis = { viewModel.runMolecularDocking() },
                            isDocking = isDocking,
                            dockingStep = dockingStep,
                            dockingStatus = dockingStatus,
                            errorMessage = errorMessage,
                            onDismissError = { viewModel.clearError() },
                            historyRecords = historyRecords,
                            onSelectRecord = { record -> viewModel.reloadFromHistory(record) },
                            onDeleteRecord = { id -> viewModel.deleteHistoryRecord(id) },
                            onClearAllHistory = { viewModel.clearAllHistory() },
                            isDarkMode = isDark,
                            onToggleDarkMode = { viewModel.toggleDarkMode() },
                            userEmail = userEmail,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
