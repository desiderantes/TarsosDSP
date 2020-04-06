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
package be.tarsos.dsp.ui

import java.awt.Point
import java.util.*

class ViewPort(cs: CoordinateSystem) {
    private val listeners: MutableList<ViewPortChangedListener>
    private val cs: CoordinateSystem
    private var xMinPref = Int.MAX_VALUE
    private var xMaxPref = Int.MAX_VALUE
    private var yMinPref = Int.MAX_VALUE
    private var yMaxPref = Int.MAX_VALUE
    var onlyZoomXAxisWithMouseWheel = false
    fun addViewPortChangedListener(listener: ViewPortChangedListener) {
        listeners.add(listener)
    }

    private fun viewPortChanged() {
        for (listener in listeners) {
            listener.viewPortChanged(this)
        }
    }

    fun setPreferredZoomWindow(xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
        xMinPref = xMin
        xMaxPref = xMax
        yMinPref = yMin
        yMaxPref = yMax
    }

    fun zoom(amount: Int, zoomPoint: Point?) {
        //time value
        val xDelta = cs.getDelta(Axis.X)
        val newXDelta = xDelta + amount * 1000
        if (newXDelta > 2 && newXDelta < 600000) {
            cs.setMax(Axis.X, cs.getMin(Axis.X) + newXDelta)
        }

        //cents value
        if (cs.getUnitsForAxis(Axis.Y) === AxisUnit.FREQUENCY && !onlyZoomXAxisWithMouseWheel) {
            val yDelta = cs.getDelta(Axis.Y)
            val newYDelta = yDelta + amount * 10
            if (newYDelta > 50 && newXDelta < 150000) {
                cs.setMax(Axis.Y, cs.getMin(Axis.Y) + newYDelta)
            }
        }
        viewPortChanged()
    }

    fun resetZoom() {
        if (xMinPref != Int.MAX_VALUE) {
            cs.setMin(Axis.X, xMinPref.toFloat())
            cs.setMax(Axis.X, xMaxPref.toFloat())
        }
        if (yMinPref != Int.MAX_VALUE) {
            cs.setMin(Axis.Y, yMinPref.toFloat())
            cs.setMax(Axis.Y, yMaxPref.toFloat())
        }
        if (xMinPref == Int.MAX_VALUE && yMinPref == Int.MAX_VALUE) {
            if (cs.getUnitsForAxis(Axis.Y) === AxisUnit.FREQUENCY) {
                cs.setMin(Axis.Y, 3600f)
                cs.setMax(Axis.Y, 12800f)
            }
            cs.setMin(Axis.X, 0f)
            cs.setMax(Axis.X, 30000f)
        }
        viewPortChanged()
    }

    fun zoomToSelection() {
        if (!cs.hasStartPoint() || cs.endX == Double.MAX_VALUE) {
            cs.clearPoints()
            return
        }
        var startX = cs.startX
        var startY = cs.startY
        var endX = cs.endX
        var endY = cs.endY
        cs.clearPoints()
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

        //do not zoom smaller than a certain threshold
        val minTimeDiff = 10 //ms
        val minCentsDiff = 50 //cents
        if (endX - startX <= minTimeDiff) {
            endX = startX + minTimeDiff
        }
        if (endY - startY <= minCentsDiff) {
            endY = startY + minCentsDiff
        }
        cs.setMin(Axis.X, startX.toFloat())
        cs.setMax(Axis.X, endX.toFloat())
        if (cs.getUnitsForAxis(Axis.Y) === AxisUnit.FREQUENCY) {
            cs.setMin(Axis.Y, startY.toFloat())
            cs.setMax(Axis.Y, endY.toFloat())
        }
        viewPortChanged()
    }

    fun drag(xAmount: Float, yAmount: Float) {
        cs.setMin(Axis.X, cs.getMin(Axis.X) + xAmount)
        cs.setMax(Axis.X, cs.getMax(Axis.X) + xAmount)
        cs.setMin(Axis.Y, cs.getMin(Axis.Y) + yAmount)
        cs.setMax(Axis.Y, cs.getMax(Axis.Y) + yAmount)
        viewPortChanged()
    }

    interface ViewPortChangedListener {
        fun viewPortChanged(newViewPort: ViewPort?)
    }

    init {
        listeners = ArrayList<ViewPortChangedListener>()
        this.cs = cs
    }
}