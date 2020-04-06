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

class AmplitudeAxisLayer(var cs: CoordinateSystem) : Layer {
    override fun draw(graphics: Graphics2D) {
        if (cs.getUnitsForAxis(Axis.Y) === AxisUnit.AMPLITUDE) {
            drawAmplitudeXAxis(graphics)
        }
    }

    fun drawAmplitudeXAxis(graphics: Graphics2D) {
        graphics.color = Color.black
        val minX = cs.getMin(Axis.X).roundToInt()
        val maxY = cs.getMax(Axis.Y).roundToInt()
        val lineWidthFourPixels =
            pixelsToUnits(graphics, 4, true).roundToInt()
        val textOffset = pixelsToUnits(graphics, 14, true).roundToInt()
        val textLabelOffset =
            pixelsToUnits(graphics, 20, false).roundToInt()
        val lineWidthTwoPixels =
            pixelsToUnits(graphics, 2, true).roundToInt()
        var i = cs.getMin(Axis.Y).toInt()
        while (i < cs.getMax(Axis.Y)) {
            if (i % 100 == 0) {
                graphics.drawLine(minX, i, minX + lineWidthFourPixels, i)
                val text = String.format("%.0f", i / 10.0)
                drawString(
                    graphics,
                    text,
                    minX + textOffset.toDouble(),
                    i.toDouble(),
                    centerHorizontal = true,
                    centerVertical = true,
                    backgroundColor = null
                )
            } else if (i % 10 == 0) {
                graphics.drawLine(minX, i, minX + lineWidthTwoPixels, i)
            }
            i += 1
        }
        graphics.drawLine(minX, 0, minX, maxY)
        drawString(
            graphics,
            "Amplitude (%)",
            minX + textOffset.toDouble(),
            maxY - textLabelOffset.toDouble(),
            centerHorizontal = false,
            centerVertical = true,
            backgroundColor = Color.white
        )
    }

    override val name: String
        get() = "Amplitude Axis"

}