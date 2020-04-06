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
import be.tarsos.dsp.ui.layers.LayerUtilities.unitsToPixels
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.roundToInt

class HorizontalFrequencyAxisLayer(var cs: CoordinateSystem) : Layer {
    override fun draw(graphics: Graphics2D) {

        //draw legend
        graphics.color = Color.black
        val minY = (cs.getMin(Axis.Y) + 1).roundToInt()
        val minX = cs.getMin(Axis.X).roundToInt()
        val maxX = cs.getMax(Axis.X).roundToInt()
        val wideMarkHeight = pixelsToUnits(graphics, 8, false).roundToInt()
        val smallMarkHeight = pixelsToUnits(graphics, 6, false).roundToInt()
        val verySmallMarkHeight = pixelsToUnits(graphics, 2, false).roundToInt()
        val textOffset = pixelsToUnits(graphics, 12, false).roundToInt()
        val textLabelOffset = pixelsToUnits(graphics, 120, true).roundToInt()
        val widthOf100CentsInPixels = unitsToPixels(graphics, 100f, true)

        //Every 100 and 1200 cents
        for ( i in cs.getMin(Axis.X).toInt() until cs.getMax(Axis.X).toInt() ){
            if (i % 1200 == 0) {
                graphics.drawLine(i, minY, i, minY + wideMarkHeight)
                val text = i.toString()
                drawString(
                    graphics,
                    text,
                    i.toDouble(),
                    minY + textOffset.toDouble(),
                    centerHorizontal = true,
                    centerVertical = false,
                    backgroundColor = null
                )
            } else if (i % 600 == 0) {
                graphics.drawLine(i, minY, i, minY + smallMarkHeight)
            } else if (widthOf100CentsInPixels > 10 && i % 100 == 0) {
                graphics.drawLine(i, minY, i, minY + verySmallMarkHeight)
            }
        }
        graphics.drawLine(minX, minY, maxX, minY)
        drawString(
            graphics,
            "Frequency (cents)",
            maxX - textLabelOffset.toDouble(),
            minY + (4 * wideMarkHeight).toDouble(),
            centerHorizontal = false,
            centerVertical = true,
            backgroundColor = Color.white
        )

        //LayerUtilities.drawString(graphics,"Frequency (cents)",minX+textOffset,maxY-textLabelOffset,false,true,Color.white);
    }

    override val name: String
        get() = "Frequency Axis"

}