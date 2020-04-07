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
package be.tarsos.dsp.pitch

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 *
 *
 * A pitch extractor that extracts the Average Magnitude Difference (AMDF) from
 * an audio buffer. This is a good measure of the Pitch (f0) of a signal.
 *
 *
 *
 * AMDF is calculated by the the difference between the waveform summing a
 * lagged version of itself.
 *
 *
 *
 * The main bulk of the code is written by Eder de Souza for the [jAudio](http://jaudio.sf.net) framework. Adapted for TarsosDSP by
 * Joren Six.
 *
 *
 * @author Eder Souza (ederwander on github)
 * @author Joren Six
 */
class AMDF @JvmOverloads constructor(
    private val sampleRate: Float,
    bufferSize: Int,
    minFrequency: Double = DEFAULT_MIN_FREQUENCY,
    maxFrequency: Double = DEFAULT_MAX_FREQUENCY
) : PitchDetector {
    private val amd: DoubleArray = DoubleArray(bufferSize)
    private val maxPeriod: Long = (sampleRate / minFrequency + 0.5).roundToLong()
    private val minPeriod: Long = (sampleRate / maxFrequency + 0.5).roundToLong()
    private val ratio: Double = DEFAULT_RATIO
    private val sensitivity: Double = DEFAULT_SENSITIVITY

    /**
     * The result of the pitch detection iteration.
     */
    private val result: PitchDetectionResult = PitchDetectionResult()
    override fun getPitch(audioBuffer: FloatArray): PitchDetectionResult {
        var t = 0
        var f0 = -1f
        var minval = Double.POSITIVE_INFINITY
        var maxval = Double.NEGATIVE_INFINITY
        var frames1 = DoubleArray(0)
        var frames2 = DoubleArray(0)
        var calcSub = DoubleArray(0)
        val maxShift = audioBuffer.size
        for (i in 0 until maxShift) {
            frames1 = DoubleArray(maxShift - i + 1)
            frames2 = DoubleArray(maxShift - i + 1)
            t = 0
            for (aux1 in 0 until maxShift - i) {
                t += 1
                frames1[t] = audioBuffer[aux1].toDouble()
            }
            t = 0
            for (aux2 in i until maxShift) {
                t += 1
                frames2[t] = audioBuffer[aux2].toDouble()
            }
            val frameLength = frames1.size
            calcSub = DoubleArray(frameLength)
            for (u in 0 until frameLength) {
                calcSub[u] = frames1[u] - frames2[u]
            }
            var summation = 0.0
            for (l in 0 until frameLength) {
                summation += abs(calcSub[l])
            }
            amd[i] = summation
        }
        for (j in minPeriod.toInt() until maxPeriod.toInt()) {
            if (amd[j] < minval) {
                minval = amd[j]
            }
            if (amd[j] > maxval) {
                maxval = amd[j]
            }
        }
        val cutoff = (sensitivity * (maxval - minval) + minval).roundToInt()
        var j = minPeriod.toInt()
        while (j <= maxPeriod.toInt() && amd[j] > cutoff) {
            j += 1
        }
        val search_length = minPeriod / 2.toDouble()
        minval = amd[j]
        var minpos = j
        var i = j
        while (i < j + search_length && i <= maxPeriod) {
            i += 1
            if (amd[i] < minval) {
                minval = amd[i]
                minpos = i
            }
        }
        if ((amd[minpos] * ratio).roundToLong() < maxval) {
            f0 = sampleRate / minpos
        }
        result.pitch = f0
        result.isPitched = -1f != f0
        result.probability = -1f
        return result
    }

    companion object {
        private const val DEFAULT_MIN_FREQUENCY = 82.0
        private const val DEFAULT_MAX_FREQUENCY = 1000.0
        private const val DEFAULT_RATIO = 5.0
        private const val DEFAULT_SENSITIVITY = 0.1
    }
}