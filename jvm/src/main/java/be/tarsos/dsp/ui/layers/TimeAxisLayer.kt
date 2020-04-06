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

import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.AxisUnit
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.LayerUtilities.drawString
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.roundToInt

class TimeAxisLayer(var cs: CoordinateSystem) : Layer {
    private val intervals = intArrayOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000)
    private var intervalIndex = 0
    override fun draw(graphics: Graphics2D) {
        if (cs.getUnitsForAxis(Axis.X) === AxisUnit.TIME) {
            // draw legend
            graphics.color = Color.black
            // every second
            val minY = cs.getMin(Axis.Y).roundToInt()
            val maxX = cs.getMax(Axis.X).roundToInt()

            //float deltaX = cs.getDelta(Axis.X); //Breedte in milisec.
            val beginDrawInterval = 1000
            intervalIndex = 0
            val smallDrawInterval = beginDrawInterval * intervals[intervalIndex]
            val markerHeight =
                pixelsToUnits(graphics, 9, false).roundToInt()
            var textOffset =
                pixelsToUnits(graphics, 12, false).roundToInt()
            val smallMarkerheight =
                pixelsToUnits(graphics, 4, false).roundToInt()
            val smallTextOffset =
                pixelsToUnits(graphics, 9, false).roundToInt()
            val smallestMarkerheight =
                pixelsToUnits(graphics, 2, false).roundToInt()
            val minValue = cs.getMin(Axis.X).toInt()
            val maxValue = cs.getMax(Axis.X).toInt()
            val differenceInMs = maxValue - minValue
            when {
                differenceInMs >= 240000 -> {
                    //only draw seconds
                    for (i in minValue until maxValue) {
                        if (i % (smallDrawInterval * 60) == 0) {
                            graphics.drawLine(i, minY, i, minY + markerHeight)
                            val text = (i / 1000).toString()
                            drawString(
                                graphics,
                                text,
                                i.toDouble(),
                                minY + textOffset.toDouble(),
                                centerHorizontal = true,
                                centerVertical = false,
                                backgroundColor = null
                            )
                        }
                    }
                }
                differenceInMs in 120000..239999 -> {
                    //only draw seconds
                    for (i in minValue until maxValue) {
                        if (i % (smallDrawInterval * 10) == 0) {
                            graphics.drawLine(i, minY, i, minY + markerHeight)
                            val text = (i / 1000).toString()
                            drawString(
                                graphics,
                                text,
                                i.toDouble(),
                                minY + textOffset.toDouble(),
                                centerHorizontal = true,
                                centerVertical = false,
                                backgroundColor = null
                            )
                        }
                    }
                }
                differenceInMs in 30000..119999 -> {
                    //only draw seconds
                    for (i in minValue until maxValue) {
                        if (i % (smallDrawInterval * 5) == 0) {
                            graphics.drawLine(i, minY, i, minY + markerHeight)
                            val text = (i / 1000).toString()
                            drawString(
                                graphics,
                                text,
                                i.toDouble(),
                                minY + textOffset.toDouble(),
                                centerHorizontal = true,
                                centerVertical = false,
                                backgroundColor = null
                            )
                        } else if (i % smallDrawInterval == 0) {
                            graphics.drawLine(i, minY, i, minY + smallMarkerheight)
                        }
                    }
                }
                differenceInMs in 10001..29999 -> {
                    //only draw seconds
                    for (i in minValue until maxValue) {
                        if (i % (smallDrawInterval * 5) == 0) {
                            graphics.drawLine(i, minY, i, minY + markerHeight)
                            val text = (i / 1000).toString()
                            drawString(
                                graphics,
                                text,
                                i.toDouble(),
                                minY + textOffset.toDouble(),
                                centerHorizontal = true,
                                centerVertical = false,
                                backgroundColor = null
                            )
                        } else if (i % smallDrawInterval == 0) {
                            graphics.drawLine(i, minY, i, minY + smallMarkerheight)
                            val text = (i / 1000).toString()
                            drawString(
                                graphics,
                                text,
                                i.toDouble(),
                                minY + smallTextOffset.toDouble(),
                                centerHorizontal = true,
                                centerVertical = false,
                                backgroundColor = null
                            )
                        }
                    }
                }
                else -> {
                    //also draw 0.1s
                    for (i in minValue until maxValue) {
                        when {
                            i % (smallDrawInterval * 5) == 0 -> {
                                graphics.drawLine(i, minY, i, minY + markerHeight)
                                val text = (i / 1000).toString()
                                drawString(
                                    graphics,
                                    text,
                                    i.toDouble(),
                                    minY + textOffset.toDouble(),
                                    centerHorizontal = true,
                                    centerVertical = false,
                                    backgroundColor = null
                                )
                            }
                            i % smallDrawInterval == 0 -> {
                                graphics.drawLine(i, minY, i, minY + smallMarkerheight)
                                val text = (i / 1000).toString()
                                drawString(
                                    graphics,
                                    text,
                                    i.toDouble(),
                                    minY + smallTextOffset.toDouble(),
                                    centerHorizontal = true,
                                    centerVertical = false,
                                    backgroundColor = null
                                )
                            }
                            i % 100 == 0 -> {
                                graphics.drawLine(i, minY, i, minY + smallestMarkerheight)
                            }
                        }
                    }
                }
            }
            val axisLabelOffset =
                pixelsToUnits(graphics, 26, true).roundToInt()
            textOffset = pixelsToUnits(graphics, 14, false).roundToInt()
            drawString(
                graphics,
                "Time (s)",
                maxX - axisLabelOffset.toDouble(),
                minY + textOffset.toDouble(),
                centerHorizontal = true,
                centerVertical = true,
                backgroundColor = Color.white
            )
        }
    }

    override val name: String
        get() = "Time axis"

}