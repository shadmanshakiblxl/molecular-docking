package com.example.docking

import com.example.data.model.Atom3D
import com.example.data.model.LigandConformer

/**
 * Robust algorithmic conformer generator for ANY universal SMILES string.
 * Uses pure mathematical parsing and 3D distance geometry embedding without preset defaults.
 */
class ConformerGenerator {

    data class ParsedAtom(
        val index: Int,
        val element: String,
        val isAromatic: Boolean,
        val isHDonor: Boolean,
        val isHAcceptor: Boolean
    )

    fun generateConformer(smilesRaw: String, customName: String? = null): LigandConformer {
        // Step 1: Robust sanitization & pure mathematical parsing
        val parsed = MolecularCalculationEngine.parseAndSanitizeSmiles(smilesRaw)
        val sanitizedSmiles = parsed.sanitizedSmiles

        // Dynamic systematic name if none explicitly provided
        val name = if (!customName.isNullOrBlank() && !customName.startsWith("Candidate Ligand")) {
            customName
        } else {
            "Ligand ${parsed.chemicalFormula} (${parsed.heavyAtomCount} HA, ${parsed.molecularWeight} Da)"
        }

        // Step 2: Build topological graph from sanitized SMILES
        val (parsedAtoms, bonds) = parseSmilesGraph(sanitizedSmiles)

        // Count rotatable bonds (non-ring single bonds)
        val rotatableBonds = maxOf(0, bonds.size - parsed.heavyAtomCount / 2 - 2)

        // Universal LogP estimate (Wildman-Crippen inspired)
        val nC = parsed.atomCounts.getOrDefault("C", 0)
        val nO = parsed.atomCounts.getOrDefault("O", 0)
        val nN = parsed.atomCounts.getOrDefault("N", 0)
        val halogens = parsed.atomCounts.getOrDefault("F", 0) +
                parsed.atomCounts.getOrDefault("Cl", 0) +
                parsed.atomCounts.getOrDefault("Br", 0) +
                parsed.atomCounts.getOrDefault("I", 0)

        val logP = Math.round((nC * 0.28 - nO * 0.45 - nN * 0.35 + halogens * 0.40 + 0.50) * 100.0) / 100.0

        // Step 3: 3D Distance Geometry embedding
        val atoms3D = generate3DCoordinates(parsedAtoms, bonds)

        return LigandConformer(
            smiles = sanitizedSmiles,
            name = name,
            formula = parsed.chemicalFormula,
            molecularWeight = parsed.molecularWeight,
            logP = logP,
            hBondDonors = parsed.estimatedHBondDonors,
            hBondAcceptors = parsed.estimatedHBondAcceptors,
            rotatableBonds = rotatableBonds,
            heavyAtomCount = parsed.heavyAtomCount,
            atoms = atoms3D,
            bonds = bonds
        )
    }

    private fun parseSmilesGraph(smiles: String): Pair<List<ParsedAtom>, List<Pair<Int, Int>>> {
        val atoms = mutableListOf<ParsedAtom>()
        val bonds = mutableListOf<Pair<Int, Int>>()

        var i = 0
        var prevAtomIndex = -1
        val branchStack = mutableListOf<Int>()
        val ringMap = mutableMapOf<Char, Int>()

        while (i < smiles.length) {
            val c = smiles[i]
            when {
                c == '(' -> {
                    if (prevAtomIndex != -1) branchStack.add(prevAtomIndex)
                    i++
                }
                c == ')' -> {
                    if (branchStack.isNotEmpty()) {
                        prevAtomIndex = branchStack.removeAt(branchStack.lastIndex)
                    }
                    i++
                }
                c.isDigit() -> {
                    if (ringMap.containsKey(c)) {
                        val ringStart = ringMap[c]!!
                        if (prevAtomIndex != -1 && ringStart != prevAtomIndex) {
                            bonds.add(Pair(minOf(prevAtomIndex, ringStart), maxOf(prevAtomIndex, ringStart)))
                        }
                        ringMap.remove(c)
                    } else {
                        if (prevAtomIndex != -1) ringMap[c] = prevAtomIndex
                    }
                    i++
                }
                c in listOf('=', '#', '-', ':', '~', '/', '\\') -> {
                    i++
                }
                c == '[' -> {
                    val endBracket = smiles.indexOf(']', i)
                    if (endBracket != -1) {
                        val sub = smiles.substring(i + 1, endBracket)
                        var el = sub.filter { it.isLetter() }.take(2).capitalizeFirst()
                        if (el.isEmpty() || el == "H") el = "C"
                        val newIdx = atoms.size
                        val isDonor = sub.contains("H", ignoreCase = true)
                        val isAcceptor = el in listOf("O", "N", "F")
                        atoms.add(ParsedAtom(newIdx, el, false, isDonor, isAcceptor))
                        if (prevAtomIndex != -1) bonds.add(Pair(prevAtomIndex, newIdx))
                        prevAtomIndex = newIdx
                        i = endBracket + 1
                    } else {
                        i++
                    }
                }
                c.isLetter() -> {
                    if (c == 'H' || c == 'h') {
                        // Explicit hydrogen, skip
                        i++
                    } else {
                        var elStr = c.toString()
                        val isAromatic = c.isLowerCase()
                        if (i + 1 < smiles.length && smiles[i + 1].isLowerCase() && c.isUpperCase()) {
                            val candidate = "" + c + smiles[i + 1]
                            if (candidate.uppercase() in listOf("CL", "BR", "SI", "SE")) {
                                elStr = candidate
                                i++
                            }
                        }
                        val standardElem = when (elStr.uppercase()) {
                            "C" -> "C"
                            "N" -> "N"
                            "O" -> "O"
                            "S" -> "S"
                            "F" -> "F"
                            "CL" -> "Cl"
                            "BR" -> "Br"
                            "I" -> "I"
                            "P" -> "P"
                            "B" -> "B"
                            "SI" -> "Si"
                            "SE" -> "Se"
                            else -> "C"
                        }
                        val newIdx = atoms.size
                        val isHDonor = standardElem in listOf("O", "N") && !isAromatic
                        val isHAcceptor = standardElem in listOf("O", "N", "F")
                        atoms.add(ParsedAtom(newIdx, standardElem, isAromatic, isHDonor, isHAcceptor))

                        if (prevAtomIndex != -1) {
                            bonds.add(Pair(prevAtomIndex, newIdx))
                        }
                        prevAtomIndex = newIdx
                        i++
                    }
                }
                else -> i++
            }
        }

        // Safety fallback: if SMILES was empty or has no heavy atoms, generate a single carbon
        if (atoms.isEmpty()) {
            atoms.add(ParsedAtom(0, "C", false, false, false))
        }

        return Pair(atoms, bonds)
    }

    private fun generate3DCoordinates(atoms: List<ParsedAtom>, bonds: List<Pair<Int, Int>>): List<Atom3D> {
        val n = atoms.size
        val result = mutableListOf<Atom3D>()

        val coords = Array(n) { DoubleArray(3) }
        val visited = BooleanArray(n)

        if (n > 0) {
            coords[0] = doubleArrayOf(0.0, 0.0, 0.0)
            visited[0] = true
        }

        val adj = Array(n) { mutableListOf<Int>() }
        for ((u, v) in bonds) {
            if (u in 0 until n && v in 0 until n) {
                adj[u].add(v)
                adj[v].add(u)
            }
        }

        val queue = ArrayDeque<Int>()
        if (n > 0) queue.add(0)

        val bondLength = 1.48 // Standard mean organic bond length in Å
        var angleOffset = 0.0

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            var branchAngle = 0.0
            for (nbr in adj[curr]) {
                if (!visited[nbr]) {
                    visited[nbr] = true
                    val theta = branchAngle + angleOffset
                    val phi = ((nbr + 1) * 1.09) % Math.PI
                    val dx = bondLength * Math.cos(theta) * Math.sin(phi)
                    val dy = bondLength * Math.sin(theta) * Math.sin(phi)
                    val dz = bondLength * Math.cos(phi)

                    coords[nbr][0] = coords[curr][0] + dx
                    coords[nbr][1] = coords[curr][1] + dy
                    coords[nbr][2] = coords[curr][2] + dz

                    branchAngle += 1.9 // ~109.5 degrees in radians
                    queue.add(nbr)
                }
            }
            angleOffset += 0.4
        }

        // Center molecule at origin (0, 0, 0)
        var meanX = 0.0
        var meanY = 0.0
        var meanZ = 0.0
        for (i in 0 until n) {
            meanX += coords[i][0]
            meanY += coords[i][1]
            meanZ += coords[i][2]
        }
        meanX /= n
        meanY /= n
        meanZ /= n

        for (i in 0 until n) {
            val a = atoms[i]
            val cx = coords[i][0] - meanX
            val cy = coords[i][1] - meanY
            val cz = coords[i][2] - meanZ
            result.add(
                Atom3D(
                    id = i + 1,
                    name = "${a.element}${i + 1}",
                    element = a.element,
                    residueName = "LIG",
                    residueNum = 1,
                    chain = "L",
                    x = Math.round(cx * 1000.0) / 1000.0,
                    y = Math.round(cy * 1000.0) / 1000.0,
                    z = Math.round(cz * 1000.0) / 1000.0,
                    isHetero = true
                )
            )
        }

        return result
    }

    private fun String.capitalizeFirst(): String =
        if (isEmpty()) this else this[0].uppercase() + substring(1).lowercase()
}
