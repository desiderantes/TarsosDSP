package be.tarsos.dsp.util.fft

import be.tarsos.dsp.util.TWO_PI
import kotlin.math.cos

/**
 * @author joren
 * See https://mgasior.web.cern.ch/mgasior/pap/FFT_resol_note.pdf
 */
class BlackmanHarrisNuttall : WindowFunction() {
    var c0 = 0.355768f
    var c1 = 0.487396f
    var c2 = 0.144232f
    var c3 = 0.012604f
    override fun value(length: Int, index: Int): Float {
        var sum = 0f
        sum += c0 * cos(TWO_PI * 0 * index / length.toFloat())
        sum += c1 * cos(TWO_PI * 1 * index / length.toFloat())
        sum += c2 * cos(TWO_PI * 2 * index / length.toFloat())
        sum += c3 * cos(TWO_PI * 3 * index / length.toFloat())
        return sum
    }
}