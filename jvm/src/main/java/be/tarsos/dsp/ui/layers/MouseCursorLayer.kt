package be.tarsos.dsp.ui.layers

import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import java.awt.Color
import java.awt.Component
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class MouseCursorLayer(var cs: CoordinateSystem) : Layer, MouseMotionListener,
    MouseListener {
    private val pcs = PropertyChangeSupport(this)
    var onlyDrawVertical = false
    private var drawCursor = false
    private var lastPoint: Point? = null
    private var component: Component? = null
    override fun draw(graphics: Graphics2D) {
        if (drawCursor) {
            val unitPoint = pixelsToUnits(
                graphics,
                lastPoint!!.getX().toInt(),
                lastPoint!!.getY().toInt()
            )
            graphics.color = Color.blue
            if (!onlyDrawVertical) {
                graphics.drawLine(
                    cs.getMax(Axis.X).roundToInt(),
                    unitPoint!!.y.toFloat().roundToInt(),
                    cs.getMin(Axis.X).roundToInt(),
                    unitPoint.y.toFloat().roundToInt()
                )
                //notify listeners of change
                pcs.firePropertyChange("cursor", null, lastPoint)
            }
            graphics.drawLine(
                unitPoint!!.x.toFloat().roundToInt(),
                floor(cs.getMin(Axis.Y).toDouble()).toInt(),
                unitPoint.x.toFloat().roundToInt(),
                ceil(cs.getMax(Axis.Y).toDouble()).toInt()
            )
        }
    }

    override val name: String
        get() = "Cursor Layer"

    override fun mouseDragged(e: MouseEvent) {}
    override fun mouseMoved(e: MouseEvent) {
        lastPoint = e.point
        component = e.component
        component?.repaint()
    }

    override fun mouseClicked(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {
        drawCursor = false
        component = e.component
        component?.repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
        lastPoint = e.point
        drawCursor = true
    }

    override fun mouseEntered(e: MouseEvent) {
        lastPoint = e.point
        drawCursor = true
        onlyDrawVertical = false
    }

    override fun mouseExited(e: MouseEvent) {
        drawCursor = false
        component = e.component
        component?.repaint()
    }

    fun addPropertyChangeListener(listener: PropertyChangeListener?) {
        pcs.addPropertyChangeListener(listener)
    }

    fun removePropertyChangeListener(listener: PropertyChangeListener?) {
        pcs.removePropertyChangeListener(listener)
    }

    fun setPoint(newPosition: Point?) {
        drawCursor = true
        onlyDrawVertical = true
        lastPoint = newPosition
        component?.repaint()
    }

}