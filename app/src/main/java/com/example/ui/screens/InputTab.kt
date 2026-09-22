package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BindingSiteMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputTab(
    pdbId: String,
    onPdbIdChange: (String) -> Unit,
    smiles: String,
    onSmilesChange: (String) -> Unit,
    bindingSiteMode: BindingSiteMode,
    onBindingSiteModeChange: (BindingSiteMode) -> Unit,
    customX: String,
    customY: String,
    customZ: String,
    onCoordinatesChange: (x: String, y: String, z: String) -> Unit,
    onApplyTargetPreset: (pdbId: String, desc: String) -> Unit,
    onApplyLigandPreset: (name: String, smiles: String) -> Unit,
    onRunAnalysis: () -> Unit,
    isDocking: Boolean,
    dockingStep: Int,
    dockingStatus: String,
    errorMessage: String?,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Target Protein Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Target Protein Receptor (PDB)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pdbId,
                    onValueChange = onPdbIdChange,
                    label = { Text("PDB ID (e.g. 1HSG, 6LU7)") },
                    placeholder = { Text("Enter 4-character RCSB PDB ID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "PDB ID"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdb_id_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quick Presets:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "1HSG" to "HIV-1 Protease",
                        "6LU7" to "SARS-CoV-2 Mpro",
                        "4EK3" to "EGFR Kinase",
                        "1E66" to "Acetylcholinesterase"
                    )
                    presets.forEach { (id, desc) ->
                        SuggestionChip(
                            onClick = { onApplyTargetPreset(id, desc) },
                            label = { Text("$id ($desc)", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("target_preset_$id")
                        )
                    }
                }
            }
        }

        // Small Molecule Ligand Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Small Molecule Ligand (SMILES)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = smiles,
                    onValueChange = onSmilesChange,
                    label = { Text("SMILES String") },
                    placeholder = { Text("e.g. CC(=O)Oc1ccccc1C(=O)O") },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Grain,
                            contentDescription = "SMILES"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smiles_string_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ligand Library Presets:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ligandPresets = listOf(
                        "Aspirin" to "CC(=O)Oc1ccccc1C(=O)O",
                        "Nirmatrelvir" to "CC(C)CN1CC(C1)C(=O)N",
                        "Imatinib" to "c1ccc(cc1)Nc2nccc(n2)c3cccnc3",
                        "Ritonavir" to "CC(C)c1nc(cn1C)C",
                        "Ibuprofen" to "CC(C)Cc1ccc(cc1)C(C)C(=O)O"
                    )
                    ligandPresets.forEach { (name, s) ->
                        SuggestionChip(
                            onClick = { onApplyLigandPreset(name, s) },
                            label = { Text(name, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("ligand_preset_$name")
                        )
                    }
                }
            }
        }

        // Binding Site Docking Determination Options
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Binding Site Specification",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3 Options from User Prompt
                BindingSiteMode.values().forEach { mode ->
                    val isSelected = bindingSiteMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBindingSiteModeChange(mode) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onBindingSiteModeChange(mode) },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = mode.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // If Option 1: User coordinates
                AnimatedVisibility(visible = bindingSiteMode == BindingSiteMode.USER_SELECTED) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Pocket Center Coordinates (Å):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customX,
                                onValueChange = { onCoordinatesChange(it, customY, customZ) },
                                label = { Text("X") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customY,
                                onValueChange = { onCoordinatesChange(customX, it, customZ) },
                                label = { Text("Y") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customZ,
                                onValueChange = { onCoordinatesChange(customX, customY, it) },
                                label = { Text("Z") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Analysis Button
        Button(
            onClick = onRunAnalysis,
            enabled = !isDocking,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("analysis_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            if (isDocking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("DOCKING & ANALYSIS PIPELINE RUNNING...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Docking"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUN MOLECULAR DOCKING PIPELINE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Pipeline Status Modal/Indicator when running
        if (isDocking) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Pipeline Execution (Step $dockingStep/6)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dockingStatus,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { dockingStep / 6.0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
