package com.example.docking

import java.util.Locale
import kotlin.math.*

/**
 * Pure mathematical and algorithmic calculation engine for universal PDB and SMILES evaluation.
 * Does not rely on hardcoded presets or example defaults.
 */
object MolecularCalculationEngine {

    // Standard atomic weights (IUPAC standard values)
    val ATOM_WEIGHTS = mapOf(
        "C" to 12.011,
        "N" to 14.007,
        "O" to 15.999,
        "S" to 32.060,
        "P" to 30.974,
        "F" to 18.998,
        "CL" to 35.453,
        "BR" to 79.904,
        "I" to 126.904,
        "B" to 10.810,
        "SI" to 28.085,
        "SE" to 78.971,
        "H" to 1.008
    )

    data class ParsedSmilesResult(
        val sanitizedSmiles: String,
        val heavyAtomCount: Int,
        val atomCounts: Map<String, Int>,
        val estimatedHydrogenCount: Int,
        val molecularWeight: Double,
        val polarAtomCount: Int, // Count of O + N
        val polarSurfaceAreaPsa: Double,
        val estimatedHBondDonors: Int,
        val estimatedHBondAcceptors: Int,
        val chemicalFormula: String
    )

    data class ThermodynamicAffinityMetrics(
        val deltaGKcalMol: Double,
        val ligandEfficiency: Double,
        val kdMolar: Double,
        val formattedKd: String,
        val kdNanoMolar: Double,
        val pKd: Double,
        val bei: Double,
        val sei: Double,
        val pocketVolume: Double,
        val estimatedHBonds: Int
    )

    /**
     * 1. Robust SMILES Parsing & Sanitization (Universal Fallback Safety)
     * - Input Cleaning: Trim whitespace, strip trailing punctuation/characters, sanitize
     * - Heavy Atom Calculation (HA): Count valid atomic symbols representing non-hydrogen heavy atoms
     * - Exclude explicit hydrogens (H, h) and structural characters
     * - Edge-Case Guard: If HA = 0 or empty/invalid, safely fall back to HA = 1
     */
    fun parseAndSanitizeSmiles(rawSmiles: String): ParsedSmilesResult {
        // Step 1: Input cleaning & sanitization
        val sanitized = rawSmiles
            .trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace("\t", "")
            .replace("\"", "")
            .replace("'", "")
            .trimEnd('.', ';', ',', ' ')

        val atomCounts = mutableMapOf<String, Int>()
        var polarCount = 0
        var donorCount = 0
        var acceptorCount = 0
        var rawHeavyAtomCount = 0

        var i = 0
        val len = sanitized.length

        while (i < len) {
            val c = sanitized[i]
            when {
                // Bracketed atom: e.g. [nH], [O-], [NH3+], [Fe+2], [C@@H], [13C]
                c == '[' -> {
                    val closeIdx = sanitized.indexOf(']', i)
                    if (closeIdx != -1) {
                        val bracketContent = sanitized.substring(i + 1, closeIdx)
                        // Extract element symbol ignoring leading isotope digits
                        var k = 0
                        while (k < bracketContent.length && bracketContent[k].isDigit()) {
                            k++
                        }
                        if (k < bracketContent.length) {
                            var elemStr = bracketContent[k].toString()
                            if (k + 1 < bracketContent.length && bracketContent[k + 1].isLowerCase() && bracketContent[k].isUpperCase()) {
                                elemStr += bracketContent[k + 1]
                            }
                            val normElem = normalizeElement(elemStr)
                            if (normElem != "H") {
                                rawHeavyAtomCount++
                                atomCounts[normElem] = atomCounts.getOrDefault(normElem, 0) + 1
                                if (normElem == "O" || normElem == "N") {
                                    polarCount++
                                    acceptorCount++
                                    if (bracketContent.contains("H", ignoreCase = true)) {
                                        donorCount++
                                    }
                                }
                            }
                        }
                        i = closeIdx + 1
                    } else {
                        i++
                    }
                }

                // Two-letter standard heavy atoms: Cl, Br, Si, Se
                c.isUpperCase() && i + 1 < len && sanitized[i + 1].isLowerCase() -> {
                    val twoLetter = sanitized.substring(i, i + 2)
                    val normElem = normalizeElement(twoLetter)
                    if (normElem in listOf("CL", "BR", "SI", "SE")) {
                        rawHeavyAtomCount++
                        val stdElem = if (normElem == "CL") "Cl" else if (normElem == "BR") "Br" else normElem
                        atomCounts[stdElem] = atomCounts.getOrDefault(stdElem, 0) + 1
                        i += 2
                    } else {
                        // Single uppercase letter
                        handleSingleLetter(c, atomCounts, { rawHeavyAtomCount++ }, { polarCount++ }, { donorCount++ }, { acceptorCount++ })
                        i++
                    }
                }

                // Single letter atoms (organic subset + aromatic lowercase)
                c.isLetter() -> {
                    if (c == 'H' || c == 'h') {
                        // Explicit hydrogen: excluded from heavy atoms
                        i++
                    } else {
                        handleSingleLetter(c, atomCounts, { rawHeavyAtomCount++ }, { polarCount++ }, { donorCount++ }, { acceptorCount++ })
                        i++
                    }
                }

                // Structural characters: bonds (=, #, -, :, ~, /, \), branches ( ), rings (0-9, %), dots (.)
                else -> {
                    i++
                }
            }
        }

        // Edge-Case Guard: If HA = 0 or empty/invalid, safely fall back to HA = 1
        val heavyAtomCount = maxOf(1, rawHeavyAtomCount)
        if (rawHeavyAtomCount == 0) {
            atomCounts["C"] = 1
        }

        // 2. Physical & Thermodynamic Metric Functions
        // Estimated Molecular Weight (MW)
        var mwHeavy = 0.0
        for ((elem, count) in atomCounts) {
            val weight = ATOM_WEIGHTS[elem.uppercase()] ?: 12.011
            mwHeavy += weight * count
        }

        // Estimate implicit hydrogens for neutral organic molecule
        val cCount = atomCounts.getOrDefault("C", 0)
        val nCount = atomCounts.getOrDefault("N", 0)
        val oCount = atomCounts.getOrDefault("O", 0)
        val halogens = atomCounts.getOrDefault("F", 0) + atomCounts.getOrDefault("Cl", 0) +
                atomCounts.getOrDefault("Br", 0) + atomCounts.getOrDefault("I", 0)

        // Saturated alkane base (2C + 2 + N - Halogens) adjusted for heavy atom graph
        val estimatedHydrogens = if (rawHeavyAtomCount == 0) {
            4 // Methane CH4 fallback
        } else {
            maxOf(0, (cCount * 2 + 2 + nCount - halogens - (oCount * 0.5).toInt()))
                .coerceAtLeast(1)
        }

        val totalMw = mwHeavy + (estimatedHydrogens * (ATOM_WEIGHTS["H"] ?: 1.008))
        val roundedMw = Math.round(maxOf(16.0, totalMw) * 100.0) / 100.0

        // Polar Surface Area estimate: PSA ≈ count(O, N) * 12
        val psa = maxOf(12.0, polarCount * 12.0)

        // Chemical formula string
        val formula = buildString {
            if (atomCounts.containsKey("C")) append("C").append(atomCounts["C"])
            if (estimatedHydrogens > 0) append("H").append(estimatedHydrogens)
            atomCounts.keys.filter { it != "C" && it != "H" }.sorted().forEach { el ->
                append(el).append(atomCounts[el])
            }
        }

        return ParsedSmilesResult(
            sanitizedSmiles = if (sanitized.isBlank()) "C" else sanitized,
            heavyAtomCount = heavyAtomCount,
            atomCounts = atomCounts,
            estimatedHydrogenCount = estimatedHydrogens,
            molecularWeight = roundedMw,
            polarAtomCount = polarCount,
            polarSurfaceAreaPsa = psa,
            estimatedHBondDonors = minOf(8, maxOf(0, donorCount)),
            estimatedHBondAcceptors = minOf(8, maxOf(0, acceptorCount)),
            chemicalFormula = formula
        )
    }

    /**
     * Compute all physical and thermodynamic metrics strictly from HA and estimated MW.
     */
    fun computeThermodynamicMetrics(
        heavyAtomCount: Int,
        molecularWeight: Double,
        polarAtomCount: Int
    ): ThermodynamicAffinityMetrics {
        val ha = maxOf(1, heavyAtomCount)
        val mw = maxOf(16.0, molecularWeight)

        // Dynamic Binding Affinity (ΔG):
        // ΔG = -min(12.5, max(3.5, 0.28 * HA + 1.2)) (in kcal/mol)
        // Clamped between -3.5 kcal/mol (weak) and -12.5 kcal/mol (strong)
        val rawDeltaG = -(minOf(12.5, maxOf(3.5, 0.28 * ha + 1.2)))
        val deltaG = Math.round(rawDeltaG * 100.0) / 100.0

        // Ligand Efficiency (LE):
        // LE = |ΔG| / HA (formatted to 2 decimal places)
        val ligandEfficiency = Math.round((abs(deltaG) / ha) * 100.0) / 100.0

        // Thermodynamic Dissociation Constant (Kd):
        // Kd = exp(ΔG / 0.59218) (Molar)
        // using standard Boltzmann relation at T = 298.15 K, R = 0.0019872 kcal/(mol·K)
        val kdMolar = exp(deltaG / 0.59218)
        val kdNanoMolar = Math.round((kdMolar * 1e9) * 100.0) / 100.0

        // Dynamic Unit Formatting:
        // If Kd >= 10^-3, display in mM (Kd * 10^3)
        // If 10^-6 <= Kd < 10^-3, display in μM (Kd * 10^6)
        // If Kd < 10^-6, display in nM (Kd * 10^9)
        val formattedKd = formatKd(kdMolar)

        // pKd = -log10(Kd)
        val pKd = -log10(maxOf(1e-15, kdMolar))

        // Efficiency Indices:
        // Binding Efficiency Index (BEI) = (pKd * 1000) / MW
        val bei = Math.round(((pKd * 1000.0) / mw) * 100.0) / 100.0

        // Surface Efficiency Index (SEI) = (pKd * 100) / PSA where PSA ≈ count(O, N) * 12
        val psa = maxOf(12.0, polarAtomCount * 12.0)
        val sei = Math.round(((pKd * 100.0) / psa) * 100.0) / 100.0

        // Pocket Volume (V): V = 300 + (HA * 18) Å³
        val pocketVolume = Math.round((300.0 + (ha * 18.0)) * 10.0) / 10.0

        // Dynamic Hydrogen Bonds estimated count (clamped between 0 and 8)
        val estimatedHBonds = minOf(8, maxOf(0, polarAtomCount))

        return ThermodynamicAffinityMetrics(
            deltaGKcalMol = deltaG,
            ligandEfficiency = ligandEfficiency,
            kdMolar = kdMolar,
            formattedKd = formattedKd,
            kdNanoMolar = kdNanoMolar,
            pKd = Math.round(pKd * 100.0) / 100.0,
            bei = bei,
            sei = sei,
            pocketVolume = pocketVolume,
            estimatedHBonds = estimatedHBonds
        )
    }

    /**
     * Dynamic Kd Unit Formatting according to exact specifications:
     * - Kd >= 10^-3 -> mM
     * - 10^-6 <= Kd < 10^-3 -> μM
     * - Kd < 10^-6 -> nM
     */
    fun formatKd(kdMolar: Double): String {
        return when {
            kdMolar >= 1e-3 -> {
                val mM = kdMolar * 1e3
                String.format(Locale.US, "%.2f mM", mM)
            }
            kdMolar >= 1e-6 -> {
                val uM = kdMolar * 1e6
                String.format(Locale.US, "%.2f μM", uM)
            }
            else -> {
                val nM = kdMolar * 1e9
                String.format(Locale.US, "%.2f nM", nM)
            }
        }
    }

    private fun handleSingleLetter(
        c: Char,
        atomCounts: MutableMap<String, Int>,
        onHeavyAtom: () -> Unit,
        onPolar: () -> Unit,
        onDonor: () -> Unit,
        onAcceptor: () -> Unit
    ) {
        val upper = c.uppercaseChar().toString()
        val standardElem = when (upper) {
            "C" -> "C"
            "N" -> "N"
            "O" -> "O"
            "S" -> "S"
            "P" -> "P"
            "F" -> "F"
            "I" -> "I"
            "B" -> "B"
            else -> "C"
        }
        onHeavyAtom()
        atomCounts[standardElem] = atomCounts.getOrDefault(standardElem, 0) + 1
        if (standardElem == "O" || standardElem == "N") {
            onPolar()
            onAcceptor()
            if (c.isUpperCase()) {
                onDonor()
            }
        }
    }

    private fun normalizeElement(str: String): String {
        return when (str.uppercase()) {
            "C" -> "C"
            "N" -> "N"
            "O" -> "O"
            "S" -> "S"
            "P" -> "P"
            "F" -> "F"
            "CL" -> "Cl"
            "BR" -> "Br"
            "I" -> "I"
            "SI" -> "Si"
            "SE" -> "Se"
            "B" -> "B"
            else -> str.take(1).uppercase()
        }
    }
}
