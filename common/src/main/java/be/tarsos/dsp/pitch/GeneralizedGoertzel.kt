package be.tarsos.dsp.pitch

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler
import be.tarsos.dsp.util.Complex
import be.tarsos.dsp.util.TWO_PI
import be.tarsos.dsp.util.fft.HammingWindow
import be.tarsos.dsp.util.fft.WindowFunction
import kotlin.math.cos
import kotlin.math.exp

/**
 * [Goertzel algorithm generalized to non-integer multiples of fundamental frequency](http://download.springer.com/static/pdf/14/art%253A10.1186%252F1687-6180-2012-56.pdf?auth66=1409747532_189c92c583694c81b3a0095e2f665c9e&ext=.pdf)
 * Petr Sysel and Pavel Rajmic
 *
 * @author Joren Six
 */
class GeneralizedGoertzel(
    audioSampleRate: Float,
    bufferSize: Int,
    /**
     * A list of frequencies to detect.
     */
    private val frequenciesToDetect: DoubleArray,
    private val handler: FrequenciesDetectedHandler
) : AudioProcessor {
    private val indvec: DoubleArray = DoubleArray(frequenciesToDetect.size) { j ->
        frequenciesToDetect[j] / (audioSampleRate / bufferSize.toFloat())
    }

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
    private val calculatedComplex: Array<Complex?> = arrayOfNulls(frequenciesToDetect.size)
    override fun process(audioEvent: AudioEvent): Boolean {
        val x = audioEvent.floatBuffer
        val f: WindowFunction = HammingWindow()
        f.apply(x)
        for (j in frequenciesToDetect.indices) {
            val pik_term = 2 * Math.PI * indvec[j] / audioEvent.bufferSize.toFloat()
            val cos_pik_term2 = cos(pik_term) * 2
            val cc = Complex(0.0, -1 * pik_term).exp()
            var s0 = 0.0
            var s1 = 0.0
            var s2 = 0.0
            for (i in 0 until audioEvent.bufferSize) {
                s0 = x[i] + cos_pik_term2 * s1 - s2
                s2 = s1
                s1 = s0
            }
            s0 = cos_pik_term2 * s1 - s2
            calculatedComplex[j] = cc.times(Complex(-s1, 0.0)).plus(Complex(s0, 0.0))
            calculatedPowers[j] = calculatedComplex[j]!!.mod()
        }
        handler.handleDetectedFrequencies(
            audioEvent.timeStamp, frequenciesToDetect.clone(), calculatedPowers.clone(),
            frequenciesToDetect.clone(), calculatedPowers.clone()
        )
        return true
    }

    override fun processingFinished() {}

    init {
        for (i in frequenciesToDetect.indices) {
            precalculatedCosines[i] = 2 * cos(
                (TWO_PI
                        * frequenciesToDetect[i]) / audioSampleRate
            )
            precalculatedWnk[i] = exp(
                (TWO_PI
                        * frequenciesToDetect[i]) / audioSampleRate
            )
        }
    }
}