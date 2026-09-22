package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.DockingRunResult
import com.example.ui.theme.BioAffinityGood
import com.example.ui.theme.BioAffinityModerate
import com.example.ui.theme.BioAffinityWeak

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MolecularViewer(
    result: DockingRunResult?,
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // When mode changes, evaluate JS on existing WebView if available
    LaunchedEffect(selectedMode) {
        webViewRef?.evaluateJavascript("if(typeof setViewerMode === 'function') setViewerMode($selectedMode);", null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // WebView 3Dmol.js Canvas
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // In environments or emulators without hardware OpenGL rendernode (e.g. headless Mesa / software renderers),
                    // allow smooth software layer fallback to prevent rendering crashes
                    try {
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    } catch (_: Exception) {
                        // Keep default layer type if not supported
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webViewClient = WebViewClient()
                    setBackgroundColor(if (isDark) 0xFF070C18.toInt() else 0xFFF8FAFC.toInt())
                    webViewRef = this
                    val html = MolecularViewerHtml.generateHtml(result, selectedMode, isDark)
                    loadDataWithBaseURL("https://3dmol.org/", html, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webViewRef = webView
                val html = MolecularViewerHtml.generateHtml(result, selectedMode, isDark)
                webView.loadDataWithBaseURL("https://3dmol.org/", html, "text/html", "UTF-8", null)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("molecular_3d_webview")
        )

        // Top Header Mode Pills
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modes = listOf(
                        1 to "1. Surface",
                        2 to "2. Pocket",
                        3 to "3. Interaction",
                        4 to "4. Split Before/After"
                    )

                    modes.forEach { (modeNum, label) ->
                        val isSelected = selectedMode == modeNum
                        FilterChip(
                            selected = isSelected,
                            onClick = { onModeSelected(modeNum) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("mode_chip_$modeNum")
                        )
                    }
                }
            }
        }

        // Bottom Metrics Overlay Card (if result available)
        if (result != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp, end = 12.dp)
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val score = result.bestPose.scoreKcalMol
                            val badgeColor = when {
                                score <= -9.0 -> BioAffinityGood
                                score <= -7.5 -> BioAffinityModerate
                                else -> BioAffinityWeak
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AFFINITY: ${result.bestPose.scoreKcalMol} kcal/mol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Kd ~ ${result.affinity.predictedKdNanoMolar} nM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "PDB: ${result.protein.pdbId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "H-Bonds: ${result.hydrogenBonds.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pocket: ${result.pocketMetrics.pocketVolumeAngstrom3.toInt()} Å³",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
