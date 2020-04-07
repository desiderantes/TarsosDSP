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
package be.tarsos.dsp

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * The continuing silence detector does not break the audio processing pipeline when silence is detected.
 */
class SilenceDetector
/**
 * Create a new silence detector with a defined threshold.
 *
 * @param threshold              The threshold which defines when a buffer is silent (in dB).
 * Normal values are [-70.0,-30.0] dB SPL.
 * @param breakProcessingQueueOnSilence
 */ @JvmOverloads constructor(
    //db
    private val threshold: Double = DEFAULT_SILENCE_THRESHOLD,
    private val breakProcessingQueueOnSilence: Boolean = false
) : AudioProcessor {
    var currentSPL = 0.0
    /**
     * Checks if the dBSPL level in the buffer falls below a certain threshold.
     *
     * @param buffer           The buffer with audio information.
     * @param silenceThreshold The threshold in dBSPL
     * @return True if the audio information in buffer corresponds with silence,
     * false otherwise.
     */
    @JvmOverloads
    fun isSilence(buffer: FloatArray, silenceThreshold: Double = threshold): Boolean {
        currentSPL = soundPressureLevel(buffer)
        return currentSPL < silenceThreshold
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val isSilence = isSilence(audioEvent.floatBuffer)
        //break processing chain on silence?
        return if (breakProcessingQueueOnSilence) {
            //break if silent
            !isSilence
        } else {
            //never break the chain
            true
        }
    }

    override fun processingFinished() {}

    companion object {
        const val DEFAULT_SILENCE_THRESHOLD = -70.0 //db

        /**
         * Calculates and returns the root mean square of the signal. Please
         * cache the result since it is calculated every time.
         *
         * @param floatBuffer The audio buffer to calculate the RMS for.
         * @return The [RMS](http://en.wikipedia.org/wiki/Root_mean_square) of
         * the signal present in the current buffer.
         */
        fun calculateRMS(floatBuffer: FloatArray): Double {
            var rms = 0.0
            for (i in floatBuffer.indices) {
                rms += floatBuffer[i] * floatBuffer[i].toDouble()
            }
            rms /= floatBuffer.size.toDouble()
            rms = sqrt(rms)
            return rms
        }

        /**
         * Returns the dBSPL for a buffer.
         *
         * @param buffer The buffer with audio information.
         * @return The dBSPL level for the buffer.
         */
        private fun soundPressureLevel(buffer: FloatArray): Double {
            val rms = calculateRMS(buffer)
            return linearToDecibel(rms)
        }

        /**
         * Converts a linear to a dB value.
         *
         * @param value The value to convert.
         * @return The converted value.
         */
        private fun linearToDecibel(value: Double): Double {
            return 20.0 * log10(value)
        }
    }
}