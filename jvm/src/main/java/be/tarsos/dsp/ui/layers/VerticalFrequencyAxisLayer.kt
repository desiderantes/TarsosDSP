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
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.LayerUtilities.drawString
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.roundToInt

class VerticalFrequencyAxisLayer(var cs: CoordinateSystem) : Layer {
    override fun draw(graphics: Graphics2D) {

        //draw legend
        graphics.color = Color.black
        val minX = cs.getMin(Axis.X).roundToInt()
        val maxY = cs.getMax(Axis.Y).roundToInt()
        val wideMarkWidth =
            pixelsToUnits(graphics, 8, true).roundToInt()
        val smallMarkWidth =
            pixelsToUnits(graphics, 4, true).roundToInt()
        val textOffset = pixelsToUnits(graphics, 12, true).roundToInt()
        val textLabelOffset =
            pixelsToUnits(graphics, 12, false).roundToInt()

        //Every 100 and 1200 cents
        var i = cs.getMin(Axis.Y).toInt()
        while (i < cs.getMax(Axis.Y)) {
            if (i % 1200 == 0) {
                graphics.drawLine(minX, i, minX + wideMarkWidth, i)
                val text = i.toString()
                drawString(
                    graphics,
                    text,
                    minX + textOffset.toDouble(),
                    i.toDouble(),
                    centerHorizontal = false,
                    centerVertical = true,
                    backgroundColor = null
                )
            } else if (i % 100 == 0) {
                graphics.drawLine(minX, i, minX + smallMarkWidth, i)
            }
            i++
        }
        drawString(
            graphics,
            "Frequency (cents)",
            minX + textOffset.toDouble(),
            maxY - textLabelOffset.toDouble(),
            centerHorizontal = false,
            centerVertical = true,
            backgroundColor = Color.white
        )
    }

    override val name: String
        get() = "Frequency Axis"

}