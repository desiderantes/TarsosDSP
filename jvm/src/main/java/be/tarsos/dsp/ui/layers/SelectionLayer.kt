/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -------------------------------------------------------------
 *
 * TarsosDSP is developed by Joren Six at IPEM, University Ghent
 *
 * -------------------------------------------------------------
 *
 *  Info: http://0110.be/tag/TarsosDSP
 *  Github: https://github.com/JorenSix/TarsosDSP
 *  Releases: http://0110.be/releases/TarsosDSP/
 *
 *  TarsosDSP includes modified source code by various authors,
 *  for credits and info, see README.
 *
 */
package be.tarsos.dsp.ui.layers

import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.LinkedPanel
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import java.awt.Color
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

/**
 * Draws the current selection.
 */
class SelectionLayer @JvmOverloads constructor(
    private val cs: CoordinateSystem,
    private val color: Color = Color.ORANGE
) : MouseAdapter(), Layer {
    override fun draw(graphics: Graphics2D) {
        var startX: Double
        var startY: Double
        var endX: Double
        var endY: Double
        startX = cs.startX
        startY = cs.startY
        endX = cs.endX
        endY = cs.endY
        if (startX != Double.MAX_VALUE) {
            if (startX > endX) {
                val temp = startX
                startX = endX
                endX = temp
            }
            if (startY > endY) {
                val temp = startY
                startY = endY
                endY = temp
            }
            val x = startX.roundToInt()
            val y = startY.roundToInt()
            val width = endX.roundToInt() - x
            val height = endY.roundToInt() - y
            graphics.color = color
            graphics.drawRect(x, y, width, height)
        }
    }

    override val name: String
        get() = "Selection Layer"

    override fun mouseDragged(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            val panel = e.component as LinkedPanel
            val graphics = panel.graphics as Graphics2D
            graphics.transform = panel.transform
            val units = pixelsToUnits(graphics, e.x, e.y)
            if (!panel.coordinateSystem.hasStartPoint()) {
                panel.coordinateSystem.setStartPoint(units!!.x, units.y)
            } else {
                panel.coordinateSystem.setEndPoint(units!!.x, units.y)
            }
            panel.repaint()
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            val panel = e.component as LinkedPanel
            panel.viewPort.zoomToSelection()
            panel.invalidate()
        }
    }

}