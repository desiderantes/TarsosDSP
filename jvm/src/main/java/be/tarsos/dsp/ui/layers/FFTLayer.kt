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
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.TooltipLayer.TooltipTextGenerator
import be.tarsos.dsp.util.PitchConverter
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HammingWindow
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.*

class FFTLayer(
    private val cs: CoordinateSystem,
    private val audioFile: File,
    private val frameSize: Int,
    private val overlap: Int
) : Layer, TooltipTextGenerator {
    private var features: TreeMap<Double, FFTFrame> = TreeMap()
    private var binWith  = 0f // in seconds
    private var maxSpectralEnergy = 0f
    private var minSpectralEnergy = 100000f
    private lateinit var binStartingPointsInCents: FloatArray
    private lateinit var binHeightsInCents: FloatArray

    /**
     * The default increment in samples.
     */
    private val increment: Int = frameSize - overlap
    override fun draw(graphics: Graphics2D) {
        val spectralInfoSubMap: Map<Double, FFTFrame> =
            features.subMap(
                cs.getMin(Axis.X) / 1000.0,
                cs.getMax(Axis.X) / 1000.0
            )
        for ((timeStart, frame) in spectralInfoSubMap) {


            // draw the pixels
            for (i in frame.magnitudes.indices) {
                var color = Color.black

                //actual energy at frame.frequencyEstimates[i];
                val centsStartingPoint = binStartingPointsInCents[i]
                // only draw the visible frequency range
                if (centsStartingPoint >= cs.getMin(Axis.Y)
                    && centsStartingPoint <= cs.getMax(Axis.Y)
                ) {
                    val factor =
                        (frame.magnitudes[i] - frame.minMagnitude) / (frame.maxMagnitude - frame.minMagnitude)
                    var greyValue = 255 - (factor * 255).toInt()
                    greyValue = 0.coerceAtLeast(greyValue)
                    color = Color(greyValue, greyValue, greyValue)
                    graphics.color = color
                    graphics.fillRect(
                        (timeStart * 1000).roundToInt(),
                        centsStartingPoint.roundToInt(),
                        (binWith * 1000).roundToInt(),
                        ceil(binHeightsInCents[i].toDouble()).toInt()
                    )
                }
            }
        }
    }

    init {
        try {
            val adp =
                fromFile(audioFile, frameSize, overlap)
            val sampleRate = adp.format.sampleRate
            val fe =
                TreeMap<Double, FFTFrame>()
            binWith = increment / sampleRate
            val fft =
                FFT(frameSize, HammingWindow())
            binStartingPointsInCents = FloatArray(frameSize)
            binHeightsInCents = FloatArray(frameSize)
            for (i in 1 until frameSize) {
                binStartingPointsInCents[i] =
                    PitchConverter.hertzToAbsoluteCent(fft.binToHz(i, sampleRate)).toFloat()
                binHeightsInCents[i] =
                    binStartingPointsInCents[i] - binStartingPointsInCents[i - 1]
            }
            val lag = frameSize / sampleRate - binWith / 2.0 // in seconds
            adp.addAudioProcessor(object : AudioProcessor {
                var previousPhaseOffsets: FloatArray? = null
                override fun process(audioEvent: AudioEvent): Boolean {
                    val buffer = audioEvent.floatBuffer.clone()
                    val amplitudes = FloatArray(buffer.size / 2)
                    val phases = FloatArray(buffer.size / 2)

                    // Extract the power and phase data
                    fft.powerPhaseFFT(buffer, amplitudes, phases)
                    val frame = FFTFrame(
                        fft,
                        frameSize,
                        overlap,
                        sampleRate,
                        amplitudes,
                        phases,
                        previousPhaseOffsets
                    )
                    previousPhaseOffsets = phases
                    fe[audioEvent.timeStamp - lag] = frame
                    return true
                }

                override fun processingFinished() {
                    val decay = 0.99f
                    val ramp = 1.01f
                    for (frame in fe.values) {
                        maxSpectralEnergy =
                            frame.calculateMaxMagnitude().coerceAtLeast(maxSpectralEnergy)
                        frame.maxMagnitude = maxSpectralEnergy
                        minSpectralEnergy =
                            frame.calculateMinMagnitude().coerceAtMost(minSpectralEnergy)
                        frame.minMagnitude = minSpectralEnergy
                        maxSpectralEnergy *= decay
                        minSpectralEnergy *= ramp
                    }
                    features = fe
                }
            })
            Thread(adp, "Calculate FFT").start()
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        } catch (e2: IOException) {
            e2.printStackTrace()
        }
    }

    override val name: String
        get() = "FFT Layer"

    override fun generateTooltip(cs: CoordinateSystem, point: Point2D): String {
        var tooltip = ""
        val timestampInSeconds = point.x / 1000.0
        val ceilingEntry = features.ceilingEntry(timestampInSeconds)
        val floorEntry = features.floorEntry(timestampInSeconds)
        val diffToFloor = abs(floorEntry.key - timestampInSeconds)
        val diffToCeil = abs(floorEntry.key - timestampInSeconds)
        val entry: Map.Entry<Double, FFTFrame>
        entry = if (diffToCeil > diffToFloor) {
            floorEntry
        } else {
            ceilingEntry
        }
        val frame = entry.value
        var binIndex = 0
        for (i in binStartingPointsInCents.indices) {
            if (binStartingPointsInCents[i] > point.y && binIndex == 0) {
                binIndex = i - 1
            }
        }
        val frequency = frame.getFrequencyForBin(binIndex)


        //double binSize = binStartingPointsInCents[binIndex+1] - binStartingPointsInCents[binIndex];
        tooltip = String.format(
            "Bin: %d  Estimated Frequency: %.02fHz  Time: %.03fs  ",
            binIndex,
            frequency,
            timestampInSeconds
        )
        return tooltip
    }

    private class FFTFrame(
        private val fft: FFT,
        bufferSize: Int,
        overlap: Int,
        private val sampleRate: Float,
        val magnitudes: FloatArray,
        private val currentPhaseOffsets: FloatArray,
        private val previousPhaseOffsets: FloatArray?
    ) {
        /**
         * Cached calculations for the frequency calculation
         */
        private val dt: Double = (bufferSize - overlap) / sampleRate.toDouble()
        private val cbin: Double = (dt * sampleRate / bufferSize.toDouble())
        private val inv2pi: Double = (1.0 / (2.0 * PI))
        private val invDeltaT: Double= (1.0 / dt)
        private val inv2piDeltaT: Double = (invDeltaT * inv2pi)
        private val frequencyEstimates: FloatArray = FloatArray(magnitudes.size)
        var minMagnitude = 0f
        var maxMagnitude = 0f
        private fun convertMagnitudesToDecibel() {
            val minValue = 5 / 1000000.0f
            for (i in magnitudes.indices) {
                //if(magnitudes[i]==0){
                //	magnitudes[i]=minValue;
                //}
                var value = 1 + magnitudes[i].toDouble()
                if (value <= 0) {
                    value = 1 + minValue.toDouble()
                }
                magnitudes[i] =
                    abs(20 * log10(value)).toFloat()
            }
        }

        /**
         * For each bin, calculate a precise frequency estimate using phase offset.
         */
        private fun calculateFrequencyEstimates() {
            for (i in frequencyEstimates.indices) {
                frequencyEstimates[i] = getFrequencyForBin(i)
            }
        }

        /*
		public float[] getFrequencyEstimates(){
			return frequencyEstimates;
		}
		*/
        fun calculateMinMagnitude(): Float {
            var minMag = 4654654f
            for (i in magnitudes.indices) {
                minMag = minMag.coerceAtMost(magnitudes[i])
            }
            return minMag
        }

        fun calculateMaxMagnitude(): Float {
            var maxMag = -1654654f
            for (i in magnitudes.indices) {
                maxMag = maxMag.coerceAtLeast(magnitudes[i])
            }
            return maxMag
        }

        /**
         * Calculates a frequency for a bin using phase info, if available.
         *
         * @param binIndex The FFT bin index.
         * @return a frequency, in Hz, calculated using available phase info.
         */
        fun getFrequencyForBin(binIndex: Int): Float {
            val frequencyInHertz: Float
            // use the phase delta information to get a more precise
            // frequency estimate
            // if the phase of the previous frame is available.
            // See
            // * Moore 1976
            // "The use of phase vocoder in computer music applications"
            // * Sethares et al. 2009 - Spectral Tools for Dynamic
            // Tonality and Audio Morphing
            // * Laroche and Dolson 1999
            frequencyInHertz = if (previousPhaseOffsets != null) {
                val phaseDelta =
                    currentPhaseOffsets[binIndex] - previousPhaseOffsets[binIndex]
                val k = (cbin * binIndex - inv2pi * phaseDelta).roundToInt()
                (inv2piDeltaT * phaseDelta + invDeltaT * k).toFloat()
            } else {
                fft.binToHz(binIndex, sampleRate).toFloat()
            }
            return frequencyInHertz
        }

        init {
            calculateFrequencyEstimates()
            convertMagnitudesToDecibel()
        }
    }
}