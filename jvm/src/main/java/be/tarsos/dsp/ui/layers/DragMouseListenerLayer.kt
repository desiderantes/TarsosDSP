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
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

class DragMouseListenerLayer(cs: CoordinateSystem) : MouseAdapter(), Layer {
    private val onlyHorizontal: Boolean = cs.isWrapping
    private val cs: CoordinateSystem
    private var previousPoint: Point?
    override fun draw(graphics: Graphics2D) {
        //do nothing, only capture mouse events
    }

    override val name: String
        get() = "Listen to drag events."

    override fun mousePressed(e: MouseEvent) {
        previousPoint = e.point
    }

    override fun mouseReleased(e: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            previousPoint = null
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(e) && previousPoint != null) {
            if (onlyHorizontal) {
                dragHorizontally(e)
            } else {
                dragBoth(e)
            }
        }
    }

    private fun dragBoth(e: MouseEvent) {
        val panel = e.component as LinkedPanel
        val graphics = panel.graphics as Graphics2D
        graphics.transform = panel.transform
        val unitsCurrent = pixelsToUnits(
            graphics,
            e.x, e.y
        )
        val unitsPrevious = pixelsToUnits(
            graphics,
            previousPoint!!.getX().toInt(),
            previousPoint!!.getY().toInt()
        )
        val millisecondAmount =
            (unitsPrevious!!.x - unitsCurrent!!.x).toFloat()
        val centAmount = (unitsPrevious.y - unitsCurrent.y).toFloat()
        previousPoint = e.point
        panel.viewPort.drag(millisecondAmount, centAmount)
        graphics.dispose()
    }

    private fun dragHorizontally(e: MouseEvent) {
        val panel = e.component as LinkedPanel
        val graphics = panel.graphics as Graphics2D
        graphics.transform = panel.transform
        val unitsCurrent =
            pixelsToUnits(graphics, e.x, previousPoint!!.getY().toInt())
        val unitsPrevious = pixelsToUnits(
            graphics,
            previousPoint!!.getX().toInt(),
            previousPoint!!.getY().toInt()
        )
        val millisecondAmount =
            (unitsPrevious!!.x - unitsCurrent!!.x).toFloat()
        previousPoint = e.point
        if (cs.isWrapping) {
            cs.wrappingOrigin = cs.wrappingOrigin + millisecondAmount
            panel.viewPort.drag(0f, 0f)
        } else {
            panel.viewPort.drag(millisecondAmount, 0f)
        }
        graphics.dispose()
    }

    init {
        previousPoint = null
        this.cs = cs
    }
}