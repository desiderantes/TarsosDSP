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
package be.tarsos.dsp.onsets

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.log10

/**
 *
 *
 * Estimates the locations of percussive onsets using a simple method described
 * in ["Drum Source Separation using Percussive Feature Detection and Spectral
 * Modulation"](http://arrow.dit.ie/cgi/viewcontent.cgi?article=1018&context=argcon) by Dan Barry, Derry Fitzgerald, Eugene Coyle and Bob Lawlor,
 * ISSC 2005.
 *
 *
 *
 * Implementation based on a [VAMP plugin example](http://vamp-plugins.org/code-doc/PercussionOnsetDetector_8cpp-source.html) by Chris Cannam at Queen Mary, London:
 *
 * <pre>
 * Centre for Digital Music, Queen Mary, University of London.
 * Copyright 2006 Chris Cannam.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the names of the Centre for
 * Digital Music; Queen Mary, University of London; and Chris Cannam
 * shall not be used in advertising or otherwise to promote the sale,
 * use or other dealings in this Software without prior written
 * authorization.
</pre> *
 *
 *
 *
 * @author Joren Six
 * @author Chris Cannam
 */
class PercussionOnsetDetector @JvmOverloads constructor(
    //samples per second (Hz)
    private val sampleRate: Float,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private var handler: OnsetHandler? = null,
    /**
     * Sensitivity of peak detector applied to broadband detection function (%).
     * In [0-100].
     */
    private val sensitivity: Double = DEFAULT_SENSITIVITY,
    /**
     * Energy rise within a frequency bin necessary to count toward broadband
     * total (dB). In [0-20].
     */
    private val threshold: Double = DEFAULT_THRESHOLD
) : AudioProcessor, OnsetDetector {
    private val fft: FFT = FFT(bufferSize / 2)
    private val priorMagnitudes: FloatArray = FloatArray(bufferSize / 2)
    private val currentMagnitudes: FloatArray = FloatArray(bufferSize / 2)

    private var dfMinus1 = 0f
    private var dfMinus2 = 0f
    private var processedSamples //in samples
            : Long = 0

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        processedSamples += audioFloatBuffer.size.toLong()
        processedSamples -= audioEvent.overlap.toLong()
        fft.forwardTransform(audioFloatBuffer)
        fft.modulus(audioFloatBuffer, currentMagnitudes)
        var binsOverThreshold = 0
        for (i in currentMagnitudes.indices) {
            if (priorMagnitudes[i] > 0f) {
                val diff = 10 * log10(
                    (currentMagnitudes[i]
                            / priorMagnitudes[i]).toDouble()
                )
                if (diff >= threshold) {
                    binsOverThreshold++
                }
            }
            priorMagnitudes[i] = currentMagnitudes[i]
        }
        if (dfMinus2 < dfMinus1 && dfMinus1 >= binsOverThreshold && dfMinus1 > (100 - sensitivity) * audioFloatBuffer.size / 200
        ) {
            val timeStamp = processedSamples / sampleRate
            handler!!.handleOnset(timeStamp.toDouble(), -1.0)
        }
        dfMinus2 = dfMinus1
        dfMinus1 = binsOverThreshold.toFloat()
        return true
    }

    override fun processingFinished() {}
    override fun setHandler(handler: OnsetHandler) {
        this.handler = handler
    }

    companion object {
        const val DEFAULT_THRESHOLD = 8.0
        const val DEFAULT_SENSITIVITY = 20.0
    }
}