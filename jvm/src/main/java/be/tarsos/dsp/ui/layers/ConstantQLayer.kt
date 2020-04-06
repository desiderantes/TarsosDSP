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

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.ConstantQ
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.util.PitchConverter
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln1p
import kotlin.math.roundToInt

class ConstantQLayer(
    private val cs: CoordinateSystem,
    private val audioFile: File,
    /**
     * The default increment in samples.
     */
    private val increment: Int,
    /**
     * The default minimum pitch, in absolute cents (+-66 Hz)
     */
    private var minimumFrequencyInCents: Int = 4000,
    /**
     * The default maximum pitch, in absolute cents (+-4200 Hz)
     */
    private var maximumFrequencyInCents: Int = 10500,
    /**
     * The default number of bins per octave.
     */
    private val binsPerOctave: Int = 48
) : Layer {
    private var features: TreeMap<Double, FloatArray>? = null
    private var maxSpectralEnergy = 0f
    private var minSpectralEnergy = 100000f
    private var binStartingPointsInCents: FloatArray? = null
    private var binWith = 0f // in seconds
    private var binHeight  = 0f // in seconds

    override fun draw(graphics: Graphics2D) {
        if (features != null) {
            val spectralInfoSubMap: Map<Double, FloatArray> =
                features!!.subMap(
                    cs.getMin(Axis.X) / 1000.0,
                    cs.getMax(Axis.X) / 1000.0
                )
            var currentMaxSpectralEnergy = 0.0
            for ((_, spectralEnergy) in spectralInfoSubMap) {
                for (i in spectralEnergy.indices) {
                    currentMaxSpectralEnergy =
                        currentMaxSpectralEnergy.coerceAtLeast(spectralEnergy[i].toDouble())
                }
            }
            for ((timeStart, spectralEnergy) in spectralInfoSubMap) {

                // draw the pixels
                for (i in spectralEnergy.indices) {
                    var color = Color.black
                    val centsStartingPoint = binStartingPointsInCents!![i]
                    // only draw the visible frequency range
                    if (centsStartingPoint >= cs.getMin(Axis.Y)
                        && centsStartingPoint <= cs.getMax(Axis.Y)
                    ) {
                        var greyValue = 255 - (ln1p(spectralEnergy[i].toDouble())
                                / ln1p(currentMaxSpectralEnergy) * 255).toInt()
                        greyValue = 0.coerceAtLeast(greyValue)
                        color = Color(greyValue, greyValue, greyValue)
                        graphics.color = color
                        graphics.fillRect(
                            (timeStart * 1000).roundToInt(),
                            centsStartingPoint.roundToInt(),
                            (binWith * 1000).roundToInt(),
                            ceil(binHeight.toDouble()).toInt()
                        )
                    }
                }
            }
        }
    }

    override val name: String
        get() = "Constant-Q Layer"

    init {
        thread(start = true, name = "Constant Q Initialization") {
            try {
                val minimumFrequencyInHertz =
                    PitchConverter.absoluteCentToHertz(minimumFrequencyInCents.toDouble()).toFloat()
                val maximumFrequencyInHertz =
                    PitchConverter.absoluteCentToHertz(maximumFrequencyInCents.toDouble()).toFloat()
                val sampleRate =
                    fromFile(audioFile, 2048, 0).format
                        .frameRate
                val constantQ = ConstantQ(
                    sampleRate,
                    minimumFrequencyInHertz,
                    maximumFrequencyInHertz,
                    binsPerOctave.toFloat()
                )
                binWith = increment / sampleRate
                binHeight = 1200 / binsPerOctave.toFloat()
                val startingPointsInHertz = constantQ.freqencies
                binStartingPointsInCents = FloatArray(startingPointsInHertz.size){ i->
                    PitchConverter.hertzToAbsoluteCent(startingPointsInHertz[i].toDouble()).toFloat()
                }
                val size = constantQ.ffTlength
                val adp = fromFile(
                    audioFile,
                    size,
                    size - increment
                )
                val constantQLag =
                    size / adp.format.sampleRate - binWith / 2.0 // in seconds
                val fe =
                    TreeMap<Double, FloatArray>()
                adp.addAudioProcessor(constantQ)
                adp.addAudioProcessor(object : AudioProcessor {
                    override fun processingFinished() {
                        val minValue = 5 / 1000000.0f
                        for (magnitudes in fe.values) {
                            for (i in magnitudes.indices) {
                                magnitudes[i] = minValue.coerceAtLeast(magnitudes[i])
                                magnitudes[i] =
                                    ln1p(magnitudes[i].toDouble()).toFloat()
                                maxSpectralEnergy = magnitudes[i].coerceAtLeast(maxSpectralEnergy)
                                minSpectralEnergy = magnitudes[i].coerceAtMost(minSpectralEnergy)
                            }
                        }
                        minSpectralEnergy = abs(minSpectralEnergy)
                        features = fe
                    }

                    override fun process(audioEvent: AudioEvent): Boolean {
                        fe[audioEvent.timeStamp - constantQLag] = constantQ.magnitudes.clone()
                        return true
                    }
                })
                Thread(adp, "Constant Q Calculation").start()
            } catch (e: UnsupportedAudioFileException) {
                e.printStackTrace()
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
        }
    }
}