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

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.TWO_PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10

/**
 * Contains an implementation of the Goertzel algorithm. It can be used to
 * detect if one or more predefined frequencies are present in a signal. E.g. to
 * do DTMF decoding.
 *
 * @author Joren Six
 */
class Goertzel(
    audioSampleRate: Float, bufferSize: Int,
    /**
     * A list of frequencies to detect.
     */
    private val frequenciesToDetect: DoubleArray, private val handler: FrequenciesDetectedHandler
) : AudioProcessor {

    /**
     * Cached cosine calculations for each frequency to detect.
     */
    private val precalculatedCosines: DoubleArray = DoubleArray(frequenciesToDetect.size)

    /**
     * Cached wnk calculations for each frequency to detect.
     */
    private val precalculatedWnk: DoubleArray = DoubleArray(frequenciesToDetect.size)

    /**
     * A calculated power for each frequency to detect. This array is reused for
     * performance reasons.
     */
    private val calculatedPowers: DoubleArray = DoubleArray(frequenciesToDetect.size)
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        var skn0: Double
        var skn1: Double
        var skn2: Double
        var numberOfDetectedFrequencies = 0
        for (j in frequenciesToDetect.indices) {
            skn2 = 0.0
            skn1 = skn2
            skn0 = skn1
            for (i in audioFloatBuffer.indices) {
                skn2 = skn1
                skn1 = skn0
                skn0 = (precalculatedCosines[j] * skn1 - skn2
                        + audioFloatBuffer[i])
            }
            val wnk = precalculatedWnk[j]
            calculatedPowers[j] = 20 * log10(abs(skn0 - wnk * skn1))
            if (calculatedPowers[j] > POWER_THRESHOLD) {
                numberOfDetectedFrequencies++
            }
        }
        if (numberOfDetectedFrequencies > 0) {
            val frequencies = DoubleArray(numberOfDetectedFrequencies)
            val powers = DoubleArray(numberOfDetectedFrequencies)
            var index = 0
            for (j in frequenciesToDetect.indices) {
                if (calculatedPowers[j] > POWER_THRESHOLD) {
                    frequencies[index] = frequenciesToDetect[j]
                    powers[index] = calculatedPowers[j]
                    index++
                }
            }
            handler.handleDetectedFrequencies(
                audioEvent.timeStamp, frequencies, powers,
                frequenciesToDetect.clone(), calculatedPowers.clone()
            )
        }
        return true
    }

    override fun processingFinished() {}

    /**
     * An interface used to react on detected frequencies.
     *
     * @author Joren Six
     */
    interface FrequenciesDetectedHandler {
        /**
         * React on detected frequencies.
         *
         * @param frequencies    A list of detected frequencies.
         * @param powers         A list of powers of the detected frequencies.
         * @param allFrequencies A list of all frequencies that were checked.
         * @param allPowers      A list of powers of all frequencies that were checked.
         */
        fun handleDetectedFrequencies(
            timestamp: Double, frequencies: DoubleArray?,
            powers: DoubleArray?, allFrequencies: DoubleArray?,
            allPowers: DoubleArray?
        )
    }

    companion object {
        /**
         * If the power in dB is higher than this threshold, the frequency is
         * present in the signal.
         */
        private const val POWER_THRESHOLD = 35.0 // in dB
    }

    init {
        for (i in frequenciesToDetect.indices) {
            precalculatedCosines[i] = 2 * cos(
                (TWO_PI * frequenciesToDetect[i]) / audioSampleRate
            )
            precalculatedWnk[i] = exp(
                (-TWO_PI * frequenciesToDetect[i]) / audioSampleRate
            )
        }
    }
}