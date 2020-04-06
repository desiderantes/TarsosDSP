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
import java.util.*
import kotlin.math.roundToInt

/**
 * Adds a legend to the upper right corner of the map.
 *
 * @author Joren Six
 */
class LegendLayer(var cs: CoordinateSystem, var pixelsFromRight: Int) : Layer {
    var colors: MutableList<Color> = ArrayList()
    var texts: MutableList<String> = ArrayList()
    override fun draw(graphics: Graphics2D) {
        val maxX = cs.getMax(Axis.X).roundToInt()
        val maxY = cs.getMax(Axis.Y).roundToInt()
        for (i in colors.indices) {
            val text = texts[i]
            val color = colors[i]
            val textYOffset =
                pixelsToUnits(graphics, 14, false).roundToInt()
            val textXOffset =
                pixelsToUnits(graphics, pixelsFromRight, true).roundToInt()
            drawString(
                graphics,
                text,
                maxX - textXOffset.toDouble(),
                maxY - textYOffset * (i + 1).toDouble(),
                centerHorizontal = false,
                centerVertical = true,
                backgroundColor = Color.white,
                textColor = color
            )
        }
    }

    override val name: String
        get() = "Legend"

    fun addEntry(string: String, blue: Color) {
        colors.add(blue)
        texts.add(string)
    }

    fun removeAllEntries() {
        colors.clear()
        texts.clear()
    }

}