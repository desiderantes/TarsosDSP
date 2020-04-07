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
 *  Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package be.tarsos.dsp.filters

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

/**
 * An Infinite Impulse Response, or IIR, filter is a filter that uses a set of
 * coefficients and previous filtered values to filter a stream of audio. It is
 * an efficient way to do digital filtering. IIRFilter is a general IIRFilter
 * that simply applies the filter designated by the filter coefficients so that
 * sub-classes only have to dictate what the values of those coefficients are by
 * defining the `calcCoeff()` function. When filling the coefficient
 * arrays, be aware that `b[0]` corresponds to
 * `b<sub>1</sub>`.
 *
 * @author Damien Di Fede
 * @author Joren Six
 */
abstract class IIRFilter(private var frequency: Float, protected val sampleRate: Float) : AudioProcessor {

    /**
     * The b coefficients.
     */
    protected lateinit var b: FloatArray

    /**
     * The a coefficients.
     */
    protected lateinit var a: FloatArray

    /**
     * The input values to the left of the output value currently being
     * calculated.
     */
    protected var inputValues: FloatArray

    /**
     * The previous output values.
     */
    protected var out: FloatArray

    /**
     * Returns the cutoff frequency (in Hz).
     *
     * @return the current cutoff frequency (in Hz).
     */
    protected fun getFrequency(): Float {
        return frequency
    }

    fun setFrequency(freq: Float) {
        frequency = freq
        calcCoeff()
    }

    /**
     * Calculates the coefficients of the filter using the current cutoff
     * frequency. To make your own IIRFilters, you must extend IIRFilter and
     * implement this function. The frequency is expressed as a fraction of the
     * sample rate. When filling the coefficient arrays, be aware that
     * `b[0]` corresponds to the coefficient
     * `b<sub>1</sub>`.
     */
    protected abstract fun calcCoeff()
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        for (i in audioEvent.overlap until audioFloatBuffer.size) {
            //shift the in array
            System.arraycopy(inputValues, 0, inputValues, 1, inputValues.size - 1)
            inputValues[0] = audioFloatBuffer[i]

            //calculate y based on a and b coefficients
            //and in and out.
            var y = 0f
            for (j in a.indices) {
                y += a[j] * inputValues[j]
            }
            for (j in b.indices) {
                y += b[j] * out[j]
            }
            //shift the out array
            System.arraycopy(out, 0, out, 1, out.size - 1)
            out[0] = y
            audioFloatBuffer[i] = y
        }
        return true
    }

    override fun processingFinished() {}

    /**
     * Constructs an IIRFilter with the given cutoff frequency that will be used
     * to filter audio recorded at `sampleRate`.
     *
     * @param freq       the cutoff frequency
     * @param sampleRate the sample rate of audio to be filtered
     */
    init {
        calcCoeff()
        inputValues = FloatArray(a.size)
        out = FloatArray(b.size)
    }
}