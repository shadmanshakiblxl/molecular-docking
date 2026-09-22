package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.DockingRecord
import com.example.data.local.DockingRepository
import com.example.data.local.DrugDatabase
import com.example.data.model.*
import com.example.docking.ConformerGenerator
import com.example.docking.DockingEngine
import com.example.docking.PdbService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrugDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        DrugDatabase::class.java,
        "drug_discovery.db"
    ).fallbackToDestructiveMigration().build()

    private val repository = DockingRepository(db.dockingDao())
    private val pdbService = PdbService()
    private val conformerGenerator = ConformerGenerator()
    private val dockingEngine = DockingEngine()

    // Splash & Auth State
    private val _hasCompletedSplash = MutableStateFlow(false)
    val hasCompletedSplash: StateFlow<Boolean> = _hasCompletedSplash.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    // Mode: default Light mode
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Navigation Tab (0: Input, 1: 3D Viewer, 2: Report, 3: Database)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Viewer Mode (1: Surface, 2: Pocket, 3: Interaction Map, 4: Split Comparison)
    private val _viewerMode = MutableStateFlow(1)
    val viewerMode: StateFlow<Int> = _viewerMode.asStateFlow()

    // Input States
    private val _pdbIdInput = MutableStateFlow("1HSG")
    val pdbIdInput: StateFlow<String> = _pdbIdInput.asStateFlow()

    private val _smilesInput = MutableStateFlow("CC(=O)Oc1ccccc1C(=O)O")
    val smilesInput: StateFlow<String> = _smilesInput.asStateFlow()

    private val _ligandNameInput = MutableStateFlow("Aspirin (Acetylsalicylic acid)")
    val ligandNameInput: StateFlow<String> = _ligandNameInput.asStateFlow()

    private val _bindingSiteMode = MutableStateFlow(BindingSiteMode.REFERENCE_LIGAND)
    val bindingSiteMode: StateFlow<BindingSiteMode> = _bindingSiteMode.asStateFlow()

    private val _customCenterX = MutableStateFlow("15.2")
    val customCenterX: StateFlow<String> = _customCenterX.asStateFlow()

    private val _customCenterY = MutableStateFlow("24.6")
    val customCenterY: StateFlow<String> = _customCenterY.asStateFlow()

    private val _customCenterZ = MutableStateFlow("5.2")
    val customCenterZ: StateFlow<String> = _customCenterZ.asStateFlow()

    // Docking Execution State
    private val _isDocking = MutableStateFlow(false)
    val isDocking: StateFlow<Boolean> = _isDocking.asStateFlow()

    private val _dockingStep = MutableStateFlow(0)
    val dockingStep: StateFlow<Int> = _dockingStep.asStateFlow()

    private val _dockingStatusMessage = MutableStateFlow("")
    val dockingStatusMessage: StateFlow<String> = _dockingStatusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Active Results
    private val _currentResult = MutableStateFlow<DockingRunResult?>(null)
    val currentResult: StateFlow<DockingRunResult?> = _currentResult.asStateFlow()

    // Saved database history
    val historyRecords: StateFlow<List<DockingRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-run an initial benchmark demonstration so the user immediately has interactive 3D visualizations
        viewModelScope.launch {
            runMolecularDocking(autoSwitchToViewer = false)
        }
    }

    fun completeSplashWithGmail(email: String = "shadman.shakiblxl@gmail.com") {
        _userEmail.value = email
        _hasCompletedSplash.value = true
    }

    fun completeSplashDirectAccess() {
        _userEmail.value = "Direct Access Guest"
        _hasCompletedSplash.value = true
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun selectViewerMode(mode: Int) {
        _viewerMode.value = mode
    }

    fun setPdbId(value: String) {
        _pdbIdInput.value = value.trim()
    }

    fun setSmiles(value: String) {
        _smilesInput.value = value.trim()
    }

    fun setBindingSiteMode(mode: BindingSiteMode) {
        _bindingSiteMode.value = mode
    }

    fun setCustomCoordinates(x: String, y: String, z: String) {
        _customCenterX.value = x
        _customCenterY.value = y
        _customCenterZ.value = z
    }

    fun applyTargetPreset(pdbId: String, desc: String) {
        _pdbIdInput.value = pdbId
    }

    fun applyLigandPreset(name: String, smiles: String) {
        _ligandNameInput.value = name
        _smilesInput.value = smiles
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun runMolecularDocking(autoSwitchToViewer: Boolean = true) {
        if (_isDocking.value) return

        viewModelScope.launch {
            _isDocking.value = true
            _errorMessage.value = null
            _dockingStep.value = 1
            _dockingStatusMessage.value = "Fetching and preparing target protein PDB..."

            try {
                val pdbId = _pdbIdInput.value.ifBlank { "1HSG" }
                val smiles = _smilesInput.value.ifBlank { "CC(=O)Oc1ccccc1C(=O)O" }

                // 1. Fetch & Parse Protein Structure
                val protein = pdbService.fetchOrLoadProtein(pdbId)

                // 2. Generate 3D Ligand Conformer
                val ligand = conformerGenerator.generateConformer(smiles, _ligandNameInput.value)

                // 3. Custom coordinates if requested
                val customCenter = if (_bindingSiteMode.value == BindingSiteMode.USER_SELECTED) {
                    Triple(
                        _customCenterX.value.toDoubleOrNull() ?: 15.2,
                        _customCenterY.value.toDoubleOrNull() ?: 24.6,
                        _customCenterZ.value.toDoubleOrNull() ?: 5.2
                    )
                } else null

                // 4. Run Docking Pipeline
                val result = dockingEngine.runPipeline(
                    protein = protein,
                    ligand = ligand,
                    bindingMode = _bindingSiteMode.value,
                    customPocketCenter = customCenter,
                    onProgress = { step, msg ->
                        _dockingStep.value = step
                        _dockingStatusMessage.value = msg
                    }
                )

                _currentResult.value = result

                // Automatically save run to Room database
                repository.insertRecord(
                    DockingRecord(
                        pdbId = result.protein.pdbId,
                        proteinTitle = result.protein.title,
                        smiles = result.ligand.smiles,
                        ligandName = result.ligand.name,
                        dockingScoreKcalMol = result.bestPose.scoreKcalMol,
                        predictedKdFormatted = result.affinity.formattedKd,
                        hBondsCount = result.hydrogenBonds.size,
                        hydrophobicCount = result.hydrophobicContacts.size,
                        bindingSiteMode = result.bindingSiteMode.title,
                        pocketVolume = result.pocketMetrics.pocketVolumeAngstrom3,
                        ligandEfficiency = result.report.ligandEfficiency,
                        lipinskiPass = result.report.lipinskiPass,
                        clinicalSuitability = result.report.clinicalPhaseViability,
                        timestamp = result.timestamp
                    )
                )

                if (autoSwitchToViewer) {
                    _selectedTab.value = 1 // Switch to 3D Viewer tab
                }

            } catch (e: Exception) {
                _errorMessage.value = "Pipeline Error: ${e.localizedMessage ?: "Docking computation failed"}"
            } finally {
                _isDocking.value = false
            }
        }
    }

    fun reloadFromHistory(record: DockingRecord) {
        viewModelScope.launch {
            _pdbIdInput.value = record.pdbId
            _smilesInput.value = record.smiles
            _ligandNameInput.value = record.ligandName
            runMolecularDocking(autoSwitchToViewer = true)
        }
    }

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecordById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
