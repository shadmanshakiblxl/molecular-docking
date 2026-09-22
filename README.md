# &GEN DRUG — Molecular Docking & Drug Discovery App

**&GEN DRUG** is an advanced, Android-native computational chemistry and structure-based drug design application. Built using Kotlin, Jetpack Compose, and integrated 3D WebGL rendering engine components, it enables real-time molecular docking simulations, affinity scoring, binding pocket metrics, and interactive 3D protein–ligand visualization.

---

## 🌟 Key Features

* **Cinematic Onboarding:** Minimalist, high-definition initial loading experience with smooth animations (`CinematicSplashScreen`).
* **Universal Target & Ligand Inputs:** Input any RCSB PDB ID (e.g., `1HSG`, `6LU7`) and SMILES chemical structure (e.g., Aspirin, Ritonavir) with dynamic structural validation (`InputTab`).
* **Interactive 3D Molecular Viewer:** Powered by embedded HTML WebGL visualizers (`3Dmol.js`) for displaying protein cartoons, surfaces, ligand stick/ball-and-stick models, and hydrogen bond interaction vectors (`ViewerTab`, `MolecularViewerHtml`).
* **Dynamic Calculation Engine:** Computes binding free energy ($\Delta G$ in kcal/mol), predicted dissociation constants ($K_d$, $K_i$), Ligand Efficiency ($LE$), BEI, and SEI metrics without hardcoded static assumptions (`MolecularCalculationEngine`, `DockingEngine`).
* **In-Depth Interaction Reports:** Structural residue proximity tables, hydrogen bond geometry distance metrics, and pocket volume calculations ($\text{Å}^3$) (`ReportTab`).
* **Local Database Storage:** Built-in persistence for previous docking runs and compound analysis history (`DrugDatabase`, `DockingRepository`).

---

## 🏗 Project Architecture

The project follows Modern Android Architecture principles (MVVM + Jetpack Compose Clean Architecture):

```text
com.example
├── data/
│   ├── local/            # Room Database & Local Repositories (DrugDatabase, DockingRepository)
│   └── model/            # Data models for protein, ligand, and docking results (MolecularModels)
├── docking/              # Core Computational Logic
│   ├── ConformerGenerator.kt           # 3D ligand conformer generation
│   ├── DockingEngine.kt                # Pose estimation & scoring routines
│   ├── MolecularCalculationEngine.kt   # Thermodynamic & efficiency metric formulas
│   └── PdbService.kt                   # RCSB PDB structure fetching service
├── ui/
│   ├── components/       # Reusable UI elements & HTML 3D WebGL Wrappers (MolecularViewer)
│   ├── screens/          # Application Tabs (InputTab, ViewerTab, ReportTab, DatabaseTab)
│   ├── theme/            # Color palettes, typography, and light/dark theme definitions
│   └── viewmodel/        # State Management (DrugDiscoveryViewModel)
└── MainActivity.kt       # Application entry point

🛠 Tech Stack & Tools
Language: Kotlin

UI Framework: Jetpack Compose (Material 3 UI elements)

Architecture: MVVM with ViewModel & StateFlow

Rendering Engine: WebGL / 3Dmol.js (integrated via Android WebView wrapper)

Asynchronous Execution: Kotlin Coroutines & Flow

Build System: Gradle (Kotlin DSL - .gradle.kts)

Testing: JUnit, Robolectric, and Screenshot Testing

🚀 Getting Started
Prerequisites
Android Studio: Ladybug / Iguana or newer

JDK: Java 17+

Android SDK: API Level 26+ (Android 8.0+)



📄 License & Attribution
Developer & Author: SHADMAN SHAKIB (&GEN)






