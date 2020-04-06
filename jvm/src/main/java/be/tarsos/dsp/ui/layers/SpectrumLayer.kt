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
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import be.tarsos.dsp.util.PitchConverter
import java.awt.Color
import java.awt.Graphics2D
import java.util.*
import kotlin.math.roundToInt

class SpectrumLayer(
    private val cs: CoordinateSystem,
    private var fftSize: Int,
    private var sampleRate: Int,
    private val color: Color
) : Layer {
    private var spectrum: FloatArray? = null
    private val peaksInBins: MutableList<Int>
    private val multiplier = 10f
    override fun draw(graphics: Graphics2D) {
        if (spectrum != null) {
            graphics.color = color
            var prevFreqInCents = 0
            var prevMagnitude = 0
            for (i in 1 until spectrum!!.size) {
                val hertzValue = i * sampleRate / fftSize.toFloat()
                val frequencyInCents = PitchConverter.hertzToAbsoluteCent(hertzValue.toDouble()).roundToInt()
                val magnitude = (spectrum!![i] * multiplier).roundToInt()
                if (cs.getMin(Axis.X) < frequencyInCents && frequencyInCents < cs.getMax(Axis.X)) {
                    graphics.drawLine(prevFreqInCents, prevMagnitude, frequencyInCents, magnitude)
                    prevFreqInCents = frequencyInCents
                    prevMagnitude = magnitude
                }
            }
            val markerWidth = pixelsToUnits(graphics, 7, true).roundToInt()
            val markerHeight = pixelsToUnits(graphics, 7, false).roundToInt()
            graphics.color = Color.blue
            for (i in peaksInBins.indices) {
                val bin = peaksInBins[i]
                val hertzValue = bin * sampleRate / fftSize.toFloat()
                val frequencyInCents = (PitchConverter.hertzToAbsoluteCent(hertzValue.toDouble()) - markerWidth / 2.0f).roundToInt()
                if (cs.getMin(Axis.X) < frequencyInCents && frequencyInCents < cs.getMax(
                        Axis.X
                    )
                ) {
                    val magnitude = (spectrum!![bin] * multiplier - markerHeight / 2.0f).roundToInt()
                    graphics.drawOval(
                        frequencyInCents,
                        magnitude,
                        markerWidth,
                        markerHeight
                    )
                }
            }
        }
    }

    override val name: String
        get() = "Spectrum"

    fun setSpectrum(spectrum: FloatArray) {
        this.spectrum = spectrum
    }

    fun setPeak(binIndex: Int) {
        peaksInBins.add(binIndex)
    }

    fun clearPeaks() {
        peaksInBins.clear()
    }

    fun setSampleRate(sampleRate: Int) {
        this.sampleRate = sampleRate
    }

    fun setFFTSize(fftSize: Int) {
        this.fftSize = fftSize
    }

    init {
        peaksInBins = ArrayList()
    }
}