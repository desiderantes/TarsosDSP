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
/******************************************************************************
 *
 * libresample4j
 * Copyright (c) 2009 Laszlo Systems, Inc. All Rights Reserved.
 *
 * libresample4j is a Java port of Dominic Mazzoni's libresample 0.1.3,
 * which is in turn based on Julius Smith's Resample 1.7 library.
 * http://www-ccrma.stanford.edu/~jos/resample/
 *
 * License: LGPL -- see the file LICENSE.txt for more information
 *
 */
package be.tarsos.dsp.resample

import be.tarsos.dsp.util.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * This file provides Kaiser-windowed low-pass filter support,
 * including a function to create the filter coefficients, and
 * two functions to apply the filter at a particular point.
 *
 * <pre>
 * reference: "Digital Filters, 2nd edition"
 * R.W. Hamming, pp. 178-179
 *
 * Izero() computes the 0th order modified bessel function of the first kind.
 * (Needed to compute Kaiser window).
 *
 * LpFilter() computes the coeffs of a Kaiser-windowed low pass filter with
 * the following characteristics:
 *
 * c[]  = array in which to store computed coeffs
 * frq  = roll-off frequency of filter
 * N    = Half the window length in number of coeffs
 * Beta = parameter of Kaiser window
 * Num  = number of coeffs before 1/frq
 *
 * Beta trades the rejection of the lowpass filter against the transition
 * width from passband to stopband.  Larger Beta means a slower
 * transition and greater stopband rejection.  See Rabiner and Gold
 * (Theory and Application of DSP) under Kaiser windows for more about
 * Beta.  The following table from Rabiner and Gold gives some feel
 * for the effect of Beta:
 *
 * All ripples in dB, width of transition band = D*N where N = window length
 *
 * BETA    D       PB RIP   SB RIP
 * 2.120   1.50  +-0.27      -30
 * 3.384   2.23    0.0864    -40
 * 4.538   2.93    0.0274    -50
 * 5.658   3.62    0.00868   -60
 * 6.764   4.32    0.00275   -70
 * 7.865   5.0     0.000868  -80
 * 8.960   5.7     0.000275  -90
 * 10.056  6.4     0.000087  -100
</pre> *
 */
internal object FilterKit {
    // Max error acceptable in Izero
    private const val IzeroEPSILON = 1E-21
    private fun Izero(x: Double): Double {
        var sum: Double
        var u: Double
        var temp: Double
        var n: Int = 1
        u = n.toDouble()
        sum = u
        val halfx: Double = x / 2.0
        do {
            temp = halfx / n.toDouble()
            n += 1
            temp *= temp
            u *= temp
            sum += u
        } while (u >= IzeroEPSILON * sum)
        return sum
    }

    @JvmStatic
    fun lrsLpFilter(c: DoubleArray, N: Int, frq: Double, Beta: Double, Num: Int) {
        var temp: Double
        var temp1: Double

        // Calculate ideal lowpass filter impulse response coefficients:
        c[0] = 2.0 * frq
        var i: Int = 1
        while (i < N) {
            temp = PI * i.toDouble() / Num.toDouble()
            c[i] = sin(2.0 * temp * frq) / temp // Analog sinc function,
            i++
        }

        /*
         * Calculate and Apply Kaiser window to ideal lowpass filter. Note: last
         * window value is IBeta which is NOT zero. You're supposed to really
         * truncate the window here, not ramp it to zero. This helps reduce the
         * first sidelobe.
         */
        val IBeta: Double = 1.0 / Izero(Beta)
        val inm1: Double = 1.0 / (N - 1).toDouble()
        i = 1
        while (i < N) {
            temp = i.toDouble() * inm1
            temp1 = 1.0 - temp * temp
            temp1 = if (temp1 < 0) 0.0 else temp1 /*
             * make sure it's not negative
             * since we're taking the square
             * root - this happens on Pentium
             * 4's due to tiny roundoff errors
             */
            c[i] *= Izero(Beta * sqrt(temp1)) * IBeta
            i++
        }
    }

    /**
     * @param Imp      impulse response
     * @param ImpD     impulse response deltas
     * @param Nwing    length of one wing of filter
     * @param Interp   Interpolate coefs using deltas?
     * @param Xp_array Current sample array
     * @param Xp_index Current sample index
     * @param Ph       Phase
     * @param Inc      increment (1 for right wing or -1 for left)
     * @return v.
     */
    @JvmStatic
    fun lrsFilterUp(
        Imp: FloatArray,
        ImpD: FloatArray,
        Nwing: Int,
        Interp: Boolean,
        Xp_array: FloatArray,
        Xp_index: Int,
        Ph: Double,
        Inc: Int
    ): Float {
        var Xp_index = Xp_index
        var Ph = Ph
        var a = 0.0
        var t: Float
        Ph *= Resampler.Npc.toDouble() // Npc is number of values per 1/delta in impulse
        // response
        var v: Float = 0.0f // The output value
        var Hp_index = Ph.toInt()
        var End_index = Nwing
        var Hdp_index = Ph.toInt()
        if (Interp) {
            // Hdp = &ImpD[(int)Ph];
            a = Ph - floor(Ph) /* fractional part of Phase */
        }
        // If doing right wing...
        if (Inc == 1) {
            // ...drop extra coeff, so when Ph is
            End_index-- // 0.5, we don't do too many mult's
            if (Ph == 0.0) // If the phase is zero...
            { // ...then we've already skipped the
                Hp_index += Resampler.Npc // first sample, so we must also
                Hdp_index += Resampler.Npc // skip ahead in Imp[] and ImpD[]
            }
        }
        if (Interp) while (Hp_index < End_index) {
            t = Imp[Hp_index] /* Get filter coeff */
            t += ImpD[Hdp_index] * a.toFloat() /* t is now interp'd filter coeff */
            Hdp_index += Resampler.Npc /* Filter coeff differences step */
            t *= Xp_array[Xp_index] /* Mult coeff by input sample */
            v += t /* The filter output */
            Hp_index += Resampler.Npc /* Filter coeff step */
            Xp_index += Inc /* Input signal step. NO CHECK ON BOUNDS */
        } else while (Hp_index < End_index) {
            t = Imp[Hp_index] /* Get filter coeff */
            t *= Xp_array[Xp_index] /* Mult coeff by input sample */
            v += t /* The filter output */
            Hp_index += Resampler.Npc /* Filter coeff step */
            Xp_index += Inc /* Input signal step. NO CHECK ON BOUNDS */
        }
        return v
    }

    /**
     * @param Imp      impulse response
     * @param ImpD     impulse response deltas
     * @param Nwing    length of one wing of filter
     * @param Interp   Interpolate coefs using deltas?
     * @param Xp_array Current sample array
     * @param Xp_index Current sample index
     * @param Ph       Phase
     * @param Inc      increment (1 for right wing or -1 for left)
     * @param dhb      filter sampling period
     * @return v.
     */
    @JvmStatic
    fun lrsFilterUD(
        Imp: FloatArray,
        ImpD: FloatArray,
        Nwing: Int,
        Interp: Boolean,
        Xp_array: FloatArray,
        Xp_index: Int,
        Ph: Double,
        Inc: Int,
        dhb: Double
    ): Float {
        var Xp_index = Xp_index
        var a: Float
        var t: Float
        var v: Float = 0.0f // The output value
        var Ho: Double = Ph * dhb
        var End_index = Nwing
        if (Inc == 1) // If doing right wing...
        { // ...drop extra coeff, so when Ph is
            End_index-- // 0.5, we don't do too many mult's
            if (Ph == 0.0) // If the phase is zero...
                Ho += dhb // ...then we've already skipped the
        } // first sample, so we must also
        // skip ahead in Imp[] and ImpD[]
        var Hp_index: Int
        if (Interp) {
            var Hdp_index: Int
            while ((Ho.also { Hp_index = it.toInt() }) < End_index) {
                t = Imp[Hp_index] // Get IR sample
                Hdp_index = Ho.toInt() // get interp bits from diff table
                a = (Ho - floor(Ho)).toFloat() // a is logically between 0
                // and 1
                t += ImpD[Hdp_index] * a // t is now interp'd filter coeff
                t *= Xp_array[Xp_index] // Mult coeff by input sample
                v += t // The filter output
                Ho += dhb // IR step
                Xp_index += Inc // Input signal step. NO CHECK ON BOUNDS
            }
        } else {
            while ((Ho.also { Hp_index = it.toInt() }) < End_index) {
                t = Imp[Hp_index] // Get IR sample
                t *= Xp_array[Xp_index] // Mult coeff by input sample
                v += t // The filter output
                Ho += dhb // IR step
                Xp_index += Inc // Input signal step. NO CHECK ON BOUNDS
            }
        }
        return v
    }
}