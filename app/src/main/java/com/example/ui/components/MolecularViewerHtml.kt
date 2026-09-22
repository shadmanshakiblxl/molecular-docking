package com.example.ui.components

import com.example.data.model.DockingRunResult
import org.json.JSONArray
import org.json.JSONObject

object MolecularViewerHtml {

    fun generateHtml(
        result: DockingRunResult?,
        mode: Int = 1, // 1: Surface, 2: Pocket, 3: Interaction Map, 4: Split Comparison
        isDark: Boolean = false
    ): String {
        val bgColor = if (isDark) "#070C18" else "#F9FAFB"
        val surfaceColor = if (isDark) "#0D1527" else "#FFFFFF"
        val textColor = if (isDark) "#F8FAFC" else "#111827"
        val mutedTextColor = if (isDark) "#94A3B8" else "#6B7280"
        val borderColor = if (isDark) "#1E2E4A" else "#E5E7EB"
        val accentColor = if (isDark) "#00E5FF" else "#088F8F"

        if (result == null) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <style>
                        body {
                            margin: 0; padding: 0; width: 100vw; height: 100vh;
                            background: $bgColor; color: $textColor;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            display: flex; flex-direction: column; align-items: center; justify-content: center;
                            text-align: center; box-sizing: border-box; padding: 24px;
                        }
                        .icon { width: 64px; height: 64px; margin-bottom: 16px; opacity: 0.7; }
                        h3 { margin: 0 0 8px 0; font-weight: 500; font-size: 1.1rem; letter-spacing: 0.5px; }
                        p { margin: 0; font-size: 0.85rem; color: $mutedTextColor; line-height: 1.4; max-width: 280px; }
                    </style>
                </head>
                <body>
                    <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="$accentColor" stroke-width="1.5">
                        <circle cx="12" cy="12" r="9"/>
                        <path d="M12 3v18M3 12h18"/>
                        <circle cx="12" cy="7" r="2" fill="$accentColor"/>
                        <circle cx="16" cy="15" r="2" fill="$accentColor"/>
                        <circle cx="8" cy="15" r="2" fill="$accentColor"/>
                    </svg>
                    <h3>3D Molecular Stage Ready</h3>
                    <p>Select a Target Protein (PDB ID) and Ligand (SMILES) to render the 3D interaction viewer.</p>
                </body>
                </html>
            """.trimIndent()
        }

        val cleanedPdbEscaped = JSONObject.quote(result.protein.cleanedPdbContent)
        val ligandPdbEscaped = JSONObject.quote(result.bestPose.toPdbString(result.ligand.name))

        val hBondsJson = JSONArray()
        result.hydrogenBonds.forEach { hb ->
            val obj = JSONObject()
            obj.put("donor", hb.donorResidue + ":" + hb.donorAtom)
            obj.put("acceptor", hb.acceptorResidue + ":" + hb.acceptorAtom)
            obj.put("dist", hb.distanceAngstrom)
            hBondsJson.put(obj)
        }

        val pocketCenterJson = JSONObject()
        pocketCenterJson.put("x", result.pocketCenter.first)
        pocketCenterJson.put("y", result.pocketCenter.second)
        pocketCenterJson.put("z", result.pocketCenter.third)

        val pocketResiduesJson = JSONArray()
        result.residueDistances.forEach { rd ->
            pocketResiduesJson.put(rd.residueNumber)
        }

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>3Dmol.js Molecular Docking Stage</title>
    <!-- Load 3Dmol.js from CDN with fallback -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/3Dmol/2.0.4/3Dmol-min.js"></script>
    <style>
        * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
        html, body {
            margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden;
            background-color: $bgColor; color: $textColor;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            user-select: none;
        }
        #container {
            position: relative; width: 100%; height: 100%; display: flex;
        }
        .viewer-box {
            position: relative; width: 100%; height: 100%; overflow: hidden;
        }
        /* Mode 4: Split Comparison */
        #container.split-mode .viewer-box {
            width: 50%; height: 100%; border-right: 1px solid $borderColor;
        }
        .split-label {
            display: none; position: absolute; top: 12px; left: 14px; z-index: 10;
            background: ${if (isDark) "rgba(13,21,39,0.85)" else "rgba(255,255,255,0.88)"};
            border: 1px solid $borderColor; border-radius: 6px;
            padding: 4px 8px; font-size: 11px; font-weight: 600; letter-spacing: 0.5px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        #container.split-mode .split-label { display: block; }
        .split-sublabel {
            font-size: 9px; color: $mutedTextColor; font-weight: normal; margin-top: 2px;
        }
        /* Floating controls overlay */
        .hud-overlay {
            position: absolute; bottom: 12px; right: 12px; z-index: 20;
            display: flex; gap: 6px; pointer-events: auto;
        }
        .hud-btn {
            background: ${if (isDark) "rgba(13,21,39,0.9)" else "rgba(255,255,255,0.92)"};
            color: $textColor; border: 1px solid $borderColor;
            border-radius: 20px; padding: 6px 12px; font-size: 11px; font-weight: 600;
            display: flex; align-items: center; gap: 4px; cursor: pointer;
            box-shadow: 0 2px 6px rgba(0,0,0,0.12);
        }
        .hud-btn:active { background: $accentColor; color: #fff; }
        /* Error/Offline Fallback canvas */
        #fallbackCanvas {
            position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: none;
        }
    </style>
</head>
<body>
    <div id="container" class="${if (mode == 4) "split-mode" else ""}">
        <!-- Left / Main Viewer -->
        <div id="viewer1_box" class="viewer-box">
            <div class="split-label">
                <div>BEFORE DOCKING</div>
                <div class="split-sublabel">Input Target Receptor + Pocket</div>
            </div>
            <div id="viewer1" style="width:100%; height:100%; position:relative;"></div>
        </div>

        <!-- Right Viewer (Only active in Mode 4 Split Comparison) -->
        <div id="viewer2_box" class="viewer-box" style="display: ${if (mode == 4) "block" else "none"};">
            <div class="split-label">
                <div style="color: $accentColor;">AFTER DOCKING</div>
                <div class="split-sublabel">Optimal Pose (${result.bestPose.scoreKcalMol} kcal/mol)</div>
            </div>
            <div id="viewer2" style="width:100%; height:100%; position:relative;"></div>
        </div>
    </div>

    <!-- HUD Control Buttons -->
    <div class="hud-overlay">
        <button class="hud-btn" onclick="centerOnPocket()">
            <span>Focus Pocket</span>
        </button>
        <button class="hud-btn" onclick="toggleSpin()">
            <span id="spinBtnText">Spin</span>
        </button>
        <button class="hud-btn" onclick="resetCamera()">
            <span>Reset</span>
        </button>
    </div>

    <canvas id="fallbackCanvas"></canvas>

    <script>
        var currentMode = $mode;
        var proteinPdb = $cleanedPdbEscaped;
        var ligandPdb = $ligandPdbEscaped;
        var hBonds = $hBondsJson;
        var pocketCenter = $pocketCenterJson;
        var pocketResidues = $pocketResiduesJson;
        var isSpinning = false;
        var glViewer1 = null;
        var glViewer2 = null;

        function hasWebGL() {
            try {
                var testCanvas = document.createElement('canvas');
                return !!(window.WebGLRenderingContext && (testCanvas.getContext('webgl') || testCanvas.getContext('experimental-webgl')));
            } catch (e) {
                return false;
            }
        }

        function init3D() {
            if (!hasWebGL() || typeof $3Dmol === 'undefined') {
                showFallbackCanvas();
                return;
            }

            try {
                // Setup Viewer 1
                var v1Element = document.getElementById('viewer1');
                var config1 = { backgroundColor: '$bgColor' };
                glViewer1 = $3Dmol.createViewer(v1Element, config1);

                // Setup Viewer 2 if split mode
                if (currentMode === 4) {
                    var v2Element = document.getElementById('viewer2');
                    var config2 = { backgroundColor: '$bgColor' };
                    glViewer2 = $3Dmol.createViewer(v2Element, config2);
                }

                renderMode(currentMode);
            } catch (err) {
                console.error("3Dmol initialization error:", err);
                showFallbackCanvas();
            }
        }

        function renderMode(mode) {
            currentMode = mode;
            if (!glViewer1) return;

            glViewer1.clear();

            if (mode === 4) {
                // SPLIT VIEW
                document.getElementById('container').className = "split-mode";
                document.getElementById('viewer2_box').style.display = "block";

                // Viewer 1: BEFORE DOCKING (Input Protein + pocket cavity)
                glViewer1.addModel(proteinPdb, "pdb");
                glViewer1.setStyle({}, { cartoon: { color: 'spectrum', opacity: 0.9 } });
                // Highlight pocket residues with yellow sticks
                glViewer1.setStyle({ resi: pocketResidues }, {
                    cartoon: { color: 'spectrum' },
                    stick: { radius: 0.16, color: '#38BDF8' }
                });
                glViewer1.zoomTo({ resi: pocketResidues });
                glViewer1.render();

                // Viewer 2: AFTER DOCKING (Protein + Docked Ligand Pose)
                if (!glViewer2) {
                    var v2Element = document.getElementById('viewer2');
                    glViewer2 = $3Dmol.createViewer(v2Element, { backgroundColor: '$bgColor' });
                }
                glViewer2.clear();
                glViewer2.addModel(proteinPdb, "pdb");
                glViewer2.setStyle({}, { cartoon: { color: 'spectrum', opacity: 0.85 } });
                glViewer2.setStyle({ resi: pocketResidues }, {
                    cartoon: { color: 'spectrum' },
                    stick: { radius: 0.14, colorscheme: 'cyanCarbon' }
                });

                var ligModel2 = glViewer2.addModel(ligandPdb, "pdb");
                ligModel2.setStyle({}, {
                    stick: { radius: 0.28, colorscheme: 'greenCarbon' },
                    sphere: { scale: 0.32, colorscheme: 'greenCarbon' }
                });

                // Add H-bond cylinder representations
                addHydrogenBondsToViewer(glViewer2);

                glViewer2.zoomTo(ligModel2);
                glViewer2.render();

            } else {
                // SINGLE VIEW MODES (1, 2, 3)
                document.getElementById('container').className = "";
                document.getElementById('viewer2_box').style.display = "none";

                var protModel = glViewer1.addModel(proteinPdb, "pdb");
                var ligModel = glViewer1.addModel(ligandPdb, "pdb");

                if (mode === 1) {
                    // MODE 1: Protein Surface + Ligand Sticks
                    glViewer1.setStyle({}, { cartoon: { color: 'spectrum', opacity: 0.4 } });
                    glViewer1.addSurface($3Dmol.SurfaceType.VDW, {
                        opacity: 0.68,
                        color: '$surfaceColor'
                    }, { model: protModel });

                    ligModel.setStyle({}, {
                        stick: { radius: 0.35, colorscheme: 'cyanCarbon' }
                    });
                    glViewer1.zoomTo(ligModel);

                } else if (mode === 2) {
                    // MODE 2: Binding Pocket (Cartoon + Pocket residues sticks + Ligand ball-and-stick)
                    glViewer1.setStyle({}, { cartoon: { color: 'spectrum', opacity: 0.8 } });
                    glViewer1.setStyle({ resi: pocketResidues }, {
                        cartoon: { color: 'spectrum' },
                        stick: { radius: 0.2, colorscheme: 'blueCarbon' }
                    });

                    ligModel.setStyle({}, {
                        stick: { radius: 0.26, colorscheme: 'element' },
                        sphere: { scale: 0.34, colorscheme: 'element' }
                    });
                    glViewer1.zoomTo(ligModel);

                } else if (mode === 3) {
                    // MODE 3: Interaction Map (H-bonds, Hydrophobic, Residue distances)
                    glViewer1.setStyle({}, { cartoon: { color: 'gray', opacity: 0.5 } });
                    glViewer1.setStyle({ resi: pocketResidues }, {
                        cartoon: { color: '#38BDF8' },
                        stick: { radius: 0.22, colorscheme: 'cyanCarbon' }
                    });

                    ligModel.setStyle({}, {
                        stick: { radius: 0.32, colorscheme: 'greenCarbon' },
                        sphere: { scale: 0.3, colorscheme: 'greenCarbon' }
                    });

                    // Add hydrogen bond dashed lines
                    addHydrogenBondsToViewer(glViewer1);

                    // Add distance labels
                    if (hBonds && hBonds.length > 0) {
                        for (var i = 0; i < Math.min(3, hBonds.length); i++) {
                            var hb = hBonds[i];
                            glViewer1.addLabel(hb.dist + " Å", {
                                position: pocketCenter,
                                backgroundColor: '$surfaceColor',
                                backgroundOpacity: 0.8,
                                fontColor: '$accentColor',
                                fontSize: 11
                            });
                        }
                    }

                    glViewer1.zoomTo(ligModel);
                }

                glViewer1.render();
            }
        }

        function addHydrogenBondsToViewer(viewer) {
            if (!viewer || !pocketCenter) return;
            // Render dashed interactive cylinder links from pocket center
            for (var i = 0; i < Math.min(3, hBonds.length); i++) {
                var offset = (i - 1) * 1.5;
                viewer.addCylinder({
                    start: { x: pocketCenter.x, y: pocketCenter.y, z: pocketCenter.z },
                    end: { x: pocketCenter.x + offset, y: pocketCenter.y + 2.2, z: pocketCenter.z + offset },
                    radius: 0.08,
                    dashed: true,
                    dashLength: 0.25,
                    dashGap: 0.15,
                    color: '#FBBF24'
                });
            }
        }

        function centerOnPocket() {
            if (glViewer1) {
                glViewer1.zoomTo({ resi: pocketResidues });
            }
            if (glViewer2 && currentMode === 4) {
                glViewer2.zoomTo({ resi: pocketResidues });
            }
        }

        function resetCamera() {
            if (glViewer1) {
                glViewer1.setCameraParameters({ fov: 45 });
                glViewer1.zoomTo();
                glViewer1.render();
            }
            if (glViewer2 && currentMode === 4) {
                glViewer2.setCameraParameters({ fov: 45 });
                glViewer2.zoomTo();
                glViewer2.render();
            }
        }

        function toggleSpin() {
            isSpinning = !isSpinning;
            document.getElementById('spinBtnText').innerText = isSpinning ? "Pause" : "Spin";
            if (glViewer1) glViewer1.spin(isSpinning ? "y" : false);
            if (glViewer2 && currentMode === 4) glViewer2.spin(isSpinning ? "y" : false);
        }

        // Native interface callback from Android Compose
        function setViewerMode(modeNumber) {
            renderMode(modeNumber);
        }

        // Fallback Canvas if 3Dmol library is unreachable offline
        function showFallbackCanvas() {
            var canvas = document.getElementById('fallbackCanvas');
            canvas.style.display = 'block';
            var ctx = canvas.getContext('2d');
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;

            var cx = canvas.width / 2;
            var cy = canvas.height / 2;

            function drawFallback() {
                ctx.fillStyle = '$bgColor';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                // Draw technical circular pocket grid
                ctx.strokeStyle = '$borderColor';
                ctx.lineWidth = 1;
                ctx.beginPath();
                ctx.arc(cx, cy, 110, 0, Math.PI * 2);
                ctx.arc(cx, cy, 70, 0, Math.PI * 2);
                ctx.stroke();

                // Draw molecular bonds representation
                ctx.strokeStyle = '$accentColor';
                ctx.lineWidth = 3;
                ctx.beginPath();
                ctx.moveTo(cx - 50, cy - 30);
                ctx.lineTo(cx, cy - 60);
                ctx.lineTo(cx + 50, cy - 30);
                ctx.lineTo(cx + 50, cy + 30);
                ctx.lineTo(cx, cy + 60);
                ctx.lineTo(cx - 50, cy + 30);
                ctx.closePath();
                ctx.stroke();

                // Draw nodes
                var nodes = [
                    [cx - 50, cy - 30, '#10B981'],
                    [cx, cy - 60, '#38BDF8'],
                    [cx + 50, cy - 30, '#F59E0B'],
                    [cx + 50, cy + 30, '#EF4444'],
                    [cx, cy + 60, '#A855F7'],
                    [cx - 50, cy + 30, '#38BDF8']
                ];
                for (var i = 0; i < nodes.length; i++) {
                    ctx.fillStyle = nodes[i][2];
                    ctx.beginPath();
                    ctx.arc(nodes[i][0], nodes[i][1], 7, 0, Math.PI * 2);
                    ctx.fill();
                }

                // Text
                ctx.fillStyle = '$textColor';
                ctx.font = '14px sans-serif';
                ctx.textAlign = 'center';
                ctx.fillText("${result.protein.pdbId} : ${result.ligand.name.take(20)}", cx, cy - 85);
                ctx.fillStyle = '$accentColor';
                ctx.font = 'bold 16px sans-serif';
                ctx.fillText("${result.bestPose.scoreKcalMol} kcal/mol (Mode " + currentMode + ")", cx, cy + 105);
            }

            drawFallback();
        }

        window.onload = function() {
            setTimeout(init3D, 100);
        };
    </script>
</body>
</html>
        """.trimIndent()
    }
}
