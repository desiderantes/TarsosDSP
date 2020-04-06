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
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.roundToInt

class BackgroundLayer @JvmOverloads constructor(
    private val cs: CoordinateSystem,
    private val color: Color = Color.WHITE
) : Layer {
    override fun draw(graphics: Graphics2D) {
        graphics.color = color
        graphics.fillRect(
            cs.getMin(Axis.X).roundToInt(),
            cs.getMin(Axis.Y).roundToInt(),
            cs.getDelta(Axis.X).roundToInt(),
            cs.getDelta(Axis.Y).roundToInt()
        )
    }

    override val name: String
        get() = "Background layer"

}