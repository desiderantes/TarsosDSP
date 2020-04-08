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
/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -----------------------------------------------------------
 *
 *  TarsosDSP is developed by Joren Six at
 *  The Royal Academy of Fine Arts & Royal Conservatory,
 *  University College Ghent,
 *  Hoogpoort 64, 9000 Ghent - Belgium
 *
 *  http://tarsos.0110.be/tag/TarsosDSP
 *  https://github.com/JorenSix/TarsosDSP
 *  http://tarsos.0110.be/releases/TarsosDSP/
 *
 */
/*
 * Copyright (c) 2006, Karl Helgason
 *
 * 2007/1/8 modified by p.j.leonard
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package be.tarsos.dsp

import be.tarsos.dsp.util.TWO_PI
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.*

/**
 * Implementation of the Constant Q Transform.<br></br> References:
 *
 *
 * Judith C. Brown, [
 * Calculation of a constant Q spectral transform](http://www.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf), J. Acoust. Soc. Am.,
 * 89(1): 425-434, 1991.
 *
 *
 *
 * Judith C. Brown and Miller S. Puckette, [An efficient algorithm for the calculation of a constant Q transform](http://www.wellesley.edu/Physics/brown/pubs/effalgV92P2698-P2701.pdf), J.
 * Acoust. Soc. Am., Vol. 92, No. 5, November 1992
 *
 *
 *
 * Benjamin Blankertz, [The Constant Q Transform](http://wwwmath1.uni-muenster.de/logik/org/staff/blankertz/constQ/constQ.pdf)
 *
 *
 * @author Joren Six
 * @author Karl Helgason
 * @author P.J Leonard
 */
class ConstantQ @JvmOverloads constructor(
    sampleRate: Float,
    /**
     * The minimum frequency, in Hertz. The Constant-Q factors are calculated
     * starting from this frequency.
     */
    private val minimumFrequency: Float,
    /**
     * The maximum frequency in Hertz.
     */
    private val maximumFrequency: Float,
    binsPerOctave: Float,
    threshold: Float = 0.001f,
    spread: Float = 1.0f
) : AudioProcessor {

    /**
     * @return The list of starting frequencies for each band. In Hertz.
     */
    /**
     * Lists the start of each frequency bin, in Hertz.
     */
    val frequencies: FloatArray
    private val qKernel: Array<FloatArray?>
    private val qKernel_indexes: Array<IntArray?>

    /**
     * Return the Constant Q coefficients calculated for the previous audio
     * buffer. Beware: the array is reused for performance reasons. If your need
     * to cache your results, please copy the array.
     *
     * @return The array with constant q coefficients. If you for example are
     * interested in coefficients between 256 and 1024 Hz (2^8 and 2^10
     * Hz) and you requested 12 bins per octave, you will need 12
     * bins/octave * 2 octaves * 2 entries/bin = 48 places in the output
     * buffer. The coefficient needs two entries in the output buffer
     * since they are complex numbers.
     */
    /**
     * The array with constant q coefficients. If you for
     * example are interested in coefficients between 256 and 1024 Hz
     * (2^8 and 2^10 Hz) and you requested 12 bins per octave, you
     * will need 12 bins/octave * 2 octaves * 2 entries/bin = 48
     * places in the output buffer. The coefficient needs two entries
     * in the output buffer since they are complex numbers.
     */
    val coefficients: FloatArray

    /**
     * Returns the Constant Q magnitudes calculated for the previous audio
     * buffer. Beware: the array is reused for performance reasons. If your need
     * to cache your results, please copy the array.
     *
     * @return The output buffer with constant q magnitudes. If you for example are
     * interested in coefficients between 256 and 1024 Hz (2^8 and 2^10 Hz) and
     * you requested 12 bins per octave, you will need 12 bins/octave * 2
     * octaves = 24 places in the output buffer.
     */
    /**
     * The output buffer with constant q magnitudes. If you for example are
     * interested in coefficients between 256 and 1024 Hz (2^8 and 2^10 Hz) and
     * you requested 12 bins per octave, you will need 12 bins/octave * 2
     * octaves = 24 places in the output buffer.
     */
    val magnitudes: FloatArray

    /**
     * @return The required length the FFT.
     */
    /**
     * The length of the underlying FFT.
     */
    var fFTlength: Int

    /**
     * @return the number of bins every octave.
     */
    /**
     * The number of bins per octave.
     */
    val binsPerOctave: Int = binsPerOctave.toInt()

    /**
     * The underlying FFT object.
     */
    private val fft: FFT

    /**
     * Take an input buffer with audio and calculate the constant Q
     * coefficients.
     *
     * @param inputBuffer The input buffer with audio.
     */
    fun calculate(inputBuffer: FloatArray) {
        fft.forwardTransform(inputBuffer)
        for (i in qKernel.indices) {
            val kernel = qKernel[i]
            val indexes = qKernel_indexes[i]
            var t_r = 0f
            var t_i = 0f
            var j = 0
            var l = 0
            while (j < kernel!!.size) {
                val jj = indexes!![l]
                val b_r = inputBuffer[jj]
                val b_i = inputBuffer[jj + 1]
                val k_r = kernel[j]
                val k_i = kernel[j + 1]
                // COMPLEX: T += B * K
                t_r += b_r * k_r - b_i * k_i
                t_i += b_r * k_i + b_i * k_r
                j += 2
                l++
            }
            coefficients[i * 2] = t_r
            coefficients[i * 2 + 1] = t_i
        }
    }

    /**
     * Take an input buffer with audio and calculate the constant Q magnitudes.
     *
     * @param inputBuffer The input buffer with audio.
     */
    fun calculateMagintudes(inputBuffer: FloatArray) {
        calculate(inputBuffer)
        for (i in magnitudes.indices) {
            magnitudes[i] = sqrt(
                coefficients[i * 2] * coefficients[i * 2] + coefficients[i * 2 + 1] * coefficients[i * 2 + 1]
                    .toDouble()
            ).toFloat()
        }
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioBuffer = audioEvent.floatBuffer.clone()
        require(audioBuffer.size == fFTlength) {
            String.format(
                "The length of the fft (%d) should be the same as the length of the audio buffer (%d)",
                fFTlength,
                audioBuffer.size
            )
        }
        calculateMagintudes(audioBuffer)
        return true
    }

    override fun processingFinished() {
        // Do nothing.
    }
    //----GETTERS

    /**
     * @return The number of coefficients, output bands.
     */
    val numberOfOutputBands: Int
        get() = frequencies.size

    init {

        // Calculate Constant Q
        val q = 1.0 / (2.0.pow(1.0 / binsPerOctave) - 1.0) / spread

        // Calculate number of output bins
        val numberOfBins = ceil(
            binsPerOctave * ln(maximumFrequency / minimumFrequency.toDouble()) / ln(2.0)
        ).toInt()

        // Initialize the coefficients array (complex number so 2 x number of bins)
        coefficients = FloatArray(numberOfBins * 2)

        // Initialize the magnitudes array
        magnitudes = FloatArray(numberOfBins)


        // Calculate the minimum length of the FFT to support the minimum
        // frequency
        val calc_fftlen = ceil(q * sampleRate / minimumFrequency).toFloat()

        // No need to use power of 2 FFT length.
        fFTlength = calc_fftlen.toInt()

        //System.out.println(fftLength);
        //The FFT length needs to be a power of two for performance reasons:
        fFTlength = 2.0.pow(ceil(ln(calc_fftlen.toDouble()) / ln(2.0))).toInt()

        // Create FFT object
        fft = FFT(fFTlength)
        qKernel = arrayOfNulls(numberOfBins)
        qKernel_indexes = arrayOfNulls(numberOfBins)
        frequencies = FloatArray(numberOfBins)

        // Calculate Constant Q kernels
        val temp = FloatArray(fFTlength * 2)
        val ctemp = FloatArray(fFTlength * 2)
        val cindexes = IntArray(fFTlength)
        for (i in 0 until numberOfBins) {
            var sKernel = temp
            // Calculate the frequency of current bin
            frequencies[i] =
                (minimumFrequency * 2.0.pow(i / binsPerOctave.toDouble())).toFloat()

            // Calculate length of window
            val len = min(
                ceil(q * sampleRate / frequencies[i]),
                fFTlength.toDouble()
            ).toInt()
            for (j in 0 until len) {
                var window =
                    -.5 * cos(TWO_PI * j.toDouble() / len.toDouble()) + .5
                // Hanning Window
                // double window = -.46*Math.cos(2.*Math.PI*(double)j/(double)len)+.54; // Hamming Window
                window /= len.toDouble()

                // Calculate kernel
                val x = TWO_PI * q * j.toDouble() / len.toDouble()
                sKernel[j * 2] = (window * cos(x)).toFloat()
                sKernel[j * 2 + 1] = (window * sin(x)).toFloat()
            }
            for (j in len * 2 until fFTlength * 2) {
                sKernel[j] = 0F
            }

            // Perform FFT on kernel
            fft.complexForwardTransform(sKernel)

            // Remove all zeros from kernel to improve performance
            var k = 0
            run {
                var j = 0
                var j2 = sKernel.size - 2
                while (j < sKernel.size / 2) {
                    var absval = sqrt(
                        sKernel[j] * sKernel[j] + sKernel[j + 1] * sKernel[j + 1].toDouble()
                    )
                    absval += sqrt(
                        sKernel[j2] * sKernel[j2] + sKernel[j2 + 1] * sKernel[j2 + 1].toDouble()
                    )
                    if (absval > threshold) {
                        cindexes[k] = j
                        ctemp[2 * k] = sKernel[j] + sKernel[j2]
                        ctemp[2 * k + 1] = sKernel[j + 1] + sKernel[j2 + 1]
                        k++
                    }
                    j += 2
                    j2 -= 2
                }
            }
            sKernel = FloatArray(k * 2)
            val indexes = IntArray(k)
            for (j in 0 until k * 2) sKernel[j] = ctemp[j]
            for (j in 0 until k) indexes[j] = cindexes[j]

            // Normalize fft output
            for (j in sKernel.indices) sKernel[j] = sKernel[j].div(fFTlength)

            // Perform complex conjugate on sKernel
            run {
                var j = 1
                while (j < sKernel.size) {
                    sKernel[j] = -sKernel[j]
                    j += 2
                }
            }
            for (j in sKernel.indices) sKernel[j] = -sKernel[j]
            qKernel_indexes[i] = indexes
            qKernel[i] = sKernel
        }
    }
}