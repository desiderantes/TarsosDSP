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

import be.tarsos.dsp.util.PitchConverter.hertzToAbsoluteCent
import be.tarsos.dsp.util.TWO_PI
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HammingWindow
import java.util.*
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToLong

/**
 *
 *
 * This class implements a spectral peak follower as described in Sethares et
 * al. 2009 - Spectral Tools for Dynamic Tonality and Audio Morphing - section
 * "Analysis-Resynthessis". It calculates a noise floor and picks spectral peaks
 * rising above a calculated noise floor with a certain factor. The noise floor
 * is determined using a simple median filter.
 *
 *
 *
 * Parts of the code is modified from [the code accompanying
 * "Spectral Tools for Dynamic Tonality and Audio Morphing"](http://www.dynamictonality.com/spectools.htm).
 *
 *
 *
 * To get the spectral peaks from an audio frame, call `getPeakList`
 * `<pre>
 * AudioDispatcher dispatcher = new AudioDispatcher(stream, fftsize, overlap);
 * dispatcher.addAudioProcessor(spectralPeakFollower);
 * dispatcher.addAudioProcessor(new AudioProcessor() {
 *
 * public void processingFinished() {
 * }
 *
 * public boolean process(AudioEvent audioEvent) {
 * float[] noiseFloor = SpectralPeakProcessor.calculateNoiseFloor(spectralPeakFollower.getMagnitudes(), medianFilterLength, noiseFloorFactor);
 * List<Integer> localMaxima = SpectralPeakProcessor.findLocalMaxima(spectralPeakFollower.getMagnitudes(), noiseFloor);
 * List<> list = SpectralPeakProcessor.findPeaks(spectralPeakFollower.getMagnitudes(), spectralPeakFollower.getFrequencyEstimates(), localMaxima, numberOfPeaks);
 * // do something with the list...
 * return true;
 * }
 * });
 * dispatcher.run();
</Integer></pre>` *
 *
 * @author Joren Six
 * @author William A. Sethares
 * @author Andrew J. Milne
 * @author Stefan Tiedje
 * @author Anthony Prechtl
 * @author James Plamondon
 */
class SpectralPeakProcessor(
    bufferSize: Int, overlap: Int,
    /**
     * The sample rate of the signal.
     */
    private val sampleRate: Int
) : AudioProcessor {

    /**
     * Cached calculations for the frequency calculation
     */
    private val dt: Double = (bufferSize - overlap) / sampleRate.toDouble()
    private val cbin: Double = (dt * sampleRate / bufferSize.toDouble())
    private val inv_2pi: Double = (1.0 / TWO_PI)
    private val inv_deltat: Double = (1.0 / dt)
    private val inv_2pideltat: Double = (inv_deltat * inv_2pi)

    /**
     * The fft object used to calculate phase and magnitudes.
     */
    private val fft: FFT = FFT(bufferSize, HammingWindow())

    /**
     * The pahse info of the current frame.
     */
    private val currentPhaseOffsets: FloatArray = FloatArray(bufferSize / 2)

    /**
     * The magnitudes in the current frame.
     */
    private val magnitudes: FloatArray = FloatArray(bufferSize / 2)

    /**
     * Detailed frequency estimates for each bin, using phase info
     */
    private val frequencyEstimates: FloatArray = FloatArray(bufferSize / 2)

    /**
     * The phase information of the previous frame, or null.
     */
    private var previousPhaseOffsets: FloatArray? = null
    private fun calculateFFT(audio: FloatArray) {
        // Clone to prevent overwriting audio data
        val fftData = audio.clone()
        // Extract the power and phase data
        fft.powerPhaseFFT(fftData, magnitudes, currentPhaseOffsets)
    }

    private fun normalizeMagintudes() {
        var maxMagnitude = (-1e6).toFloat()
        for (i in magnitudes.indices) {
            maxMagnitude = max(maxMagnitude, magnitudes[i])
        }

        //log10 of the normalized value
        //adding 75 makes sure the value is above zero, a bit ugly though...
        for (i in 1 until magnitudes.size) {
            magnitudes[i] =
                (10 * log10(magnitudes[i] / maxMagnitude.toDouble())).toFloat() + 75
        }
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val audio = audioEvent.floatBuffer

        // 1. Extract magnitudes, and phase using an FFT.
        calculateFFT(audio)

        // 2. Estimate a detailed frequency for each bin.
        calculateFrequencyEstimates()

        // 3. Normalize the each magnitude.
        normalizeMagintudes()

        // 4. Store the current phase so it can be used for the next frequency estimates block.
        previousPhaseOffsets = currentPhaseOffsets.clone()
        return true
    }

    override fun processingFinished() {}

    /**
     * For each bin, calculate a precise frequency estimate using phase offset.
     */
    private fun calculateFrequencyEstimates() {
        for (i in frequencyEstimates.indices) {
            frequencyEstimates[i] = getFrequencyForBin(i)
        }
    }

    /**
     * @return the magnitudes.
     */
    fun getMagnitudes(): FloatArray {
        return magnitudes.clone()
    }

    /**
     * @return the precise frequency for each bin.
     */
    fun getFrequencyEstimates(): FloatArray {
        return frequencyEstimates.clone()
    }

    /**
     * Calculates a frequency for a bin using phase info, if available.
     *
     * @param binIndex The FFT bin index.
     * @return a frequency, in Hz, calculated using available phase info.
     */
    private fun getFrequencyForBin(binIndex: Int): Float {
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
            val phaseDelta = currentPhaseOffsets[binIndex] - previousPhaseOffsets!![binIndex]
            val k = (cbin * binIndex - inv_2pi * phaseDelta).roundToLong()
            (inv_2pideltat * phaseDelta + inv_deltat * k).toFloat()
        } else {
            fft.binToHz(binIndex, sampleRate.toFloat()).toFloat()
        }
        return frequencyInHertz
    }

    class SpectralPeak(
        /**
         * Timestamp in fractional seconds
         */
        val timeStamp: Float,
        val frequencyInHertz: Float,
        val magnitude: Float,
        val refFrequencyInHertz: Float,
        val bin: Int
    ) {

        val relativeFrequencyInCents: Float
            get() = if (refFrequencyInHertz > 0 && frequencyInHertz > 0) {
                val refInCents =
                    hertzToAbsoluteCent(refFrequencyInHertz.toDouble()).toFloat()
                val valueInCents =
                    hertzToAbsoluteCent(frequencyInHertz.toDouble()).toFloat()
                valueInCents - refInCents
            } else {
                0F
            }

        override fun toString(): String {
            return String.format("%.2f %.2f %.2f", frequencyInHertz, relativeFrequencyInCents, magnitude)
        }

    }

    companion object {
        /**
         * Calculate a noise floor for an array of magnitudes.
         *
         * @param magnitudes         The magnitudes of the current frame.
         * @param medianFilterLength The length of the median filter used to determine the noise floor.
         * @param noiseFloorFactor   The noise floor is multiplied with this factor to determine if the
         * information is either noise or an interesting spectral peak.
         * @return a float array representing the noise floor.
         */
        @JvmStatic
        fun calculateNoiseFloor(
            magnitudes: FloatArray,
            medianFilterLength: Int,
            noiseFloorFactor: Float
        ): FloatArray {
            var noiseFloorBuffer: DoubleArray
            val noisefloor = FloatArray(magnitudes.size)
            val median = median(magnitudes.clone()).toFloat()

            // Naive median filter implementation.
            // For each element take a median of surrounding values (noiseFloorBuffer)
            // Store the median as the noise floor.
            for (i in magnitudes.indices) {
                noiseFloorBuffer = DoubleArray(medianFilterLength)
                var index = 0
                var j = i - medianFilterLength / 2
                while (j <= i + medianFilterLength / 2 && index < noiseFloorBuffer.size) {
                    if (j >= 0 && j < magnitudes.size) {
                        noiseFloorBuffer[index] = magnitudes[j].toDouble()
                    } else {
                        noiseFloorBuffer[index] = median.toDouble()
                    }
                    index++
                    j++
                }
                // calculate the noise floor value.
                noisefloor[i] =
                    (median(noiseFloorBuffer) * noiseFloorFactor)
            }
            val rampLength = 12.0f
            var i = 0
            while (i <= rampLength) {

                //ramp
                var ramp = 1.0f
                ramp = (-1 * ln(i / rampLength.toDouble())).toFloat() + 1.0f
                noisefloor[i] = ramp * noisefloor[i]
                i++
            }
            return noisefloor
        }

        /**
         * Finds the local magintude maxima and stores them in the given list.
         *
         * @param magnitudes The magnitudes.
         * @param noisefloor The noise floor.
         * @return a list of local maxima.
         */
        @JvmStatic
        fun findLocalMaxima(
            magnitudes: FloatArray,
            noisefloor: FloatArray
        ): List<Int> {
            val localMaximaIndexes: MutableList<Int> = ArrayList()
            for (i in 1 until magnitudes.size - 1) {
                val largerThanPrevious = magnitudes[i - 1] < magnitudes[i]
                val largerThanNext = magnitudes[i] > magnitudes[i + 1]
                val largerThanNoiseFloor = magnitudes[i] > noisefloor[i]
                if (largerThanPrevious && largerThanNext && largerThanNoiseFloor) {
                    localMaximaIndexes.add(i)
                }
            }
            return localMaximaIndexes
        }

        /**
         * @param magnitudes the magnitudes.
         * @return the index for the maximum magnitude.
         */
        private fun findMaxMagnitudeIndex(magnitudes: FloatArray): Int {
            var maxMagnitudeIndex = 0
            var maxMagnitude = (-1e6).toFloat()
            for (i in 1 until magnitudes.size - 1) {
                if (magnitudes[i] > maxMagnitude) {
                    maxMagnitude = magnitudes[i]
                    maxMagnitudeIndex = i
                }
            }
            return maxMagnitudeIndex
        }

        /**
         * @param magnitudes         the magnitudes..
         * @param frequencyEstimates The frequency estimates for each bin.
         * @param localMaximaIndexes The indexes of the local maxima.
         * @param numberOfPeaks      The requested number of peaks.
         * @param minDistanceInCents The minimum distance in cents between the peaks
         * @return A list with spectral peaks.
         */
        @JvmStatic
        fun findPeaks(
            magnitudes: FloatArray,
            frequencyEstimates: FloatArray,
            localMaximaIndexes: MutableList<Int>,
            numberOfPeaks: Int,
            minDistanceInCents: Int
        ): List<SpectralPeak> {
            val maxMagnitudeIndex = findMaxMagnitudeIndex(magnitudes)
            val spectralPeakList: MutableList<SpectralPeak> = ArrayList()
            if (localMaximaIndexes.isEmpty()) return spectralPeakList
            var referenceFrequency = 0f
            //the frequency of the bin with the highest magnitude
            referenceFrequency = frequencyEstimates[maxMagnitudeIndex]

            //remove frequency estimates below zero
            run {
                var i = 0
                while (i < localMaximaIndexes.size) {
                    if (frequencyEstimates[localMaximaIndexes[i]] < 0) {
                        localMaximaIndexes.removeAt(i)
                        frequencyEstimates[localMaximaIndexes[i]] = 1F //Hz
                        i--
                    }
                    i++
                }
            }

            //filter the local maxima indexes, remove peaks that are too close to each other
            //assumes that localmaximaIndexes is sorted from lowest to higest index
            run {
                var i = 1
                while (i < localMaximaIndexes.size) {
                    val centCurrent =
                        hertzToAbsoluteCent(frequencyEstimates[localMaximaIndexes[i]].toDouble())
                    val centPrev =
                        hertzToAbsoluteCent(frequencyEstimates[localMaximaIndexes[i - 1]].toDouble())
                    val centDelta = centCurrent - centPrev
                    if (centDelta < minDistanceInCents) {
                        if (magnitudes[localMaximaIndexes[i]] > magnitudes[localMaximaIndexes[i - 1]]) {
                            localMaximaIndexes.removeAt(i - 1)
                        } else {
                            localMaximaIndexes.removeAt(i)
                        }
                        i--
                    }
                    i++
                }
            }

            // Retrieve the maximum values for the indexes
            val maxMagnitudes = FloatArray(localMaximaIndexes.size)
            for (i in localMaximaIndexes.indices) {
                maxMagnitudes[i] = magnitudes[localMaximaIndexes[i]]
            }
            // Sort the magnitudes in ascending order
            Arrays.sort(maxMagnitudes)

            // Find the threshold, the first value or somewhere in the array.
            var peakthresh = maxMagnitudes[0]
            if (maxMagnitudes.size > numberOfPeaks) {
                peakthresh = maxMagnitudes[maxMagnitudes.size - numberOfPeaks]
            }

            //store the peaks
            for (i in localMaximaIndexes) {
                if (magnitudes[i] >= peakthresh) {
                    val frequencyInHertz = frequencyEstimates[i]
                    //ignore frequencies lower than 30Hz
                    val binMagnitude = magnitudes[i]
                    val peak = SpectralPeak(0F, frequencyInHertz, binMagnitude, referenceFrequency, i)
                    spectralPeakList.add(peak)
                }
            }
            return spectralPeakList
        }

        fun median(arr: DoubleArray): Float {
            return percentile(arr, 0.5)
        }

        /**
         * Returns the p-th percentile of values in an array. You can use this
         * function to establish a threshold of acceptance. For example, you can
         * decide to examine candidates who score above the 90th percentile (0.9).
         * The elements of the input array are modified (sorted) by this method.
         *
         * @param arr An array of sample data values that define relative standing.
         * The contents of the input array are sorted by this method.
         * @param p   The percentile value in the range 0..1, inclusive.
         * @return The p-th percentile of values in an array.  If p is not a multiple
         * of 1/(n - 1), this method interpolates to determine the value at
         * the p-th percentile.
         */
        fun percentile(arr: DoubleArray, p: Double): Float {
            require(!(p < 0 || p > 1)) { "Percentile out of range." }

            //	Sort the array in ascending order.
            Arrays.sort(arr)

            //	Calculate the percentile.
            val t = p * (arr.size - 1)
            val i = t.toInt()
            return ((i + 1 - t) * arr[i] + (t - i) * arr[i + 1]).toFloat()
        }

        fun median(m: FloatArray): Double {
//		Sort the array in ascending order.
            Arrays.sort(m)
            val middle = m.size / 2
            return if (m.size % 2 == 1) {
                m[middle].toDouble()
            } else {
                (m[middle - 1] + m[middle]) / 2.0
            }
        }
    }
}