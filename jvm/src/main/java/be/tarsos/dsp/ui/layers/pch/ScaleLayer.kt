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
package be.tarsos.dsp.ui.layers.pch

import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.LinkedPanel
import be.tarsos.dsp.ui.layers.Layer
import be.tarsos.dsp.ui.layers.LayerUtilities.drawString
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import java.awt.Color
import java.awt.Graphics2D
import java.awt.event.*
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class ScaleLayer(private val cs: CoordinateSystem, enableEditor: Boolean) : MouseAdapter(),
    Layer, MouseMotionListener, KeyListener {
    private val enableEditor: Boolean
    private var movingElement = -1.0
    var scale: DoubleArray
    override val name: String
        get() = "Scale Editor Layer"

    override fun draw(graphics: Graphics2D) {
        //draw legend
        graphics.color = Color.black
        val minY = cs.getMin(Axis.Y).roundToInt()
        val maxY = cs.getMax(Axis.Y).roundToInt()
        val maxX = cs.getMax(Axis.X).roundToInt()

        //int markerheightOffset = Math.round(LayerUtilities.pixelsToUnits(graphics, 15, false));
        var textOffset = pixelsToUnits(graphics, 20, false).roundToInt()
        var i = cs.getMin(Axis.X).toInt()
        while (i < cs.getMax(Axis.X)) {
            val realValue = cs.getRealXValue(i.toFloat()).toInt()
            for (scaleEntry in scale) {
                if (realValue == scaleEntry.toInt()) {
                    if (scaleEntry == movingElement) {
                        graphics.color = Color.RED
                    } else {
                        if (enableEditor) {
                            graphics.color = Color.GRAY
                        } else {
                            graphics.color = Color.LIGHT_GRAY
                        }
                    }
                    graphics.drawLine(
                        i,
                        minY + (1.5 * textOffset).toInt(),
                        i,
                        maxY - (1.5 * textOffset).toInt()
                    )
                    val text = realValue.toString()
                    if (enableEditor) {
                        drawString(
                            graphics,
                            text,
                            i.toDouble(),
                            minY + textOffset.toDouble(),
                            centerHorizontal = true,
                            centerVertical = false,
                            backgroundColor = null
                        )
                    } else {
                        drawString(
                            graphics,
                            text,
                            i.toDouble(),
                            maxY - textOffset.toDouble(),
                            centerHorizontal = true,
                            centerVertical = false,
                            backgroundColor = null
                        )
                    }
                }
            }
            i++
        }
        val axisLabelOffset =
            pixelsToUnits(graphics, 60, true).roundToInt()
        textOffset = pixelsToUnits(graphics, 10, false).roundToInt()
        drawString(
            graphics,
            "Frequency (cents)",
            maxX - axisLabelOffset.toDouble(),
            maxY - textOffset.toDouble(),
            centerHorizontal = true,
            centerVertical = true,
            backgroundColor = Color.white
        )
    }

    override fun mouseReleased(e: MouseEvent) {
        if (!enableEditor) {
            return
        }
        if (movingElement != -1.0) {
            Arrays.sort(scale)
        }
        movingElement = -1.0
        e.component.repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
        if (!enableEditor) {
            return
        }
        e.component.requestFocus()
        if (e.isAltDown || e.isAltGraphDown) {
            //request focus for the key listener to work...
            e.component.requestFocus()
            // add new element
            if (movingElement != -1.0) {
                var index = -1
                for (i in scale.indices) {
                    if (scale[i] == movingElement) {
                        index = i
                    }
                }
                if (index == -1) {
                    movingElement = -1.0
                } else {
                    scale[index] = getCents(e)
                    movingElement = scale[index]
                }
                e.component.repaint()
            } else {
                val newScale = DoubleArray(scale.size + 1)
                for (i in scale.indices) {
                    newScale[i] = scale[i]
                }
                newScale[newScale.size - 1] = getCents(e)
                movingElement = newScale[newScale.size - 1]
                Arrays.sort(newScale)
                scale = newScale
                e.component.repaint()
            }
        } else if (e.isControlDown && scale.isNotEmpty()) {
            //request focus for the key listener to work...

            // move the closest element
            if (movingElement == -1.0) {
                val index = closestIndex(getCents(e))
                movingElement = scale[index]
            }
            for (i in scale.indices) {
                if (scale[i] == movingElement) {
                    scale[i] = getCents(e)
                    movingElement = scale[i]
                }
            }
            e.component.repaint()
        }
    }

    private fun getCents(e: MouseEvent): Double {
        val panel = e.component as LinkedPanel
        val graphics = panel.graphics as Graphics2D
        graphics.transform = panel.transform
        val unitsCurrent = pixelsToUnits(graphics, e.x, e.y)
        return cs.getRealXValue(unitsCurrent!!.x.toFloat()).toDouble()
    }

    private fun closestIndex(key: Double): Int {
        var distance = Double.MAX_VALUE
        var index = -1
        for (i in scale.indices) {
            val currentDistance = abs(key - scale[i])
            val wrappedDistance = abs(key - (scale[i] + 1200))
            if (min(currentDistance, wrappedDistance) < distance) {
                distance = min(currentDistance, wrappedDistance)
                index = i
            }
        }
        return index
    }

    override fun keyTyped(e: KeyEvent) {
        if (!enableEditor) {
            return
        }
        val elementSelected = movingElement != -1.0
        val deleteKeyPressed =
            e.keyChar == 'd' || e.keyCode == KeyEvent.VK_DELETE || e.keyChar
                .toInt() == 127
        if (elementSelected && deleteKeyPressed) {
            val newScale = DoubleArray(scale.size - 1)
            var j = 0
            for (i in scale.indices) {
                if (scale[i] != movingElement) {
                    newScale[j] = scale[i]
                    j++
                }
            }
            Arrays.sort(newScale)
            scale = newScale
            movingElement = -1.0
            e.component.repaint()
        }
    }

    override fun keyPressed(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}

    init {
        val scale = doubleArrayOf(0.0, 100.0, 200.0, 400.0, 1000.0, 1100.0)
        this.scale = scale
        this.enableEditor = enableEditor
    }
}