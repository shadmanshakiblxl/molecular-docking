package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DockingRecord
import com.example.data.model.BindingSiteMode
import com.example.data.model.DockingRunResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDiscoveryScreen(
    currentResult: DockingRunResult?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    viewerMode: Int,
    onViewerModeSelected: (Int) -> Unit,
    pdbId: String,
    onPdbIdChange: (String) -> Unit,
    smiles: String,
    onSmilesChange: (String) -> Unit,
    bindingSiteMode: BindingSiteMode,
    onBindingSiteModeChange: (BindingSiteMode) -> Unit,
    customX: String,
    customY: String,
    customZ: String,
    onCoordinatesChange: (String, String, String) -> Unit,
    onApplyTargetPreset: (String, String) -> Unit,
    onApplyLigandPreset: (String, String) -> Unit,
    onRunAnalysis: () -> Unit,
    isDocking: Boolean,
    dockingStep: Int,
    dockingStatus: String,
    errorMessage: String?,
    onDismissError: () -> Unit,
    historyRecords: List<DockingRecord>,
    onSelectRecord: (DockingRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    userEmail: String?,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "&GEN",
                            fontWeight = FontWeight.ExtraLight,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 3.sp,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DRUG",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Dark / Light Mode Toggle
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Mode",
                            tint = if (isDarkMode) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Info / Docs
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About Pipeline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                val navItems = listOf(
                    Triple(0, "Input", Icons.Default.Input to Icons.Outlined.Input),
                    Triple(1, "3D View", Icons.Default.ViewInAr to Icons.Outlined.ViewInAr),
                    Triple(2, "Report", Icons.Default.Assessment to Icons.Outlined.Assessment),
                    Triple(3, "Database", Icons.Default.Storage to Icons.Outlined.Storage)
                )

                navItems.forEach { (index, title, iconPair) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) iconPair.first else iconPair.second,
                                contentDescription = title
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_$index")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> InputTab(
                    pdbId = pdbId,
                    onPdbIdChange = onPdbIdChange,
                    smiles = smiles,
                    onSmilesChange = onSmilesChange,
                    bindingSiteMode = bindingSiteMode,
                    onBindingSiteModeChange = onBindingSiteModeChange,
                    customX = customX,
                    customY = customY,
                    customZ = customZ,
                    onCoordinatesChange = onCoordinatesChange,
                    onApplyTargetPreset = onApplyTargetPreset,
                    onApplyLigandPreset = onApplyLigandPreset,
                    onRunAnalysis = onRunAnalysis,
                    isDocking = isDocking,
                    dockingStep = dockingStep,
                    dockingStatus = dockingStatus,
                    errorMessage = errorMessage,
                    onDismissError = onDismissError
                )

                1 -> ViewerTab(
                    result = currentResult,
                    selectedMode = viewerMode,
                    onModeSelected = onViewerModeSelected,
                    isDark = isDarkMode,
                    onNavigateToReport = { onTabSelected(2) },
                    onNavigateToInput = { onTabSelected(0) }
                )

                2 -> ReportTab(
                    result = currentResult,
                    onNavigateToViewer = { onTabSelected(1) }
                )

                3 -> DatabaseTab(
                    records = historyRecords,
                    onSelectRecord = { record ->
                        onSelectRecord(record)
                    },
                    onDeleteRecord = onDeleteRecord,
                    onClearAll = onClearAllHistory
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "&GEN DRUG Pipeline Architecture",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• 3Dmol.js: High performance WebGL molecular graphics engine rendering cartoon, surface, stick and ball-and-stick representations.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• RDKit Conformers: Distance geometry ETKDG conformer generator calculating 3D coordinates and heavy atom valencies directly from SMILES.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• AutoDock Vina Physics: Energetic scoring assessing Gauss attractions, steric repulsion, hydrophobic contacts, and hydrogen bond potentials.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• RCSB PDB: Direct structure streaming and topology curation of target receptor proteins.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Room Database: Local encrypted persistence for historical complex records and binding affinities.",
                        fontSize = 12.sp
                    )
                    if (userEmail != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Active Session: $userEmail",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
