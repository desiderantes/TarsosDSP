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
package be.tarsos.dsp.mfcc

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HammingWindow
import kotlin.math.*

class MFCC(
    private val samplesPerFrame: Int,
    private val sampleRate: Float,
    //Number of MFCCs per frame
    private val amountOfCepstrumCoef: Int,
    //Number of mel filters (SPHINX-III uses 40)
    protected var amountOfMelFilters: Int,
    lowerFilterFreq: Float,
    upperFilterFreq: Float
) : AudioProcessor {
    //lower limit of filter (or 64 Hz?)
    protected var lowerFilterFreq: Float = lowerFilterFreq.coerceAtLeast(25f)

    //upper limit of filter (or half of sampling freq.?)
    protected var upperFilterFreq: Float = upperFilterFreq.coerceAtMost(sampleRate / 2)
    lateinit var centerFrequencies: IntArray

    //There are as many mfccs as there are frames !?
    //There are then CEPSTRA coefficients per frame
    private var mfcc: FloatArray = FloatArray(0)
    private val fft: FFT = FFT(samplesPerFrame, HammingWindow())

    constructor(samplesPerFrame: Int, sampleRate: Int) : this(
        samplesPerFrame,
        sampleRate.toFloat(),
        30,
        30,
        133.3334f,
        sampleRate.toFloat() / 2f
    )

    override fun process(audioEvent: AudioEvent): Boolean {

        // Magnitude Spectrum
        val bin = magnitudeSpectrum(audioEvent.floatBuffer.clone())
        // get Mel Filterbank
        val fbank = melFilter(bin, centerFrequencies)
        // Non-linear transformation
        val f = nonLinearTransformation(fbank)
        // Cepstral coefficients
        mfcc = cepCoefficients(f)
        return true
    }

    override fun processingFinished() {}

    /**
     * computes the magnitude spectrum of the input frame<br></br>
     * calls: none<br></br>
     * called by: featureExtraction
     *
     * @param frame Input frame signal
     * @return Magnitude Spectrum array
     */
    fun magnitudeSpectrum(frame: FloatArray): FloatArray {
        val magSpectrum = FloatArray(frame.size)

        // calculate FFT for current frame
        fft.forwardTransform(frame)

        // calculate magnitude spectrum
        for (k in 0 until frame.size / 2) {
            magSpectrum[frame.size / 2 + k] = fft.modulus(frame, frame.size / 2 - 1 - k)
            magSpectrum[frame.size / 2 - 1 - k] = magSpectrum[frame.size / 2 + k]
        }
        return magSpectrum
    }

    /**
     * calculates the FFT bin indices<br></br> calls: none<br></br> called by:
     * featureExtraction
     */
    fun calculateFilterBanks() {
        centerFrequencies = IntArray(amountOfMelFilters + 2)
        centerFrequencies[0] = (lowerFilterFreq / sampleRate * samplesPerFrame).roundToInt()
        centerFrequencies[centerFrequencies.size - 1] = (samplesPerFrame / 2)
        val mel = DoubleArray(2)
        mel[0] = freqToMel(lowerFilterFreq).toDouble()
        mel[1] = freqToMel(upperFilterFreq).toDouble()
        val factor =
            ((mel[1] - mel[0]) / (amountOfMelFilters + 1)).toFloat()
        //Calculates te centerfrequencies.
        for (i in 1..amountOfMelFilters) {
            val fc = inverseMel(mel[0] + factor * i) / sampleRate * samplesPerFrame
            centerFrequencies[i] = fc.roundToInt()
        }
    }
    //    /**
    //     * calculates center frequency<br>
    //     * calls: none<br>
    //     * called by: featureExtraction
    //     * @param i Index of mel filters
    //     * @return Center Frequency
    //     */
    //    private static float centerFreq(int i,float samplingRate){
    //        double mel[] = new double[2];
    //        mel[0] = freqToMel(lowerFilterFreq);
    //        mel[1] = freqToMel(samplingRate / 2);
    //
    //        // take inverse mel of:
    //        double temp = mel[0] + ((mel[1] - mel[0]) / (amountOfMelFilters + 1)) * i;
    //        return inverseMel(temp);
    //    }
    /**
     * the output of mel filtering is subjected to a logarithm function (natural logarithm)<br></br>
     * calls: none<br></br>
     * called by: featureExtraction
     *
     * @param fbank Output of mel filtering
     * @return Natural log of the output of mel filtering
     */
    fun nonLinearTransformation(fbank: FloatArray): FloatArray {
        val f = FloatArray(fbank.size)
        val FLOOR = -50f
        for (i in fbank.indices) {
            f[i] = ln(fbank[i].toDouble()).toFloat()

            // check if ln() returns a value less than the floor
            if (f[i] < FLOOR) f[i] = FLOOR
        }
        return f
    }

    /**
     * Calculate the output of the mel filter<br></br> calls: none called by:
     * featureExtraction
     *
     * @param bin               The bins.
     * @param centerFrequencies The frequency centers.
     * @return Output of mel filter.
     */
    fun melFilter(bin: FloatArray, centerFrequencies: IntArray): FloatArray {
        val temp = FloatArray(amountOfMelFilters + 2)
        for (k in 1..amountOfMelFilters) {
            var num1 = 0f
            var num2 = 0f
            var den =
                (centerFrequencies[k] - centerFrequencies[k - 1] + 1).toFloat()
            for (i in centerFrequencies[k - 1]..centerFrequencies[k]) {
                num1 += bin[i] * (i - centerFrequencies[k - 1] + 1)
            }
            num1 /= den
            den = (centerFrequencies[k + 1] - centerFrequencies[k] + 1).toFloat()
            for (i in centerFrequencies[k] + 1..centerFrequencies[k + 1]) {
                num2 += bin[i] * (1 - (i - centerFrequencies[k]) / den)
            }
            temp[k] = num1 + num2
        }
        val fbank = FloatArray(amountOfMelFilters)
        for (i in 0 until amountOfMelFilters) {
            fbank[i] = temp[i + 1]
        }
        return fbank
    }

    /**
     * Cepstral coefficients are calculated from the output of the Non-linear Transformation method<br></br>
     * calls: none<br></br>
     * called by: featureExtraction
     *
     * @param f Output of the Non-linear Transformation method
     * @return Cepstral Coefficients
     */
    fun cepCoefficients(f: FloatArray): FloatArray {
        val cepc = FloatArray(amountOfCepstrumCoef)
        for (i in cepc.indices) {
            for (j in f.indices) {
                cepc[i] += (f[j] * cos(PI * i / f.size * (j + 0.5))).toFloat()
            }
        }
        return cepc
    }

    val mFCC: FloatArray
        get() = mfcc.clone()

    companion object {
        /**
         * convert frequency to mel-frequency<br></br>
         * calls: none<br></br>
         * called by: featureExtraction
         *
         * @param freq Frequency
         * @return Mel-Frequency
         */
        protected fun freqToMel(freq: Float): Float {
            return (2595 * log10(1 + freq / 700))
        }

        /**
         * calculates the inverse of Mel Frequency<br></br>
         * calls: none<br></br>
         * called by: featureExtraction
         */
        private fun inverseMel(x: Double): Float {
            return (700 * (10.0.pow(x / 2595) - 1)).toFloat()
        }
    }

    init {
        calculateFilterBanks()
    }
}