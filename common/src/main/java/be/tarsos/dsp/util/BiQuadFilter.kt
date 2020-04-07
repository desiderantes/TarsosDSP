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
package be.tarsos.dsp.util

/**
 * Implements a [BiQuad filter](http://en.wikipedia.org/wiki/Digital_biquad_filter),
 * which can be used for e.g. low pass filtering.
 *
 *
 * The implementation is a translation of biquad.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem></piem>@aubio.org>
 *
 * @author Joren Six
 * @author Paul Brossiers
 */
class BiQuadFilter(
    private val b1: Double,
    private val b2: Double,
    private val b3: Double,
    private val a2: Double,
    private val a3: Double
) {
    private var i1 = 0.0
    private var i2 = 0.0
    private var o1 = 0.0
    private var o2 = 0.0
    fun doFiltering(input: FloatArray, tmp: FloatArray) {
        /* mirroring */
        var mir: Double = 2 * input[0].toDouble()
        i1 = mir - input[2]
        i2 = mir - input[1]
        /* apply filtering */doBiQuad(input)
        /* invert  */for (j in input.indices) {
            tmp[input.size - j - 1] = input[j]
        }
        /* mirror again */mir = 2 * tmp[0].toDouble()
        i1 = mir - tmp[2]
        i2 = mir - tmp[1]
        /* apply filtering */doBiQuad(tmp)
        /* invert back */for (j in input.indices) {
            input[j] = tmp[input.size - j - 1]
        }
    }

    private fun doBiQuad(input: FloatArray) {
        for (j in input.indices) {
            val i0 = input[j].toDouble()
            val o0 = b1 * i0 + b2 * i1 + b3 * i2 - a2 * o1 - a3 * o2
            input[j] = o0.toFloat()
            i2 = i1
            i1 = i0
            o2 = o1
            o1 = o0
        }
    }

}