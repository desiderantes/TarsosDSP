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

/**
 * An implementation of the AUBIO_YIN pitch tracking algorithm. See [the YIN paper.](http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf) Implementation based on [aubio](http://aubio.org)
 *
 * @author Joren Six
 * @author Paul Brossier
 */
class Yin @JvmOverloads constructor(
    /**
     * The audio sample rate. Most audio has a sample rate of 44.1kHz.
     */
    private val sampleRate: Float,
    bufferSize: Int,
    /**
     * The actual YIN threshold.
     */
    private val threshold: Double = DEFAULT_THRESHOLD
) : PitchDetector {

    /**
     * The buffer that stores the calculated values. It is exactly half the size
     * of the input buffer.
     */
    private val yinBuffer: FloatArray = FloatArray(bufferSize / 2)

    /**
     * The result of the pitch detection iteration.
     */
    private val result: PitchDetectionResult = PitchDetectionResult()

    /**
     * The main flow of the YIN algorithm. Returns a pitch value in Hz or -1 if
     * no pitch is detected.
     *
     * @return a pitch value in Hz or -1 if no pitch is detected.
     */
    override fun getPitch(audioBuffer: FloatArray): PitchDetectionResult {
        val tauEstimate: Int
        val pitchInHertz: Float

        // step 2
        difference(audioBuffer)

        // step 3
        cumulativeMeanNormalizedDifference()

        // step 4
        tauEstimate = absoluteThreshold()

        // step 5
        pitchInHertz = if (tauEstimate != -1) {
            val betterTau = parabolicInterpolation(tauEstimate)

            // step 6
            // TODO Implement optimization for the AUBIO_YIN algorithm.
            // 0.77% => 0.5% error rate,
            // using the data of the YIN paper
            // bestLocalEstimate()

            // conversion to Hz
            sampleRate / betterTau
        } else {
            // no pitch found
            -1f
        }
        result.pitch = pitchInHertz
        return result
    }

    /**
     * Implements the difference function as described in step 2 of the YIN
     * paper.
     */
    private fun difference(audioBuffer: FloatArray) {
        var index: Int
        var delta: Float
        var tau: Int = 0
        while (tau < yinBuffer.size) {
            yinBuffer[tau] = 0F
            tau++
        }
        tau = 1
        while (tau < yinBuffer.size) {
            index = 0
            while (index < yinBuffer.size) {
                delta = audioBuffer[index] - audioBuffer[index + tau]
                yinBuffer[tau] += delta * delta
                index++
            }
            tau++
        }
    }

    /**
     * The cumulative mean normalized difference function as described in step 3
     * of the YIN paper. <br></br>
     * `
     * yinBuffer[0] == yinBuffer[1] = 1
    ` *
     */
    private fun cumulativeMeanNormalizedDifference() {
        var tau: Int
        yinBuffer[0] = 1F
        var runningSum = 0f
        tau = 1
        while (tau < yinBuffer.size) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] *= tau / runningSum
            tau++
        }
    }

    /**
     * Implements step 4 of the AUBIO_YIN paper.
     */
    private fun absoluteThreshold(): Int {
        // Uses another loop construct
        // than the AUBIO implementation
        var tau: Int
        // first two positions in yinBuffer are always 1
        // So start at the third (index 2)
        tau = 2
        while (tau < yinBuffer.size) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < yinBuffer.size && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                // found tau, exit loop and return
                // store the probability
                // From the YIN paper: The threshold determines the list of
                // candidates admitted to the set, and can be interpreted as the
                // proportion of aperiodic power tolerated
                // within a periodic signal.
                //
                // Since we want the periodicity and and not aperiodicity:
                // periodicity = 1 - aperiodicity
                result.probability = 1 - yinBuffer[tau]
                break
            }
            tau++
        }


        // if no pitch found, tau => -1
        if (tau == yinBuffer.size || yinBuffer[tau] >= threshold) {
            tau = -1
            result.probability = 0f
            result.isPitched = false
        } else {
            result.isPitched = true
        }
        return tau
    }

    /**
     * Implements step 5 of the AUBIO_YIN paper. It refines the estimated tau
     * value using parabolic interpolation. This is needed to detect higher
     * frequencies more precisely. See http://fizyka.umk.pl/nrbook/c10-2.pdf and
     * for more background
     * http://fedc.wiwi.hu-berlin.de/xplore/tutorials/xegbohtmlnode62.html
     *
     * @param tauEstimate The estimated tau value.
     * @return A better, more precise tau value.
     */
    private fun parabolicInterpolation(tauEstimate: Int): Float {
        val betterTau: Float
        val x0: Int = if (tauEstimate < 1) {
            tauEstimate
        } else {
            tauEstimate - 1
        }
        val x2: Int = if (tauEstimate + 1 < yinBuffer.size) {
            tauEstimate + 1
        } else {
            tauEstimate
        }
        when {
            x0 == tauEstimate -> {
                betterTau = if (yinBuffer[tauEstimate] <= yinBuffer[x2]) {
                    tauEstimate.toFloat()
                } else {
                    x2.toFloat()
                }
            }
            x2 == tauEstimate -> {
                betterTau = if (yinBuffer[tauEstimate] <= yinBuffer[x0]) {
                    tauEstimate.toFloat()
                } else {
                    x0.toFloat()
                }
            }
            else -> {
                val s0: Float = yinBuffer[x0]
                val s1: Float = yinBuffer[tauEstimate]
                val s2: Float = yinBuffer[x2]
                // fixed AUBIO implementation, thanks to Karl Helgason:
                // (2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
                betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0))
            }
        }
        return betterTau
    }

    companion object {
        /**
         * The default size of an audio buffer (in samples).
         */
        const val DEFAULT_BUFFER_SIZE = 2048

        /**
         * The default overlap of two consecutive audio buffers (in samples).
         */
        const val DEFAULT_OVERLAP = 1536

        /**
         * The default YIN threshold value. Should be around 0.10~0.15. See YIN
         * paper for more information.
         */
        private const val DEFAULT_THRESHOLD = 0.20
    }
    /**
     * Create a new pitch detector for a stream with the defined sample rate.
     * Processes the audio in blocks of the defined size.
     *
     * @param sampleRate The sample rate of the audio stream. E.g. 44.1 kHz.
     * @param bufferSize      The size of a buffer. E.g. 1024.
     * @param threshold    The parameter that defines which peaks are kept as possible
     * pitch candidates. See the YIN paper for more details.
     */
}