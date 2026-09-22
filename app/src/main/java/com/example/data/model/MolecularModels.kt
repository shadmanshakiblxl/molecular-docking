package com.example.data.model

data class Atom3D(
    val id: Int,
    val name: String,
    val element: String,
    val residueName: String = "LIG",
    val residueNum: Int = 1,
    val chain: String = "A",
    val x: Double,
    val y: Double,
    val z: Double,
    val charge: Double = 0.0,
    val isHetero: Boolean = false,
    val isHydrogen: Boolean = false
) {
    fun distanceTo(other: Atom3D): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return Math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun toPdbLine(atomIndex: Int): String {
        val recordType = if (isHetero) "HETATM" else "ATOM  "
        return String.format(
            java.util.Locale.US,
            "%-6s%5d %-4s %3s %1s%4d    %8.3f%8.3f%8.3f%6.2f%6.2f          %2s",
            recordType,
            atomIndex,
            if (name.length < 4) " " + name else name.take(4),
            residueName.take(3),
            chain.take(1),
            residueNum,
            x, y, z,
            1.00, 0.00,
            element.take(2)
        )
    }
}

data class ProteinStructure(
    val pdbId: String,
    val title: String,
    val resolution: String,
    val chains: List<String>,
    val residuesCount: Int,
    val atoms: List<Atom3D>,
    val rawPdbContent: String,
    val cleanedPdbContent: String,
    val referenceLigandName: String? = null,
    val referenceLigandCenter: Triple<Double, Double, Double>? = null,
    val detectedPockets: List<PocketInfo> = emptyList()
)

data class PocketInfo(
    val id: Int,
    val name: String,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val sizeX: Double = 22.0,
    val sizeY: Double = 22.0,
    val sizeZ: Double = 22.0,
    val volume: Double,
    val keyResidues: List<String>,
    val druggabilityScore: Double
)

enum class BindingSiteMode(val title: String, val subtitle: String) {
    USER_SELECTED("User Defined", "Specify custom coordinates or cavity center"),
    REFERENCE_LIGAND("Reference Ligand", "Derive docking pocket from co-crystallized ligand"),
    AUTO_POCKET("Automated Detection", "Algorithmic pocket & cavity detection")
}

data class LigandConformer(
    val smiles: String,
    val name: String,
    val formula: String,
    val molecularWeight: Double,
    val logP: Double,
    val hBondDonors: Int,
    val hBondAcceptors: Int,
    val rotatableBonds: Int,
    val heavyAtomCount: Int,
    val atoms: List<Atom3D>,
    val bonds: List<Pair<Int, Int>> = emptyList()
) {
    fun toPdbString(): String {
        val sb = StringBuilder()
        sb.append("COMPND    ").append(name).append("\n")
        atoms.forEachIndexed { idx, atom ->
            sb.append(atom.toPdbLine(idx + 1)).append("\n")
        }
        bonds.forEach { (a1, a2) ->
            sb.append(String.format(java.util.Locale.US, "CONECT%5d%5d\n", a1 + 1, a2 + 1))
        }
        sb.append("END\n")
        return sb.toString()
    }
}

data class DockingPose(
    val poseRank: Int,
    val scoreKcalMol: Double, // AutoDock Vina affinity in kcal/mol
    val rmsdLowerBound: Double = 0.0,
    val rmsdUpperBound: Double = 0.0,
    val atoms: List<Atom3D>
) {
    fun toPdbString(ligandName: String = "LIG"): String {
        val sb = StringBuilder()
        sb.append("MODEL     ").append(poseRank).append("\n")
        atoms.forEachIndexed { idx, atom ->
            sb.append(atom.toPdbLine(idx + 1)).append("\n")
        }
        sb.append("ENDMDL\n")
        return sb.toString()
    }
}

data class HydrogenBond(
    val donorResidue: String,
    val donorAtom: String,
    val acceptorResidue: String,
    val acceptorAtom: String,
    val distanceAngstrom: Double,
    val bondType: String = "Standard H-Bond",
    val estimatedEnergyKcal: Double
)

data class HydrophobicContact(
    val residueName: String,
    val residueNumber: Int,
    val residueAtom: String,
    val ligandAtom: String,
    val distanceAngstrom: Double
)

data class ResidueDistance(
    val residueName: String,
    val residueNumber: Int,
    val chain: String,
    val minDistanceAngstrom: Double,
    val role: String // e.g. "Catalytic Dyad", "S1 Subsite", "Oxyanion Hole", "Hydrophobic Pocket"
)

data class PocketMetrics(
    val pocketVolumeAngstrom3: Double,
    val surfaceAreaAngstrom2: Double,
    val hydrophobicityRatio: Double,
    val pocketDepthAngstrom: Double,
    val polarityIndex: Double
)

data class BindingAffinityMetrics(
    val dockingScoreKcalMol: Double,
    val kdMolar: Double = 0.0,
    val formattedKd: String = "",
    val predictedKdNanoMolar: Double = 0.0,
    val predictedKiNanoMolar: Double = 0.0,
    val predictedIC50NanoMolar: Double = 0.0,
    val experimentalDisclaimer: String = "Experimental values such as Kd, Ki, IC50 are obtained through specific laboratory measurements and should not be substituted with docking scores.",
    val predictedAffinityNote: String = "Predicted binding affinity is derived from Boltzmann relation at 298.15K (ΔG = RT ln Kd). Validated across universal heavy-atom empirical scaling."
)

data class PharmaceuticalReport(
    val ligandEfficiency: Double, // LE = -ΔG / N_heavy
    val bindingEfficiencyIndex: Double, // BEI = pKd / MW * 1000
    val surfaceEfficiencyIndex: Double, // SEI = pKd / PSA * 100
    val lipinskiPass: Boolean,
    val lipinskiViolations: List<String>,
    val clinicalPhaseViability: String,
    val leadOptimizationAdvice: String,
    val admetIndicators: List<Pair<String, String>>
)

data class DockingRunResult(
    val id: Long = System.currentTimeMillis(),
    val protein: ProteinStructure,
    val ligand: LigandConformer,
    val bindingSiteMode: BindingSiteMode,
    val pocketCenter: Triple<Double, Double, Double>,
    val pocketSize: Triple<Double, Double, Double>,
    val poses: List<DockingPose>,
    val bestPose: DockingPose,
    val hydrogenBonds: List<HydrogenBond>,
    val hydrophobicContacts: List<HydrophobicContact>,
    val residueDistances: List<ResidueDistance>,
    val pocketMetrics: PocketMetrics,
    val affinity: BindingAffinityMetrics,
    val report: PharmaceuticalReport,
    val timestamp: Long = System.currentTimeMillis()
)
