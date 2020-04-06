package be.tarsos.dsp.ui.layers

import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.LayerUtilities.drawString
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import java.awt.Color
import java.awt.Component
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.geom.Point2D
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class TooltipLayer @JvmOverloads constructor(
    private val cs: CoordinateSystem,
    private val tooltipTextGenerator: TooltipTextGenerator = defaultTooltipGenerator
) : Layer, MouseMotionListener, MouseListener {
    private var enableTooltip = false
    private val millisecondsBeforeAppearance = 1000
    private var mouseStoppedAtMilliseconds = System.currentTimeMillis()
    private var lastPoint: Point? = null
    private var lastDrawnPoint: Point? = null
    private var component: Component? = null
    override fun draw(graphics: Graphics2D) {
        val diff = System.currentTimeMillis() - mouseStoppedAtMilliseconds
        if (enableTooltip && diff > millisecondsBeforeAppearance) {
            val unitPoint = pixelsToUnits(
                graphics,
                lastPoint!!.getX().toInt(),
                lastPoint!!.getY().toInt()
            )
            val textYOffset =
                pixelsToUnits(graphics, 10, false).roundToInt()
            val textXOffset =
                pixelsToUnits(graphics, 10, true).roundToInt()
            val text = tooltipTextGenerator.generateTooltip(cs, unitPoint!!)
            drawString(
                graphics,
                text,
                unitPoint.x + textXOffset,
                unitPoint.y + textYOffset,
                centerHorizontal = false,
                centerVertical = true,
                backgroundColor = Color.white,
                textColor = Color.black
            )
            lastDrawnPoint = lastPoint
        }
    }

    override val name: String
        get() = "Tooltip Layer"

    override fun mouseDragged(e: MouseEvent) {
        enableTooltip = false
    }

    override fun mouseMoved(e: MouseEvent) {
        lastPoint = e.point
        component = e.component
        mouseStoppedAtMilliseconds = System.currentTimeMillis()
    }

    override fun mouseClicked(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {
        enableTooltip = false
    }

    override fun mouseReleased(e: MouseEvent) {
        enableTooltip = true
    }

    override fun mouseEntered(e: MouseEvent) {
        lastPoint = e.point
        component = e.component
        enableTooltip = true
    }

    override fun mouseExited(e: MouseEvent) {
        enableTooltip = false
    }

    interface TooltipTextGenerator {
        fun generateTooltip(cs: CoordinateSystem, point: Point2D): String
    }

    companion object {
        private val defaultTooltipGenerator: TooltipTextGenerator = object : TooltipTextGenerator {
            override fun generateTooltip(
                cs: CoordinateSystem,
                point: Point2D
            ): String {
                return String.format(
                    "[%.03f%s , %.02f%s]",
                    point.x / 1000.0,
                    cs.getUnitsForAxis(Axis.X)!!.unit,
                    point.y,
                    cs.getUnitsForAxis(Axis.Y)!!.unit
                )
            }
        }
    }

    init {
        thread(start = true, name = "Tooltip Repaint Check") {
            while (true) {
                Thread.sleep(30)
                val diff =
                    System.currentTimeMillis() - mouseStoppedAtMilliseconds
                if (component != null && diff > millisecondsBeforeAppearance && lastDrawnPoint !== lastPoint) {
                    component!!.repaint()
                }
            }
        }
    }
}