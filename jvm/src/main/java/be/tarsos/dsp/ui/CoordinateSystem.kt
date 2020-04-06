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


class CoordinateSystem @JvmOverloads constructor(
    private val xAxisUnits: AxisUnit = AxisUnit.TIME,
    private val yAxisUnits: AxisUnit?,
    private var yMin: Float,
    private var yMax: Float,
    val isWrapping: Boolean = false
) {
    private var xMin = 0f
    private var xMax = 10000f
    var wrappingOrigin = 0.0f

    //For selection Layer
    var startX = Double.MAX_VALUE
        private set
    var startY = Double.MAX_VALUE
        private set
    var endX = Double.MAX_VALUE
        private set
    var endY = Double.MAX_VALUE
        private set

    fun getRealXValue(value: Float): Float {
        return if (isWrapping) {
            (1000 * getDelta(Axis.X) + value + wrappingOrigin) % getDelta(
                Axis.X
            )
        } else {
            value
        }
    }

    fun getDelta(axis: Axis): Float {
        return if (axis === Axis.X) {
            xMax - xMin
        } else {
            yMax - yMin
        }
    }

    fun getUnitsForAxis(axis: Axis): AxisUnit? {
        return if (axis === Axis.X) {
            xAxisUnits
        } else {
            yAxisUnits
        }
    }

    fun getMin(axis: Axis): Float {
        return if (axis === Axis.X) {
            xMin
        } else {
            yMin
        }
    }

    fun getMax(axis: Axis): Float {
        return if (axis === Axis.X) {
            xMax
        } else {
            yMax
        }
    }

    fun setMax(axis: Axis, value: Float) {
        if (axis === Axis.X) {
            xMax = value
        } else {
            yMax = value
        }
    }

    fun setMin(axis: Axis, value: Float) {
        if (axis === Axis.X) {
            xMin = value
        } else {
            yMin = value
        }
    }

    fun setStartPoint(x: Double, y: Double) {
        startX = x
        startY = y
    }

    fun hasStartPoint(): Boolean {
        return startX != Double.MAX_VALUE
    }

    fun setEndPoint(x: Double, y: Double) {
        endX = x
        endY = y
    }

    fun clearPoints() {
        startX = Double.MAX_VALUE
        startY = Double.MAX_VALUE
        endX = Double.MAX_VALUE
        endY = Double.MAX_VALUE
    }

}