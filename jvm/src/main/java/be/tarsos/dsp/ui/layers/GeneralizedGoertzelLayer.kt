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

import be.tarsos.dsp.StopAudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.pitch.GeneralizedGoertzel
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.util.PitchConverter
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToInt

class GeneralizedGoertzelLayer(
    private val cs: CoordinateSystem,
    private val audioFile: File,
    binHeightInCents: Int
) : Layer {
    private var features: TreeMap<Double, DoubleArray>? = null
    private var maxSpectralEnergy = 0.0
    private var minSpectralEnergy = 100000.0
    private lateinit var binStartingPointsInCents: FloatArray
    private var binWith = 0f // in seconds
    private var binHeight = 0f // in cents

    override fun draw(graphics: Graphics2D) {
        calculateFeatures()
        if (features != null) {
            val spectralInfoSubMap: Map<Double, DoubleArray> =
                features!!.subMap(
                    cs.getMin(Axis.X) / 1000.0,
                    cs.getMax(Axis.X) / 1000.0
                )
            for ((timeStart, spectralEnergy) in spectralInfoSubMap) {

                // draw the pixels
                for (i in spectralEnergy.indices) {
                    var color = Color.black
                    val centsStartingPoint = binStartingPointsInCents[i]
                    // only draw the visible frequency range
                    if (centsStartingPoint >= cs.getMin(Axis.Y)
                        && centsStartingPoint <= cs.getMax(Axis.Y)
                    ) {
                        val factor = spectralEnergy[i] / maxSpectralEnergy
                        var greyValue = 255 - (factor * 255).toInt()
                        greyValue = 0.coerceAtLeast(greyValue)
                        color = Color(greyValue, greyValue, greyValue)
                        graphics.color = color
                        graphics.fillRect(
                            (timeStart * 1000).roundToInt().toInt(),
                            centsStartingPoint.roundToInt(),
                            (binWith * 1000).roundToInt() as Int,
                            ceil(binHeight.toDouble()).toInt()
                        )
                    }
                }
            }
        }
    }

    fun calculateFeatures() {

        //maxSpectralEnergy = 0;
        //minSpectralEnergy = 100000;
        val blockSize = 8000
        val overlap = 7500


        val lowFrequencyInCents = cs.getMin(Axis.Y).toDouble()
        val highFrequencyInCents = cs.getMax(Axis.Y).toDouble()
        val steps = 50 // 100 steps;
        val stepInCents =
            (highFrequencyInCents - lowFrequencyInCents) / steps.toFloat()

        val frequencies = DoubleArray(steps)
        binStartingPointsInCents = FloatArray(steps) { i ->
            val valueInCents = i * stepInCents + lowFrequencyInCents
            frequencies[i] = PitchConverter.absoluteCentToHertz(valueInCents)
            valueInCents.toFloat()
        }
        try {
            val adp =
                fromFile(audioFile, blockSize, overlap)
            adp.skip(0.0.coerceAtLeast(cs.getMin(Axis.X) / 1000.0))
            adp.addAudioProcessor(StopAudioProcessor(cs.getMax(Axis.X) / 1000.0))
            val sampleRate = adp.format.frameRate
            binWith = (blockSize - overlap) / sampleRate
            binHeight = stepInCents.toFloat()
            val fe =
                TreeMap<Double, DoubleArray>()
            val handler: FrequenciesDetectedHandler = object : FrequenciesDetectedHandler {
                var i = 0
                override fun handleDetectedFrequencies(
                    time: Double, frequencies: DoubleArray,
                    powers: DoubleArray, allFrequencies: DoubleArray,
                    allPowers: DoubleArray
                ) {
                    val timeStamp = 0.0.coerceAtLeast(cs.getMin(Axis.X) / 1000.0) + i * binWith
                    i++
                    fe[timeStamp] = allPowers.clone()
                }
            }
            val goertzel =
                GeneralizedGoertzel(sampleRate, blockSize, frequencies, handler)
            adp.addAudioProcessor(goertzel)
            adp.run()
            for (magnitudes in fe.values) {
                for (i in magnitudes.indices) {
                    if (magnitudes[i] == 0.0) {
                        magnitudes[i] = 1.0 / 1e10.toFloat()
                    }
                    //to dB
                    magnitudes[i] =
                        20 * ln(1 + abs(magnitudes[i])) / ln(
                            10.0
                        )
                    maxSpectralEnergy = magnitudes[i].coerceAtLeast(maxSpectralEnergy)
                    minSpectralEnergy = magnitudes[i].coerceAtMost(minSpectralEnergy)
                }
            }
            minSpectralEnergy = abs(minSpectralEnergy)
            features = fe
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        } catch (e2: IOException) {
            e2.printStackTrace()
        }
    }

    override val name: String
        get() = "Generalized Goertzel Layer"

    init {
        calculateFeatures()
    }
}