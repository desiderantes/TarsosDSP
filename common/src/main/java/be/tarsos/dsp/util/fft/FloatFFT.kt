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
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package be.tarsos.dsp.util.fft

import be.tarsos.dsp.util.ConcurrencyUtils
import be.tarsos.dsp.util.PI
import be.tarsos.dsp.util.TWO_PI
import java.util.concurrent.Future
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

/**
 * Computes 1D Discrete Fourier Transform (DFT) of complex and real, single
 * precision data. The size of the data can be an arbitrary number. This is a
 * parallel implementation of split-radix and mixed-radix algorithms optimized
 * for SMP systems. <br></br>
 * <br></br>
 * This code is derived from General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html) and from JFFTPack written
 * by Baoshe Zhang (http://jfftpack.sourceforge.net/)
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
@Strictfp
class FloatFFT(private val n: Int) {
    private var nBluestein = 0
    private val ip: IntArray
    private val w: FloatArray
    private var nw = 0
    private var nc = 0
    private val wtable: FloatArray
    private val wtableR: FloatArray
    private val bk1: FloatArray
    private val bk2: FloatArray
    private var plan: Plans? = null
    /**
     * Computes 1D forward DFT of complex data leaving the result in
     * `a`. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br></br>
     *
     * <pre>
     * a[offa+2*k] = Re[k],
     * a[offa+2*k+1] = Im[k], 0&lt;=k&lt;n
    </pre> *
     *
     * @param a    data to transform
     * @param offa index of the first element in array `a`
     */
    /**
     * Computes 1D forward DFT of complex data leaving the result in
     * `a`. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br></br>
     *
     * <pre>
     * a[2*k] = Re[k],
     * a[2*k+1] = Im[k], 0&lt;=k&lt;n
    </pre> *
     *
     * @param a data to transform
     */
    @JvmOverloads
    fun complexForward(a: FloatArray, offa: Int = 0) {
        if (n == 1) return
        when (plan) {
            Plans.SPLIT_RADIX -> cftbsub(2 * n, a, offa, ip, nw, w)
            Plans.MIXED_RADIX -> cfftf(a, offa, -1)
            Plans.BLUESTEIN -> bluestein_complex(a, offa, -1)
        }
    }

    /**
     * Computes 1D inverse DFT of complex data leaving the result in
     * `a`. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br></br>
     *
     * <pre>
     * a[2*k] = Re[k],
     * a[2*k+1] = Im[k], 0&lt;=k&lt;n
    </pre> *
     *
     * @param a     data to transform
     * @param scale if true then scaling is performed
     */
    fun complexInverse(a: FloatArray, scale: Boolean) {
        complexInverse(a, 0, scale)
    }

    /**
     * Computes 1D inverse DFT of complex data leaving the result in
     * `a`. Complex number is stored as two float values in
     * sequence: the real and imaginary part, i.e. the size of the input array
     * must be greater or equal 2*n. The physical layout of the input data has
     * to be as follows:<br></br>
     *
     * <pre>
     * a[offa+2*k] = Re[k],
     * a[offa+2*k+1] = Im[k], 0&lt;=k&lt;n
    </pre> *
     *
     * @param a     data to transform
     * @param offa  index of the first element in array `a`
     * @param scale if true then scaling is performed
     */
    fun complexInverse(a: FloatArray, offa: Int, scale: Boolean) {
        if (n == 1) return
        when (plan) {
            Plans.SPLIT_RADIX -> cftfsub(2 * n, a, offa, ip, nw, w)
            Plans.MIXED_RADIX -> cfftf(a, offa, +1)
            Plans.BLUESTEIN -> bluestein_complex(a, offa, 1)
        }
        if (scale) {
            scale(n.toFloat(), a, offa, true)
        }
    }
    /**
     * Computes 1D forward DFT of real data leaving the result in `a`
     * . The physical layout of the output data is as follows:<br></br>
     *
     *
     * if n is even then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[offa+1] = Re[n/2]
    </pre> *
     *
     *
     * if n is odd then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[offa+1] = Im[(n-1)/2]
    </pre> *
     *
     *
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use `realForwardFull`. To get back the
     * original data, use `realInverse` on the output of this method.
     *
     * @param a    data to transform
     * @param offa index of the first element in array `a`
     */
    /**
     * Computes 1D forward DFT of real data leaving the result in `a`
     * . The physical layout of the output data is as follows:<br></br>
     *
     *
     * if n is even then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[1] = Re[n/2]
    </pre> *
     *
     *
     * if n is odd then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[1] = Im[(n-1)/2]
    </pre> *
     *
     *
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use `realForwardFull`. To get back the
     * original data, use `realInverse` on the output of this method.
     *
     * @param a data to transform
     */
    @JvmOverloads
    fun realForward(a: FloatArray, offa: Int = 0) {
        if (n == 1) return
        when (plan) {
            Plans.SPLIT_RADIX -> {
                if (n > 4) {
                    cftfsub(n, a, offa, ip, nw, w)
                    rftfsub(n, a, offa, nc, w, nw)
                } else if (n == 4) {
                    cftx020(a, offa)
                }
                val xi: Float = a[offa] - a[offa + 1]
                a[offa] += a[offa + 1]
                a[offa + 1] = xi
            }
            Plans.MIXED_RADIX -> {
                rfftf(a, offa)
                var k = n - 1
                while (k >= 2) {
                    val idx = offa + k
                    val tmp = a[idx]
                    a[idx] = a[idx - 1]
                    a[idx - 1] = tmp
                    k--
                }
            }
            Plans.BLUESTEIN -> bluestein_real_forward(a, offa)
        }
    }
    /**
     * Computes 1D forward DFT of real data leaving the result in `a`
     * . This method computes the full real forward transform, i.e. you will get
     * the same result as from `complexForward` called with all
     * imaginary part equal 0. Because the result is stored in `a`,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data. To get back the original data,
     * use `complexInverse` on the output of this method.
     *
     * @param a    data to transform
     * @param offa index of the first element in array `a`
     */
    /**
     * Computes 1D forward DFT of real data leaving the result in `a`
     * . This method computes the full real forward transform, i.e. you will get
     * the same result as from `complexForward` called with all
     * imaginary parts equal 0. Because the result is stored in `a`,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data. To get back the original data,
     * use `complexInverse` on the output of this method.
     *
     * @param a data to transform
     */
    @JvmOverloads
    fun realForwardFull(a: FloatArray, offa: Int = 0) {
        val twon = 2 * n
        when (plan) {
            Plans.SPLIT_RADIX -> {
                realForward(a, offa)
                val nthreads = ConcurrencyUtils.getNumberOfThreads()
                if (nthreads > 1 && n / 2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {

                    val k = n / 2 / nthreads
                    var i = 0
                    val futures = Array<Future<*>>(nthreads) { i ->
                        val firstIdx = i * k
                        val lastIdx = if (i == nthreads - 1) n / 2 else firstIdx + k
                        return@Array ConcurrencyUtils.submit {
                            var idx1: Int
                            var idx2: Int
                            var k1 = firstIdx
                            while (k1 < lastIdx) {
                                idx1 = 2 * k1
                                idx2 = offa + (twon - idx1) % twon
                                a[idx2] = a[offa + idx1]
                                a[idx2 + 1] = -a[offa + idx1 + 1]
                                k1++
                            }
                        }
                    }
                    ConcurrencyUtils.waitForCompletion(futures)
                } else {
                    var idx1: Int
                    var idx2: Int
                    var k = 0
                    while (k < n / 2) {
                        idx1 = 2 * k
                        idx2 = offa + (twon - idx1) % twon
                        a[idx2] = a[offa + idx1]
                        a[idx2 + 1] = -a[offa + idx1 + 1]
                        k++
                    }
                }
                a[offa + n] = -a[offa + 1]
                a[offa + 1] = 0F
            }
            Plans.MIXED_RADIX -> {
                rfftf(a, offa)
                val m: Int = if (n % 2 == 0) {
                    n / 2
                } else {
                    (n + 1) / 2
                }
                run {
                    var k = 1
                    while (k < m) {
                        val idx1 = offa + twon - 2 * k
                        val idx2 = offa + 2 * k
                        a[idx1 + 1] = -a[idx2]
                        a[idx1] = a[idx2 - 1]
                        k++
                    }
                }
                var k = 1
                while (k < n) {
                    val idx = offa + n - k
                    val tmp = a[idx + 1]
                    a[idx + 1] = a[idx]
                    a[idx] = tmp
                    k++
                }
                a[offa + 1] = 0F
            }
            Plans.BLUESTEIN -> bluestein_real_full(a, offa, -1)
        }
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in `a`
     * . The physical layout of the input data has to be as follows:<br></br>
     *
     *
     * if n is even then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[1] = Re[n/2]
    </pre> *
     *
     *
     * if n is odd then
     *
     * <pre>
     * a[2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[1] = Im[(n-1)/2]
    </pre> *
     *
     *
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use `realInverseFull`.
     *
     * @param a     data to transform
     * @param scale if true then scaling is performed
     */
    fun realInverse(a: FloatArray, scale: Boolean) {
        realInverse(a, 0, scale)
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in `a`
     * . The physical layout of the input data has to be as follows:<br></br>
     *
     *
     * if n is even then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;n/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;n/2
     * a[offa+1] = Re[n/2]
    </pre> *
     *
     *
     * if n is odd then
     *
     * <pre>
     * a[offa+2*k] = Re[k], 0&lt;=k&lt;(n+1)/2
     * a[offa+2*k+1] = Im[k], 0&lt;k&lt;(n-1)/2
     * a[offa+1] = Im[(n-1)/2]
    </pre> *
     *
     *
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use `realInverseFull`.
     *
     * @param a     data to transform
     * @param offa  index of the first element in array `a`
     * @param scale if true then scaling is performed
     */
    fun realInverse(a: FloatArray, offa: Int, scale: Boolean) {
        if (n == 1) return
        when (plan) {
            Plans.SPLIT_RADIX -> {
                a[offa + 1] = (0.5 * (a[offa] - a[offa + 1])).toFloat()
                a[offa] -= a[offa + 1]
                if (n > 4) {
                    rftfsub(n, a, offa, nc, w, nw)
                    cftbsub(n, a, offa, ip, nw, w)
                } else if (n == 4) {
                    cftxc020(a, offa)
                }
                if (scale) {
                    scale(n / 2.toFloat(), a, offa, false)
                }
            }
            Plans.MIXED_RADIX -> {
                var k = 2
                while (k < n) {
                    val idx = offa + k
                    val tmp = a[idx - 1]
                    a[idx - 1] = a[idx]
                    a[idx] = tmp
                    k++
                }
                rfftb(a, offa)
                if (scale) {
                    scale(n.toFloat(), a, offa, false)
                }
            }
            Plans.BLUESTEIN -> {
                bluestein_real_inverse(a, offa)
                if (scale) {
                    scale(n.toFloat(), a, offa, false)
                }
            }
        }
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in `a`
     * . This method computes the full real inverse transform, i.e. you will get
     * the same result as from `complexInverse` called with all
     * imaginary part equal 0. Because the result is stored in `a`,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data.
     *
     * @param a     data to transform
     * @param scale if true then scaling is performed
     */
    fun realInverseFull(a: FloatArray, scale: Boolean) {
        realInverseFull(a, 0, scale)
    }

    /**
     * Computes 1D inverse DFT of real data leaving the result in `a`
     * . This method computes the full real inverse transform, i.e. you will get
     * the same result as from `complexInverse` called with all
     * imaginary part equal 0. Because the result is stored in `a`,
     * the size of the input array must greater or equal 2*n, with only the
     * first n elements filled with real data.
     *
     * @param a     data to transform
     * @param offa  index of the first element in array `a`
     * @param scale if true then scaling is performed
     */
    fun realInverseFull(a: FloatArray, offa: Int, scale: Boolean) {
        val twon = 2 * n
        when (plan) {
            Plans.SPLIT_RADIX -> {
                realInverse2(a, offa, scale)
                val nthreads = ConcurrencyUtils.getNumberOfThreads()
                if (nthreads > 1 && n / 2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {

                    val k = n / 2 / nthreads
                    var i = 0
                    val futures = Array<Future<*>>(nthreads) { i ->
                        val firstIdx = i * k
                        val lastIdx = if (i == nthreads - 1) n / 2 else firstIdx + k
                        return@Array ConcurrencyUtils.submit {
                            var idx1: Int
                            var idx2: Int
                            var k1 = firstIdx
                            while (k1 < lastIdx) {
                                idx1 = 2 * k1
                                idx2 = offa + (twon - idx1) % twon
                                a[idx2] = a[offa + idx1]
                                a[idx2 + 1] = -a[offa + idx1 + 1]
                                k1++
                            }
                        }
                    }
                    ConcurrencyUtils.waitForCompletion(futures)
                } else {
                    var idx1: Int
                    var idx2: Int
                    var k = 0
                    while (k < n / 2) {
                        idx1 = 2 * k
                        idx2 = offa + (twon - idx1) % twon
                        a[idx2] = a[offa + idx1]
                        a[idx2 + 1] = -a[offa + idx1 + 1]
                        k++
                    }
                }
                a[offa + n] = -a[offa + 1]
                a[offa + 1] = 0F
            }
            Plans.MIXED_RADIX -> {
                rfftf(a, offa)
                if (scale) {
                    scale(n.toFloat(), a, offa, false)
                }
                val m: Int = if (n % 2 == 0) {
                    n / 2
                } else {
                    (n + 1) / 2
                }
                run {
                    var k = 1
                    while (k < m) {
                        val idx1 = offa + 2 * k
                        val idx2 = offa + twon - 2 * k
                        a[idx1] = -a[idx1]
                        a[idx2 + 1] = -a[idx1]
                        a[idx2] = a[idx1 - 1]
                        k++
                    }
                }
                var k = 1
                while (k < n) {
                    val idx = offa + n - k
                    val tmp = a[idx + 1]
                    a[idx + 1] = a[idx]
                    a[idx] = tmp
                    k++
                }
                a[offa + 1] = 0F
            }
            Plans.BLUESTEIN -> {
                bluestein_real_full(a, offa, 1)
                if (scale) {
                    scale(n.toFloat(), a, offa, true)
                }
            }
        }
    }

    protected fun realInverse2(a: FloatArray, offa: Int, scale: Boolean) {
        if (n == 1) return
        when (plan) {
            Plans.SPLIT_RADIX -> {
                if (n > 4) {
                    cftfsub(n, a, offa, ip, nw, w)
                    rftbsub(n, a, offa, nc, w, nw)
                } else if (n == 4) {
                    cftbsub(n, a, offa, ip, nw, w)
                }
                val xi: Float = a[offa] - a[offa + 1]
                a[offa] += a[offa + 1]
                a[offa + 1] = xi
                if (scale) {
                    scale(n.toFloat(), a, offa, false)
                }
            }
            Plans.MIXED_RADIX -> {
                rfftf(a, offa)
                var k = n - 1
                while (k >= 2) {
                    val idx = offa + k
                    val tmp = a[idx]
                    a[idx] = a[idx - 1]
                    a[idx - 1] = tmp
                    k--
                }
                if (scale) {
                    scale(n.toFloat(), a, offa, false)
                }
                val m: Int
                if (n % 2 == 0) {
                    m = n / 2
                    var i = 1
                    while (i < m) {
                        val idx = offa + 2 * i + 1
                        a[idx] = -a[idx]
                        i++
                    }
                } else {
                    m = (n - 1) / 2
                    var i = 0
                    while (i < m) {
                        val idx = offa + 2 * i + 1
                        a[idx] = -a[idx]
                        i++
                    }
                }
            }
            Plans.BLUESTEIN -> {
                bluestein_real_inverse2(a, offa)
                if (scale) {
                    scale(n.toFloat(), a, offa, false)
                }
            }
        }
    }

    fun cffti(n: Int, offw: Int) {
        if (n == 1) return
        val twon = 2 * n
        val fourn = 4 * n
        val argh: Float
        var idot: Int
        var ntry = 0
        var i: Int
        var j: Int
        var argld: Float
        var i1: Int
        var l1: Int
        var l2: Int
        var ib: Int
        var fi: Float
        var ld: Int
        var ii: Int
        var ip: Int
        var nl: Int
        var nq: Int
        var nr: Int
        var arg: Float
        var ido: Int
        var ipm: Int
        nl = n
        var nf: Int = 0
        j = 0
        factorize_loop@ while (true) {
            j++
            if (j <= 4) ntry = DEFAULT_FACTORS[j - 1] else ntry += 2
            do {
                nq = nl / ntry
                nr = nl - ntry * nq
                if (nr != 0) continue@factorize_loop
                nf++
                wtable[offw + nf + 1 + fourn] = ntry.toFloat()
                nl = nq
                if (ntry == 2 && nf != 1) {
                    i = 2
                    while (i <= nf) {
                        ib = nf - i + 2
                        val idx = ib + fourn
                        wtable[offw + idx + 1] = wtable[offw + idx]
                        i++
                    }
                    wtable[offw + 2 + fourn] = 2F
                }
            } while (nl != 1)
            break@factorize_loop
        }
        wtable[offw + fourn] = n.toFloat()
        wtable[offw + 1 + fourn] = nf.toFloat()
        argh = TWO_PI / n.toFloat()
        i = 1
        l1 = 1
        var k1: Int = 1
        while (k1 <= nf) {
            ip = wtable[offw + k1 + 1 + fourn].toInt()
            ld = 0
            l2 = l1 * ip
            ido = n / l2
            idot = ido + ido + 2
            ipm = ip - 1
            j = 1
            while (j <= ipm) {
                i1 = i
                wtable[offw + i - 1 + twon] = 1F
                wtable[offw + i + twon] = 0F
                ld += l1
                fi = 0f
                argld = ld * argh
                ii = 4
                while (ii <= idot) {
                    i += 2
                    fi += 1f
                    arg = fi * argld
                    val idx = i + twon
                    wtable[offw + idx - 1] = cos(arg.toDouble()).toFloat()
                    wtable[offw + idx] = sin(arg.toDouble()).toFloat()
                    ii += 2
                }
                if (ip > 5) {
                    val idx1 = i1 + twon
                    val idx2 = i + twon
                    wtable[offw + idx1 - 1] = wtable[offw + idx2 - 1]
                    wtable[offw + idx1] = wtable[offw + idx2]
                }
                j++
            }
            l1 = l2
            k1++
        }
    }

    /* -------- initializing routines -------- */ /*---------------------------------------------------------
       cffti: initialization of Complex FFT
      --------------------------------------------------------*/
    fun cffti() {
        if (n == 1) return
        val twon = 2 * n
        val fourn = 4 * n
        var idot: Int
        var ntry = 0
        var i: Int
        var j: Int
        var argld: Float
        var i1: Int
        var l1: Int
        var l2: Int
        var ib: Int
        var fi: Float
        var ld: Int
        var ii: Int
        var ip: Int
        var nl: Int
        var nq: Int
        var nr: Int
        var arg: Float
        var ido: Int
        var ipm: Int
        nl = n
        var nf: Int = 0
        j = 0
        factorize_loop@ while (true) {
            j++
            if (j <= 4) ntry = DEFAULT_FACTORS[j - 1] else ntry += 2
            do {
                nq = nl / ntry
                nr = nl - ntry * nq
                if (nr != 0) continue@factorize_loop
                nf++
                wtable[nf + 1 + fourn] = ntry.toFloat()
                nl = nq
                if (ntry == 2 && nf != 1) {
                    i = 2
                    while (i <= nf) {
                        ib = nf - i + 2
                        val idx = ib + fourn
                        wtable[idx + 1] = wtable[idx]
                        i++
                    }
                    wtable[2 + fourn] = 2F
                }
            } while (nl != 1)
            break@factorize_loop
        }
        wtable[fourn] = n.toFloat()
        wtable[1 + fourn] = nf.toFloat()
        val argh: Float = TWO_PI / n.toFloat()
        i = 1
        l1 = 1
        var k1: Int = 1
        while (k1 <= nf) {
            ip = wtable[k1 + 1 + fourn].toInt()
            ld = 0
            l2 = l1 * ip
            ido = n / l2
            idot = ido + ido + 2
            ipm = ip - 1
            j = 1
            while (j <= ipm) {
                i1 = i
                wtable[i - 1 + twon] = 1F
                wtable[i + twon] = 0F
                ld += l1
                fi = 0f
                argld = ld * argh
                ii = 4
                while (ii <= idot) {
                    i += 2
                    fi += 1f
                    arg = fi * argld
                    val idx = i + twon
                    wtable[idx - 1] = cos(arg.toDouble()).toFloat()
                    wtable[idx] = sin(arg.toDouble()).toFloat()
                    ii += 2
                }
                if (ip > 5) {
                    val idx1 = i1 + twon
                    val idx2 = i + twon
                    wtable[idx1 - 1] = wtable[idx2 - 1]
                    wtable[idx1] = wtable[idx2]
                }
                j++
            }
            l1 = l2
            k1++
        }
    }

    fun rffti() {
        if (n == 1) return
        val twon = 2 * n
        val argh: Float
        var ntry = 0
        var i: Int
        var j: Int
        var argld: Float
        var l1: Int
        var l2: Int
        var ib: Int
        var fi: Float
        var ld: Int
        var ii: Int
        var ip: Int
        var nl: Int
        var nq: Int
        var nr: Int
        var arg: Float
        var ido: Int
        var ipm: Int
        val nfm1: Int
        nl = n
        var nf: Int = 0
        j = 0
        factorize_loop@ while (true) {
            ++j
            if (j <= 4) ntry = DEFAULT_FACTORS[j - 1] else ntry += 2
            do {
                nq = nl / ntry
                nr = nl - ntry * nq
                if (nr != 0) continue@factorize_loop
                ++nf
                wtableR[nf + 1 + twon] = ntry.toFloat()
                nl = nq
                if (ntry == 2 && nf != 1) {
                    i = 2
                    while (i <= nf) {
                        ib = nf - i + 2
                        val idx = ib + twon
                        wtableR[idx + 1] = wtableR[idx]
                        i++
                    }
                    wtableR[2 + twon] = 2F
                }
            } while (nl != 1)
            break@factorize_loop
        }
        wtableR[twon] = n.toFloat()
        wtableR[1 + twon] = nf.toFloat()
        argh = TWO_PI / n.toFloat()
        var `is`: Int = 0
        nfm1 = nf - 1
        l1 = 1
        if (nfm1 == 0) return
        var k1: Int = 1
        while (k1 <= nfm1) {
            ip = wtableR[k1 + 1 + twon].toInt()
            ld = 0
            l2 = l1 * ip
            ido = n / l2
            ipm = ip - 1
            j = 1
            while (j <= ipm) {
                ld += l1
                i = `is`
                argld = ld.toFloat() * argh
                fi = 0f
                ii = 3
                while (ii <= ido) {
                    i += 2
                    fi += 1f
                    arg = fi * argld
                    val idx = i + n
                    wtableR[idx - 2] = cos(arg.toDouble()).toFloat()
                    wtableR[idx - 1] = sin(arg.toDouble()).toFloat()
                    ii += 2
                }
                `is` += ido
                ++j
            }
            l1 = l2
            k1++
        }
    }

    private fun bluesteini() {
        var k = 0
        var arg: Float
        val pi_n = PI / n
        bk1[0] = 1F
        bk1[1] = 0F
        for (i in 1 until n) {
            k += 2 * i - 1
            if (k >= 2 * n) k -= 2 * n
            arg = pi_n * k
            bk1[2 * i] = cos(arg.toDouble()).toFloat()
            bk1[2 * i + 1] = sin(arg.toDouble()).toFloat()
        }
        val scale = (1.0 / nBluestein).toFloat()
        bk2[0] = bk1[0] * scale
        bk2[1] = bk1[1] * scale
        var i = 2
        while (i < 2 * n) {
            bk2[i] = bk1[i] * scale
            bk2[i + 1] = bk1[i + 1] * scale
            bk2[2 * nBluestein - i] = bk2[i]
            bk2[2 * nBluestein - i + 1] = bk2[i + 1]
            i += 2
        }
        cftbsub(2 * nBluestein, bk2, 0, ip, nw, w)
    }

    private fun makewt(nw: Int) {
        var j: Int
        var nwh: Int
        var nw0: Int
        var nw1: Int
        val delta: Float
        val wn4r: Float
        var wk1r: Float
        var wk1i: Float
        var wk3r: Float
        var wk3i: Float
        val delta2: Float
        var deltaj: Float
        var deltaj3: Float
        ip[0] = nw
        ip[1] = 1
        if (nw > 2) {
            nwh = nw shr 1
            delta = (0.785398163397448278999490867136046290 / nwh).toFloat()
            delta2 = delta * 2
            wn4r = cos(delta * nwh.toDouble()).toFloat()
            w[0] = 1F
            w[1] = wn4r
            if (nwh == 4) {
                w[2] = cos(delta2.toDouble()).toFloat()
                w[3] = sin(delta2.toDouble()).toFloat()
            } else if (nwh > 4) {
                makeipt(nw)
                w[2] = (0.5 / cos(delta2.toDouble())).toFloat()
                w[3] = (0.5 / cos(delta * 6.toDouble())).toFloat()
                j = 4
                while (j < nwh) {
                    deltaj = delta * j
                    deltaj3 = 3 * deltaj
                    w[j] = cos(deltaj.toDouble()).toFloat()
                    w[j + 1] = sin(deltaj.toDouble()).toFloat()
                    w[j + 2] = cos(deltaj3.toDouble()).toFloat()
                    w[j + 3] = (-sin(deltaj3.toDouble())).toFloat()
                    j += 4
                }
            }
            nw0 = 0
            while (nwh > 2) {
                nw1 = nw0 + nwh
                nwh = nwh shr 1
                w[nw1] = 1F
                w[nw1 + 1] = wn4r
                if (nwh == 4) {
                    wk1r = w[nw0 + 4]
                    wk1i = w[nw0 + 5]
                    w[nw1 + 2] = wk1r
                    w[nw1 + 3] = wk1i
                } else if (nwh > 4) {
                    wk1r = w[nw0 + 4]
                    wk3r = w[nw0 + 6]
                    w[nw1 + 2] = (0.5 / wk1r).toFloat()
                    w[nw1 + 3] = (0.5 / wk3r).toFloat()
                    j = 4
                    while (j < nwh) {
                        val idx1 = nw0 + 2 * j
                        val idx2 = nw1 + j
                        wk1r = w[idx1]
                        wk1i = w[idx1 + 1]
                        wk3r = w[idx1 + 2]
                        wk3i = w[idx1 + 3]
                        w[idx2] = wk1r
                        w[idx2 + 1] = wk1i
                        w[idx2 + 2] = wk3r
                        w[idx2 + 3] = wk3i
                        j += 4
                    }
                }
                nw0 = nw1
            }
        }
    }

    private fun makeipt(nw: Int) {
        var j: Int
        var l: Int
        var m: Int
        var m2: Int
        var p: Int
        var q: Int
        ip[2] = 0
        ip[3] = 16
        m = 2
        l = nw
        while (l > 32) {
            m2 = m shl 1
            q = m2 shl 3
            j = m
            while (j < m2) {
                p = ip[j] shl 2
                ip[m + j] = p
                ip[m2 + j] = p + q
                j++
            }
            m = m2
            l = l shr 2
        }
    }

    private fun makect(nc: Int, c: FloatArray, startc: Int) {
        var j: Int
        val nch: Int
        val delta: Float
        var deltaj: Float
        ip[1] = nc
        if (nc > 1) {
            nch = nc shr 1
            delta = (0.785398163397448278999490867136046290 / nch).toFloat()
            c[startc] = cos(delta * nch.toDouble()).toFloat()
            c[startc + nch] = (0.5 * c[startc]).toFloat()
            j = 1
            while (j < nch) {
                deltaj = delta * j
                c[startc + j] = (0.5 * cos(deltaj.toDouble())).toFloat()
                c[startc + nc - j] = (0.5 * sin(deltaj.toDouble())).toFloat()
                j++
            }
        }
    }

    private fun bluestein_complex(a: FloatArray, offa: Int, isign: Int) {
        val ak = FloatArray(2 * nBluestein)
        var nthreads = ConcurrencyUtils.getNumberOfThreads()
        if (nthreads > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2
            if (nthreads >= 4 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
                nthreads = 4
            }
            var k = n / nthreads
            val futures = Array<Future<*>>(nthreads) { i ->
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                return@Array ConcurrencyUtils.submit {
                    if (isign > 0) {
                        for (i13 in firstIdx until lastIdx) {
                            val idx1 = 2 * i13
                            val idx2 = idx1 + 1
                            val idx3 = offa + idx1
                            val idx4 = offa + idx2
                            ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2]
                            ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1]
                        }
                    } else {
                        for (i13 in firstIdx until lastIdx) {
                            val idx1 = 2 * i13
                            val idx2 = idx1 + 1
                            val idx3 = offa + idx1
                            val idx4 = offa + idx2
                            ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2]
                            ak[idx2] = -a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1]
                        }
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = nBluestein / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) nBluestein else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    if (isign > 0) {
                        for (i12 in firstIdx until lastIdx) {
                            val idx1 = 2 * i12
                            val idx2 = idx1 + 1
                            val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                            ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                            ak[idx2] = im
                        }
                    } else {
                        for (i12 in firstIdx until lastIdx) {
                            val idx1 = 2 * i12
                            val idx2 = idx1 + 1
                            val im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                            ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2]
                            ak[idx2] = im
                        }
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = n / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    if (isign > 0) {
                        for (i1 in firstIdx until lastIdx) {
                            val idx1 = 2 * i1
                            val idx2 = idx1 + 1
                            val idx3 = offa + idx1
                            val idx4 = offa + idx2
                            a[idx3] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                            a[idx4] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                        }
                    } else {
                        for (i1 in firstIdx until lastIdx) {
                            val idx1 = 2 * i1
                            val idx2 = idx1 + 1
                            val idx3 = offa + idx1
                            val idx4 = offa + idx2
                            a[idx3] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2]
                            a[idx4] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                        }
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
        } else {
            if (isign > 0) {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val idx3 = offa + idx1
                    val idx4 = offa + idx2
                    ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2]
                    ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1]
                }
            } else {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val idx3 = offa + idx1
                    val idx4 = offa + idx2
                    ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2]
                    ak[idx2] = -a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1]
                }
            }
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            if (isign > 0) {
                for (i in 0 until nBluestein) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                    ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                    ak[idx2] = im
                }
            } else {
                for (i in 0 until nBluestein) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                    ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2]
                    ak[idx2] = im
                }
            }
            cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
            if (isign > 0) {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val idx3 = offa + idx1
                    val idx4 = offa + idx2
                    a[idx3] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                    a[idx4] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                }
            } else {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val idx3 = offa + idx1
                    val idx4 = offa + idx2
                    a[idx3] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2]
                    a[idx4] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                }
            }
        }
    }

    private fun bluestein_real_full(a: FloatArray, offa: Int, isign: Int) {
        val ak = FloatArray(2 * nBluestein)
        var nthreads = ConcurrencyUtils.getNumberOfThreads()
        if (nthreads > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2
            if (nthreads >= 4 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
                nthreads = 4
            }
            var k = n / nthreads
            val futures = Array<Future<*>>(nthreads) { i ->
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                return@Array ConcurrencyUtils.submit {
                    if (isign > 0) {
                        for (i13 in firstIdx until lastIdx) {
                            val idx1 = 2 * i13
                            val idx2 = idx1 + 1
                            val idx3 = offa + i13
                            ak[idx1] = a[idx3] * bk1[idx1]
                            ak[idx2] = a[idx3] * bk1[idx2]
                        }
                    } else {
                        for (i13 in firstIdx until lastIdx) {
                            val idx1 = 2 * i13
                            val idx2 = idx1 + 1
                            val idx3 = offa + i13
                            ak[idx1] = a[idx3] * bk1[idx1]
                            ak[idx2] = -a[idx3] * bk1[idx2]
                        }
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = nBluestein / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) nBluestein else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    if (isign > 0) {
                        for (i12 in firstIdx until lastIdx) {
                            val idx1 = 2 * i12
                            val idx2 = idx1 + 1
                            val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                            ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                            ak[idx2] = im
                        }
                    } else {
                        for (i12 in firstIdx until lastIdx) {
                            val idx1 = 2 * i12
                            val idx2 = idx1 + 1
                            val im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                            ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2]
                            ak[idx2] = im
                        }
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = n / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    if (isign > 0) {
                        for (i1 in firstIdx until lastIdx) {
                            val idx1 = 2 * i1
                            val idx2 = idx1 + 1
                            a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                            a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                        }
                    } else {
                        for (i1 in firstIdx until lastIdx) {
                            val idx1 = 2 * i1
                            val idx2 = idx1 + 1
                            a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2]
                            a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                        }
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
        } else {
            if (isign > 0) {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val idx3 = offa + i
                    ak[idx1] = a[idx3] * bk1[idx1]
                    ak[idx2] = a[idx3] * bk1[idx2]
                }
            } else {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val idx3 = offa + i
                    ak[idx1] = a[idx3] * bk1[idx1]
                    ak[idx2] = -a[idx3] * bk1[idx2]
                }
            }
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            if (isign > 0) {
                for (i in 0 until nBluestein) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                    ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                    ak[idx2] = im
                }
            } else {
                for (i in 0 until nBluestein) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    val im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                    ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2]
                    ak[idx2] = im
                }
            }
            cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
            if (isign > 0) {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                    a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                }
            } else {
                for (i in 0 until n) {
                    val idx1 = 2 * i
                    val idx2 = idx1 + 1
                    a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2]
                    a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
                }
            }
        }
    }

    private fun bluestein_real_forward(a: FloatArray, offa: Int) {
        val ak = FloatArray(2 * nBluestein)
        var nthreads = ConcurrencyUtils.getNumberOfThreads()
        if (nthreads > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2
            if (nthreads >= 4 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
                nthreads = 4
            }

            var k = n / nthreads
            val futures = Array<Future<*>>(nthreads) { i ->
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                return@Array ConcurrencyUtils.submit {
                    for (i12 in firstIdx until lastIdx) {
                        val idx1 = 2 * i12
                        val idx2 = idx1 + 1
                        val idx3 = offa + i12
                        ak[idx1] = a[idx3] * bk1[idx1]
                        ak[idx2] = -a[idx3] * bk1[idx2]
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = nBluestein / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) nBluestein else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    for (i1 in firstIdx until lastIdx) {
                        val idx1 = 2 * i1
                        val idx2 = idx1 + 1
                        val im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                        ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2]
                        ak[idx2] = im
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
        } else {
            for (i in 0 until n) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val idx3 = offa + i
                ak[idx1] = a[idx3] * bk1[idx1]
                ak[idx2] = -a[idx3] * bk1[idx2]
            }
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            for (i in 0 until nBluestein) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val im = ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                ak[idx1] = ak[idx1] * bk2[idx1] - ak[idx2] * bk2[idx2]
                ak[idx2] = im
            }
        }
        cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
        if (n % 2 == 0) {
            a[offa] = bk1[0] * ak[0] + bk1[1] * ak[1]
            a[offa + 1] = bk1[n] * ak[n] + bk1[n + 1] * ak[n + 1]
            for (i in 1 until n / 2) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2]
                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
            }
        } else {
            a[offa] = bk1[0] * ak[0] + bk1[1] * ak[1]
            a[offa + 1] = -bk1[n] * ak[n - 1] + bk1[n - 1] * ak[n]
            for (i in 1 until (n - 1) / 2) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                a[offa + idx1] = bk1[idx1] * ak[idx1] + bk1[idx2] * ak[idx2]
                a[offa + idx2] = -bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
            }
            a[offa + n - 1] = bk1[n - 1] * ak[n - 1] + bk1[n] * ak[n]
        }
    }

    private fun bluestein_real_inverse(a: FloatArray, offa: Int) {
        val ak = FloatArray(2 * nBluestein)
        if (n % 2 == 0) {
            ak[0] = a[offa] * bk1[0]
            ak[1] = a[offa] * bk1[1]
            for (i in 1 until n / 2) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val idx3 = offa + idx1
                val idx4 = offa + idx2
                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2]
                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1]
            }
            ak[n] = a[offa + 1] * bk1[n]
            ak[n + 1] = a[offa + 1] * bk1[n + 1]
            for (i in n / 2 + 1 until n) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val idx3 = offa + 2 * n - idx1
                val idx4 = idx3 + 1
                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2]
                ak[idx2] = a[idx3] * bk1[idx2] - a[idx4] * bk1[idx1]
            }
        } else {
            ak[0] = a[offa] * bk1[0]
            ak[1] = a[offa] * bk1[1]
            for (i in 1 until (n - 1) / 2) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val idx3 = offa + idx1
                val idx4 = offa + idx2
                ak[idx1] = a[idx3] * bk1[idx1] - a[idx4] * bk1[idx2]
                ak[idx2] = a[idx3] * bk1[idx2] + a[idx4] * bk1[idx1]
            }
            ak[n - 1] = a[offa + n - 1] * bk1[n - 1] - a[offa + 1] * bk1[n]
            ak[n] = a[offa + n - 1] * bk1[n] + a[offa + 1] * bk1[n - 1]
            ak[n + 1] = a[offa + n - 1] * bk1[n + 1] + a[offa + 1] * bk1[n + 2]
            ak[n + 2] = a[offa + n - 1] * bk1[n + 2] - a[offa + 1] * bk1[n + 1]
            for (i in (n - 1) / 2 + 2 until n) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val idx3 = offa + 2 * n - idx1
                val idx4 = idx3 + 1
                ak[idx1] = a[idx3] * bk1[idx1] + a[idx4] * bk1[idx2]
                ak[idx2] = a[idx3] * bk1[idx2] - a[idx4] * bk1[idx1]
            }
        }
        cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
        var nthreads = ConcurrencyUtils.getNumberOfThreads()
        if (nthreads > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2
            if (nthreads >= 4 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
                nthreads = 4
            }

            var k = nBluestein / nthreads
            val futures = Array<Future<*>>(nthreads) { i ->
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) nBluestein else firstIdx + k
                return@Array ConcurrencyUtils.submit {
                    for (i12 in firstIdx until lastIdx) {
                        val idx1 = 2 * i12
                        val idx2 = idx1 + 1
                        val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                        ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                        ak[idx2] = im
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = n / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    for (i1 in firstIdx until lastIdx) {
                        val idx1 = 2 * i1
                        val idx2 = idx1 + 1
                        a[offa + i1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
        } else {
            for (i in 0 until nBluestein) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                ak[idx2] = im
            }
            cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
            for (i in 0 until n) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                a[offa + i] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
            }
        }
    }

    private fun bluestein_real_inverse2(a: FloatArray, offa: Int) {
        val ak = FloatArray(2 * nBluestein)
        var nthreads = ConcurrencyUtils.getNumberOfThreads()
        if (nthreads > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2
            if (nthreads >= 4 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
                nthreads = 4
            }

            var k = n / nthreads
            val futures = Array<Future<*>>(nthreads) { i ->
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) n else firstIdx + k
                return@Array ConcurrencyUtils.submit {
                    for (i12 in firstIdx until lastIdx) {
                        val idx1 = 2 * i12
                        val idx2 = idx1 + 1
                        val idx3 = offa + i12
                        ak[idx1] = a[idx3] * bk1[idx1]
                        ak[idx2] = a[idx3] * bk1[idx2]
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            k = nBluestein / nthreads
            for (i in 0 until nthreads) {
                val firstIdx = i * k
                val lastIdx = if (i == nthreads - 1) nBluestein else firstIdx + k
                futures[i] = ConcurrencyUtils.submit {
                    for (i1 in firstIdx until lastIdx) {
                        val idx1 = 2 * i1
                        val idx2 = idx1 + 1
                        val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                        ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                        ak[idx2] = im
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
        } else {
            for (i in 0 until n) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val idx3 = offa + i
                ak[idx1] = a[idx3] * bk1[idx1]
                ak[idx2] = a[idx3] * bk1[idx2]
            }
            cftbsub(2 * nBluestein, ak, 0, ip, nw, w)
            for (i in 0 until nBluestein) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                val im = -ak[idx1] * bk2[idx2] + ak[idx2] * bk2[idx1]
                ak[idx1] = ak[idx1] * bk2[idx1] + ak[idx2] * bk2[idx2]
                ak[idx2] = im
            }
        }
        cftfsub(2 * nBluestein, ak, 0, ip, nw, w)
        if (n % 2 == 0) {
            a[offa] = bk1[0] * ak[0] - bk1[1] * ak[1]
            a[offa + 1] = bk1[n] * ak[n] - bk1[n + 1] * ak[n + 1]
            for (i in 1 until n / 2) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
            }
        } else {
            a[offa] = bk1[0] * ak[0] - bk1[1] * ak[1]
            a[offa + 1] = bk1[n] * ak[n - 1] + bk1[n - 1] * ak[n]
            for (i in 1 until (n - 1) / 2) {
                val idx1 = 2 * i
                val idx2 = idx1 + 1
                a[offa + idx1] = bk1[idx1] * ak[idx1] - bk1[idx2] * ak[idx2]
                a[offa + idx2] = bk1[idx2] * ak[idx1] + bk1[idx1] * ak[idx2]
            }
            a[offa + n - 1] = bk1[n - 1] * ak[n - 1] - bk1[n] * ak[n]
        }
    }

    /*---------------------------------------------------------
       rfftf1: further processing of Real forward FFT
      --------------------------------------------------------*/
    fun rfftf(a: FloatArray, offa: Int) {
        if (n == 1) return
        var l1: Int
        var l2: Int
        var na: Int
        var kh: Int
        val nf: Int
        var ip: Int
        var iw: Int
        var ido: Int
        var idl1: Int
        val ch = FloatArray(n)
        val twon = 2 * n
        nf = wtableR[1 + twon].toInt()
        na = 1
        l2 = n
        iw = twon - 1
        for (k1 in 1..nf) {
            kh = nf - k1
            ip = wtableR[kh + 2 + twon].toInt()
            l1 = l2 / ip
            ido = n / l2
            idl1 = ido * l1
            iw -= (ip - 1) * ido
            na = 1 - na
            when (ip) {
                2 -> if (na == 0) {
                    radf2(ido, l1, a, offa, ch, 0, iw)
                } else {
                    radf2(ido, l1, ch, 0, a, offa, iw)
                }
                3 -> if (na == 0) {
                    radf3(ido, l1, a, offa, ch, 0, iw)
                } else {
                    radf3(ido, l1, ch, 0, a, offa, iw)
                }
                4 -> if (na == 0) {
                    radf4(ido, l1, a, offa, ch, 0, iw)
                } else {
                    radf4(ido, l1, ch, 0, a, offa, iw)
                }
                5 -> if (na == 0) {
                    radf5(ido, l1, a, offa, ch, 0, iw)
                } else {
                    radf5(ido, l1, ch, 0, a, offa, iw)
                }
                else -> {
                    if (ido == 1) na = 1 - na
                    na = if (na == 0) {
                        radfg(ido, ip, l1, idl1, a, offa, ch, 0, iw)
                        1
                    } else {
                        radfg(ido, ip, l1, idl1, ch, 0, a, offa, iw)
                        0
                    }
                }
            }
            l2 = l1
        }
        if (na == 1) return
        System.arraycopy(ch, 0, a, offa, n)
    }

    /*---------------------------------------------------------
       rfftb1: further processing of Real backward FFT
      --------------------------------------------------------*/
    fun rfftb(a: FloatArray, offa: Int) {
        if (n == 1) return
        var l1: Int
        var l2: Int
        var na: Int
        val nf: Int
        var ip: Int
        var ido: Int
        var idl1: Int
        val ch = FloatArray(n)
        val twon = 2 * n
        nf = wtableR[1 + twon].toInt()
        na = 0
        l1 = 1
        var iw: Int = n
        for (k1 in 1..nf) {
            ip = wtableR[k1 + 1 + twon].toInt()
            l2 = ip * l1
            ido = n / l2
            idl1 = ido * l1
            when (ip) {
                2 -> {
                    if (na == 0) {
                        radb2(ido, l1, a, offa, ch, 0, iw)
                    } else {
                        radb2(ido, l1, ch, 0, a, offa, iw)
                    }
                    na = 1 - na
                }
                3 -> {
                    if (na == 0) {
                        radb3(ido, l1, a, offa, ch, 0, iw)
                    } else {
                        radb3(ido, l1, ch, 0, a, offa, iw)
                    }
                    na = 1 - na
                }
                4 -> {
                    if (na == 0) {
                        radb4(ido, l1, a, offa, ch, 0, iw)
                    } else {
                        radb4(ido, l1, ch, 0, a, offa, iw)
                    }
                    na = 1 - na
                }
                5 -> {
                    if (na == 0) {
                        radb5(ido, l1, a, offa, ch, 0, iw)
                    } else {
                        radb5(ido, l1, ch, 0, a, offa, iw)
                    }
                    na = 1 - na
                }
                else -> {
                    if (na == 0) {
                        radbg(ido, ip, l1, idl1, a, offa, ch, 0, iw)
                    } else {
                        radbg(ido, ip, l1, idl1, ch, 0, a, offa, iw)
                    }
                    if (ido == 1) na = 1 - na
                }
            }
            l1 = l2
            iw += (ip - 1) * ido
        }
        if (na == 0) return
        System.arraycopy(ch, 0, a, offa, n)
    }

    /*-------------------------------------------------
       radf2: Real FFT's forward processing of factor 2
      -------------------------------------------------*/
    fun radf2(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        var i: Int
        var ic: Int
        var idx1: Int
        var idx2: Int
        var idx3: Int
        var idx4: Int
        var t1i: Float
        var t1r: Float
        var w1r: Float
        var w1i: Float
        val iw1: Int = offset
        val idx0: Int = l1 * ido
        idx1 = 2 * ido
        for (k in 0 until l1) {
            val oidx1 = out_off + k * idx1
            val oidx2 = oidx1 + idx1 - 1
            val iidx1 = in_off + k * ido
            val iidx2 = iidx1 + idx0
            val i1r = `in`[iidx1]
            val i2r = `in`[iidx2]
            out[oidx1] = i1r + i2r
            out[oidx2] = i1r - i2r
        }
        if (ido < 2) return
        if (ido != 2) {
            for (k in 0 until l1) {
                idx1 = k * ido
                idx2 = 2 * idx1
                idx3 = idx2 + ido
                idx4 = idx1 + idx0
                i = 2
                while (i < ido) {
                    ic = ido - i
                    val widx1 = i - 1 + iw1
                    val oidx1 = out_off + i + idx2
                    val oidx2 = out_off + ic + idx3
                    val iidx1 = in_off + i + idx1
                    val iidx2 = in_off + i + idx4
                    val a1i = `in`[iidx1 - 1]
                    val a1r = `in`[iidx1]
                    val a2i = `in`[iidx2 - 1]
                    val a2r = `in`[iidx2]
                    w1r = wtableR[widx1 - 1]
                    w1i = wtableR[widx1]
                    t1r = w1r * a2i + w1i * a2r
                    t1i = w1r * a2r - w1i * a2i
                    out[oidx1] = a1r + t1i
                    out[oidx1 - 1] = a1i + t1r
                    out[oidx2] = t1i - a1r
                    out[oidx2 - 1] = a1i - t1r
                    i += 2
                }
            }
            if (ido % 2 == 1) return
        }
        idx2 = 2 * idx1
        for (k in 0 until l1) {
            idx1 = k * ido
            val oidx1 = out_off + idx2 + ido
            val iidx1 = in_off + ido - 1 + idx1
            out[oidx1] = -`in`[iidx1 + idx0]
            out[oidx1 - 1] = `in`[iidx1]
        }
    }

    /*-------------------------------------------------
       radb2: Real FFT's backward processing of factor 2
      -------------------------------------------------*/
    fun radb2(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        var i: Int
        var ic: Int
        var t1i: Float
        var t1r: Float
        var w1r: Float
        var w1i: Float
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 2 * idx1
            val idx3 = idx2 + ido
            val oidx1 = out_off + idx1
            val iidx1 = in_off + idx2
            val iidx2 = in_off + ido - 1 + idx3
            val i1r = `in`[iidx1]
            val i2r = `in`[iidx2]
            out[oidx1] = i1r + i2r
            out[oidx1 + idx0] = i1r - i2r
        }
        if (ido < 2) return
        if (ido != 2) {
            for (k in 0 until l1) {
                val idx1 = k * ido
                val idx2 = 2 * idx1
                val idx3 = idx2 + ido
                val idx4 = idx1 + idx0
                i = 2
                while (i < ido) {
                    ic = ido - i
                    val idx5 = i - 1 + offset
                    val idx6 = out_off + i
                    val idx7 = in_off + i
                    val idx8 = in_off + ic
                    w1r = wtableR[idx5 - 1]
                    w1i = wtableR[idx5]
                    val iidx1 = idx7 + idx2
                    val iidx2 = idx8 + idx3
                    val oidx1 = idx6 + idx1
                    val oidx2 = idx6 + idx4
                    t1r = `in`[iidx1 - 1] - `in`[iidx2 - 1]
                    t1i = `in`[iidx1] + `in`[iidx2]
                    val i1i = `in`[iidx1]
                    val i1r = `in`[iidx1 - 1]
                    val i2i = `in`[iidx2]
                    val i2r = `in`[iidx2 - 1]
                    out[oidx1 - 1] = i1r + i2r
                    out[oidx1] = i1i - i2i
                    out[oidx2 - 1] = w1r * t1r - w1i * t1i
                    out[oidx2] = w1r * t1i + w1i * t1r
                    i += 2
                }
            }
            if (ido % 2 == 1) return
        }
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 2 * idx1
            val oidx1 = out_off + ido - 1 + idx1
            val iidx1 = in_off + idx2 + ido
            out[oidx1] = 2 * `in`[iidx1 - 1]
            out[oidx1 + idx0] = -2 * `in`[iidx1]
        }
    }

    /*-------------------------------------------------
       radf3: Real FFT's forward processing of factor 3
      -------------------------------------------------*/
    fun radf3(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        val taur = -0.5f
        val taui = 0.866025403784438707610604524234076962f
        var i: Int
        var ic: Int
        var ci2: Float
        var di2: Float
        var di3: Float
        var cr2: Float
        var dr2: Float
        var dr3: Float
        var ti2: Float
        var ti3: Float
        var tr2: Float
        var tr3: Float
        var w1r: Float
        var w2r: Float
        var w1i: Float
        var w2i: Float
        val iw2: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx3 = 2 * idx0
            val idx4 = (3 * k + 1) * ido
            val iidx1 = in_off + idx1
            val iidx2 = iidx1 + idx0
            val iidx3 = iidx1 + idx3
            val i1r = `in`[iidx1]
            val i2r = `in`[iidx2]
            val i3r = `in`[iidx3]
            cr2 = i2r + i3r
            out[out_off + 3 * idx1] = i1r + cr2
            out[out_off + idx4 + ido] = taui * (i3r - i2r)
            out[out_off + ido - 1 + idx4] = i1r + taur * cr2
        }
        if (ido == 1) return
        for (k in 0 until l1) {
            val idx3 = k * ido
            val idx4 = 3 * idx3
            val idx5 = idx3 + idx0
            val idx6 = idx5 + idx0
            val idx7 = idx4 + ido
            val idx8 = idx7 + ido
            i = 2
            while (i < ido) {
                ic = ido - i
                val widx1 = i - 1 + iw1
                val widx2 = i - 1 + iw2
                w1r = wtableR[widx1 - 1]
                w1i = wtableR[widx1]
                w2r = wtableR[widx2 - 1]
                w2i = wtableR[widx2]
                val idx9 = in_off + i
                val idx10 = out_off + i
                val idx11 = out_off + ic
                val iidx1 = idx9 + idx3
                val iidx2 = idx9 + idx5
                val iidx3 = idx9 + idx6
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val i3i = `in`[iidx3 - 1]
                val i3r = `in`[iidx3]
                dr2 = w1r * i2i + w1i * i2r
                di2 = w1r * i2r - w1i * i2i
                dr3 = w2r * i3i + w2i * i3r
                di3 = w2r * i3r - w2i * i3i
                cr2 = dr2 + dr3
                ci2 = di2 + di3
                tr2 = i1i + taur * cr2
                ti2 = i1r + taur * ci2
                tr3 = taui * (di2 - di3)
                ti3 = taui * (dr3 - dr2)
                val oidx1 = idx10 + idx4
                val oidx2 = idx11 + idx7
                val oidx3 = idx10 + idx8
                out[oidx1 - 1] = i1i + cr2
                out[oidx1] = i1r + ci2
                out[oidx2 - 1] = tr2 - tr3
                out[oidx2] = ti3 - ti2
                out[oidx3 - 1] = tr2 + tr3
                out[oidx3] = ti2 + ti3
                i += 2
            }
        }
    }

    /*-------------------------------------------------
       radb3: Real FFT's backward processing of factor 3
      -------------------------------------------------*/
    fun radb3(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        val taur = -0.5f
        val taui = 0.866025403784438707610604524234076962f
        var i: Int
        var ic: Int
        var ci2: Float
        var ci3: Float
        var di2: Float
        var di3: Float
        var cr2: Float
        var cr3: Float
        var dr2: Float
        var dr3: Float
        var ti2: Float
        var tr2: Float
        var w1r: Float
        var w2r: Float
        var w1i: Float
        var w2i: Float
        val iw2: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val iidx1 = in_off + 3 * idx1
            val iidx2 = iidx1 + 2 * ido
            val i1i = `in`[iidx1]
            tr2 = 2 * `in`[iidx2 - 1]
            cr2 = i1i + taur * tr2
            ci3 = 2 * taui * `in`[iidx2]
            out[out_off + idx1] = i1i + tr2
            out[out_off + (k + l1) * ido] = cr2 - ci3
            out[out_off + (k + 2 * l1) * ido] = cr2 + ci3
        }
        if (ido == 1) return
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 3 * idx1
            val idx3 = idx2 + ido
            val idx4 = idx3 + ido
            val idx5 = idx1 + idx0
            val idx6 = idx5 + idx0
            i = 2
            while (i < ido) {
                ic = ido - i
                val idx7 = in_off + i
                val idx8 = in_off + ic
                val idx9 = out_off + i
                val iidx1 = idx7 + idx2
                val iidx2 = idx7 + idx4
                val iidx3 = idx8 + idx3
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val i3i = `in`[iidx3 - 1]
                val i3r = `in`[iidx3]
                tr2 = i2i + i3i
                cr2 = i1i + taur * tr2
                ti2 = i2r - i3r
                ci2 = i1r + taur * ti2
                cr3 = taui * (i2i - i3i)
                ci3 = taui * (i2r + i3r)
                dr2 = cr2 - ci3
                dr3 = cr2 + ci3
                di2 = ci2 + cr3
                di3 = ci2 - cr3
                val widx1 = i - 1 + iw1
                val widx2 = i - 1 + iw2
                w1r = wtableR[widx1 - 1]
                w1i = wtableR[widx1]
                w2r = wtableR[widx2 - 1]
                w2i = wtableR[widx2]
                val oidx1 = idx9 + idx1
                val oidx2 = idx9 + idx5
                val oidx3 = idx9 + idx6
                out[oidx1 - 1] = i1i + tr2
                out[oidx1] = i1r + ti2
                out[oidx2 - 1] = w1r * dr2 - w1i * di2
                out[oidx2] = w1r * di2 + w1i * dr2
                out[oidx3 - 1] = w2r * dr3 - w2i * di3
                out[oidx3] = w2r * di3 + w2i * dr3
                i += 2
            }
        }
    }

    /*-------------------------------------------------
       radf4: Real FFT's forward processing of factor 4
      -------------------------------------------------*/
    fun radf4(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        val hsqt2 = 0.707106781186547572737310929369414225f
        var i: Int
        var ic: Int
        var ci2: Float
        var ci3: Float
        var ci4: Float
        var cr2: Float
        var cr3: Float
        var cr4: Float
        var ti1: Float
        var ti2: Float
        var ti3: Float
        var ti4: Float
        var tr1: Float
        var tr2: Float
        var tr3: Float
        var tr4: Float
        var w1r: Float
        var w1i: Float
        var w2r: Float
        var w2i: Float
        var w3r: Float
        var w3i: Float
        val iw3: Int
        val iw1: Int = offset
        val iw2: Int = offset + ido
        iw3 = iw2 + ido
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 4 * idx1
            val idx3 = idx1 + idx0
            val idx4 = idx3 + idx0
            val idx5 = idx4 + idx0
            val idx6 = idx2 + ido
            val i1r = `in`[in_off + idx1]
            val i2r = `in`[in_off + idx3]
            val i3r = `in`[in_off + idx4]
            val i4r = `in`[in_off + idx5]
            tr1 = i2r + i4r
            tr2 = i1r + i3r
            val oidx1 = out_off + idx2
            val oidx2 = out_off + idx6 + ido
            out[oidx1] = tr1 + tr2
            out[oidx2 - 1 + ido + ido] = tr2 - tr1
            out[oidx2 - 1] = i1r - i3r
            out[oidx2] = i4r - i2r
        }
        if (ido < 2) return
        if (ido != 2) {
            for (k in 0 until l1) {
                val idx1 = k * ido
                val idx2 = idx1 + idx0
                val idx3 = idx2 + idx0
                val idx4 = idx3 + idx0
                val idx5 = 4 * idx1
                val idx6 = idx5 + ido
                val idx7 = idx6 + ido
                val idx8 = idx7 + ido
                i = 2
                while (i < ido) {
                    ic = ido - i
                    val widx1 = i - 1 + iw1
                    val widx2 = i - 1 + iw2
                    val widx3 = i - 1 + iw3
                    w1r = wtableR[widx1 - 1]
                    w1i = wtableR[widx1]
                    w2r = wtableR[widx2 - 1]
                    w2i = wtableR[widx2]
                    w3r = wtableR[widx3 - 1]
                    w3i = wtableR[widx3]
                    val idx9 = in_off + i
                    val idx10 = out_off + i
                    val idx11 = out_off + ic
                    val iidx1 = idx9 + idx1
                    val iidx2 = idx9 + idx2
                    val iidx3 = idx9 + idx3
                    val iidx4 = idx9 + idx4
                    val i1i = `in`[iidx1 - 1]
                    val i1r = `in`[iidx1]
                    val i2i = `in`[iidx2 - 1]
                    val i2r = `in`[iidx2]
                    val i3i = `in`[iidx3 - 1]
                    val i3r = `in`[iidx3]
                    val i4i = `in`[iidx4 - 1]
                    val i4r = `in`[iidx4]
                    cr2 = w1r * i2i + w1i * i2r
                    ci2 = w1r * i2r - w1i * i2i
                    cr3 = w2r * i3i + w2i * i3r
                    ci3 = w2r * i3r - w2i * i3i
                    cr4 = w3r * i4i + w3i * i4r
                    ci4 = w3r * i4r - w3i * i4i
                    tr1 = cr2 + cr4
                    tr4 = cr4 - cr2
                    ti1 = ci2 + ci4
                    ti4 = ci2 - ci4
                    ti2 = i1r + ci3
                    ti3 = i1r - ci3
                    tr2 = i1i + cr3
                    tr3 = i1i - cr3
                    val oidx1 = idx10 + idx5
                    val oidx2 = idx11 + idx6
                    val oidx3 = idx10 + idx7
                    val oidx4 = idx11 + idx8
                    out[oidx1 - 1] = tr1 + tr2
                    out[oidx4 - 1] = tr2 - tr1
                    out[oidx1] = ti1 + ti2
                    out[oidx4] = ti1 - ti2
                    out[oidx3 - 1] = ti4 + tr3
                    out[oidx2 - 1] = tr3 - ti4
                    out[oidx3] = tr4 + ti3
                    out[oidx2] = tr4 - ti3
                    i += 2
                }
            }
            if (ido % 2 == 1) return
        }
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 4 * idx1
            val idx3 = idx1 + idx0
            val idx4 = idx3 + idx0
            val idx5 = idx4 + idx0
            val idx6 = idx2 + ido
            val idx7 = idx6 + ido
            val idx8 = idx7 + ido
            val idx9 = in_off + ido
            val idx10 = out_off + ido
            val i1i = `in`[idx9 - 1 + idx1]
            val i2i = `in`[idx9 - 1 + idx3]
            val i3i = `in`[idx9 - 1 + idx4]
            val i4i = `in`[idx9 - 1 + idx5]
            ti1 = -hsqt2 * (i2i + i4i)
            tr1 = hsqt2 * (i2i - i4i)
            out[idx10 - 1 + idx2] = tr1 + i1i
            out[idx10 - 1 + idx7] = i1i - tr1
            out[out_off + idx6] = ti1 - i3i
            out[out_off + idx8] = ti1 + i3i
        }
    }

    /*-------------------------------------------------
       radb4: Real FFT's backward processing of factor 4
      -------------------------------------------------*/
    fun radb4(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        val sqrt2 = 1.41421356237309514547462185873882845f
        var i: Int
        var ic: Int
        var ci2: Float
        var ci3: Float
        var ci4: Float
        var cr2: Float
        var cr3: Float
        var cr4: Float
        var ti1: Float
        var ti2: Float
        var ti3: Float
        var ti4: Float
        var tr1: Float
        var tr2: Float
        var tr3: Float
        var tr4: Float
        var w1r: Float
        var w1i: Float
        var w2r: Float
        var w2i: Float
        var w3r: Float
        var w3i: Float
        val iw2: Int
        val iw3: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        iw3 = iw2 + ido
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 4 * idx1
            val idx3 = idx1 + idx0
            val idx4 = idx3 + idx0
            val idx5 = idx4 + idx0
            val idx6 = idx2 + ido
            val idx7 = idx6 + ido
            val idx8 = idx7 + ido
            val i1r = `in`[in_off + idx2]
            val i2r = `in`[in_off + idx7]
            val i3r = `in`[in_off + ido - 1 + idx8]
            val i4r = `in`[in_off + ido - 1 + idx6]
            tr1 = i1r - i3r
            tr2 = i1r + i3r
            tr3 = i4r + i4r
            tr4 = i2r + i2r
            out[out_off + idx1] = tr2 + tr3
            out[out_off + idx3] = tr1 - tr4
            out[out_off + idx4] = tr2 - tr3
            out[out_off + idx5] = tr1 + tr4
        }
        if (ido < 2) return
        if (ido != 2) {
            for (k in 0 until l1) {
                val idx1 = k * ido
                val idx2 = idx1 + idx0
                val idx3 = idx2 + idx0
                val idx4 = idx3 + idx0
                val idx5 = 4 * idx1
                val idx6 = idx5 + ido
                val idx7 = idx6 + ido
                val idx8 = idx7 + ido
                i = 2
                while (i < ido) {
                    ic = ido - i
                    val widx1 = i - 1 + iw1
                    val widx2 = i - 1 + iw2
                    val widx3 = i - 1 + iw3
                    w1r = wtableR[widx1 - 1]
                    w1i = wtableR[widx1]
                    w2r = wtableR[widx2 - 1]
                    w2i = wtableR[widx2]
                    w3r = wtableR[widx3 - 1]
                    w3i = wtableR[widx3]
                    val idx12 = in_off + i
                    val idx13 = in_off + ic
                    val idx14 = out_off + i
                    val iidx1 = idx12 + idx5
                    val iidx2 = idx13 + idx6
                    val iidx3 = idx12 + idx7
                    val iidx4 = idx13 + idx8
                    val i1i = `in`[iidx1 - 1]
                    val i1r = `in`[iidx1]
                    val i2i = `in`[iidx2 - 1]
                    val i2r = `in`[iidx2]
                    val i3i = `in`[iidx3 - 1]
                    val i3r = `in`[iidx3]
                    val i4i = `in`[iidx4 - 1]
                    val i4r = `in`[iidx4]
                    ti1 = i1r + i4r
                    ti2 = i1r - i4r
                    ti3 = i3r - i2r
                    tr4 = i3r + i2r
                    tr1 = i1i - i4i
                    tr2 = i1i + i4i
                    ti4 = i3i - i2i
                    tr3 = i3i + i2i
                    cr3 = tr2 - tr3
                    ci3 = ti2 - ti3
                    cr2 = tr1 - tr4
                    cr4 = tr1 + tr4
                    ci2 = ti1 + ti4
                    ci4 = ti1 - ti4
                    val oidx1 = idx14 + idx1
                    val oidx2 = idx14 + idx2
                    val oidx3 = idx14 + idx3
                    val oidx4 = idx14 + idx4
                    out[oidx1 - 1] = tr2 + tr3
                    out[oidx1] = ti2 + ti3
                    out[oidx2 - 1] = w1r * cr2 - w1i * ci2
                    out[oidx2] = w1r * ci2 + w1i * cr2
                    out[oidx3 - 1] = w2r * cr3 - w2i * ci3
                    out[oidx3] = w2r * ci3 + w2i * cr3
                    out[oidx4 - 1] = w3r * cr4 - w3i * ci4
                    out[oidx4] = w3r * ci4 + w3i * cr4
                    i += 2
                }
            }
            if (ido % 2 == 1) return
        }
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 4 * idx1
            val idx3 = idx1 + idx0
            val idx4 = idx3 + idx0
            val idx5 = idx4 + idx0
            val idx6 = idx2 + ido
            val idx7 = idx6 + ido
            val idx8 = idx7 + ido
            val idx9 = in_off + ido
            val idx10 = out_off + ido
            val i1r = `in`[idx9 - 1 + idx2]
            val i2r = `in`[idx9 - 1 + idx7]
            val i3r = `in`[in_off + idx6]
            val i4r = `in`[in_off + idx8]
            ti1 = i3r + i4r
            ti2 = i4r - i3r
            tr1 = i1r - i2r
            tr2 = i1r + i2r
            out[idx10 - 1 + idx1] = tr2 + tr2
            out[idx10 - 1 + idx3] = sqrt2 * (tr1 - ti1)
            out[idx10 - 1 + idx4] = ti2 + ti2
            out[idx10 - 1 + idx5] = -sqrt2 * (tr1 + ti1)
        }
    }

    /*-------------------------------------------------
       radf5: Real FFT's forward processing of factor 5
      -------------------------------------------------*/
    fun radf5(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        val tr11 = 0.309016994374947451262869435595348477f
        val ti11 = 0.951056516295153531181938433292089030f
        val tr12 = -0.809016994374947340240566973079694435f
        val ti12 = 0.587785252292473248125759255344746634f
        var i: Int
        var ic: Int
        var ci2: Float
        var di2: Float
        var ci4: Float
        var ci5: Float
        var di3: Float
        var di4: Float
        var di5: Float
        var ci3: Float
        var cr2: Float
        var cr3: Float
        var dr2: Float
        var dr3: Float
        var dr4: Float
        var dr5: Float
        var cr5: Float
        var cr4: Float
        var ti2: Float
        var ti3: Float
        var ti5: Float
        var ti4: Float
        var tr2: Float
        var tr3: Float
        var tr4: Float
        var tr5: Float
        var w1r: Float
        var w1i: Float
        var w2r: Float
        var w2i: Float
        var w3r: Float
        var w3i: Float
        var w4r: Float
        var w4i: Float
        val iw2: Int
        val iw3: Int
        val iw4: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        iw3 = iw2 + ido
        iw4 = iw3 + ido
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 5 * idx1
            val idx3 = idx2 + ido
            val idx4 = idx3 + ido
            val idx5 = idx4 + ido
            val idx6 = idx5 + ido
            val idx7 = idx1 + idx0
            val idx8 = idx7 + idx0
            val idx9 = idx8 + idx0
            val idx10 = idx9 + idx0
            val idx11 = out_off + ido - 1
            val i1r = `in`[in_off + idx1]
            val i2r = `in`[in_off + idx7]
            val i3r = `in`[in_off + idx8]
            val i4r = `in`[in_off + idx9]
            val i5r = `in`[in_off + idx10]
            cr2 = i5r + i2r
            ci5 = i5r - i2r
            cr3 = i4r + i3r
            ci4 = i4r - i3r
            out[out_off + idx2] = i1r + cr2 + cr3
            out[idx11 + idx3] = i1r + tr11 * cr2 + tr12 * cr3
            out[out_off + idx4] = ti11 * ci5 + ti12 * ci4
            out[idx11 + idx5] = i1r + tr12 * cr2 + tr11 * cr3
            out[out_off + idx6] = ti12 * ci5 - ti11 * ci4
        }
        if (ido == 1) return
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 5 * idx1
            val idx3 = idx2 + ido
            val idx4 = idx3 + ido
            val idx5 = idx4 + ido
            val idx6 = idx5 + ido
            val idx7 = idx1 + idx0
            val idx8 = idx7 + idx0
            val idx9 = idx8 + idx0
            val idx10 = idx9 + idx0
            i = 2
            while (i < ido) {
                val widx1 = i - 1 + iw1
                val widx2 = i - 1 + iw2
                val widx3 = i - 1 + iw3
                val widx4 = i - 1 + iw4
                w1r = wtableR[widx1 - 1]
                w1i = wtableR[widx1]
                w2r = wtableR[widx2 - 1]
                w2i = wtableR[widx2]
                w3r = wtableR[widx3 - 1]
                w3i = wtableR[widx3]
                w4r = wtableR[widx4 - 1]
                w4i = wtableR[widx4]
                ic = ido - i
                val idx15 = in_off + i
                val idx16 = out_off + i
                val idx17 = out_off + ic
                val iidx1 = idx15 + idx1
                val iidx2 = idx15 + idx7
                val iidx3 = idx15 + idx8
                val iidx4 = idx15 + idx9
                val iidx5 = idx15 + idx10
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val i3i = `in`[iidx3 - 1]
                val i3r = `in`[iidx3]
                val i4i = `in`[iidx4 - 1]
                val i4r = `in`[iidx4]
                val i5i = `in`[iidx5 - 1]
                val i5r = `in`[iidx5]
                dr2 = w1r * i2i + w1i * i2r
                di2 = w1r * i2r - w1i * i2i
                dr3 = w2r * i3i + w2i * i3r
                di3 = w2r * i3r - w2i * i3i
                dr4 = w3r * i4i + w3i * i4r
                di4 = w3r * i4r - w3i * i4i
                dr5 = w4r * i5i + w4i * i5r
                di5 = w4r * i5r - w4i * i5i
                cr2 = dr2 + dr5
                ci5 = dr5 - dr2
                cr5 = di2 - di5
                ci2 = di2 + di5
                cr3 = dr3 + dr4
                ci4 = dr4 - dr3
                cr4 = di3 - di4
                ci3 = di3 + di4
                tr2 = i1i + tr11 * cr2 + tr12 * cr3
                ti2 = i1r + tr11 * ci2 + tr12 * ci3
                tr3 = i1i + tr12 * cr2 + tr11 * cr3
                ti3 = i1r + tr12 * ci2 + tr11 * ci3
                tr5 = ti11 * cr5 + ti12 * cr4
                ti5 = ti11 * ci5 + ti12 * ci4
                tr4 = ti12 * cr5 - ti11 * cr4
                ti4 = ti12 * ci5 - ti11 * ci4
                val oidx1 = idx16 + idx2
                val oidx2 = idx17 + idx3
                val oidx3 = idx16 + idx4
                val oidx4 = idx17 + idx5
                val oidx5 = idx16 + idx6
                out[oidx1 - 1] = i1i + cr2 + cr3
                out[oidx1] = i1r + ci2 + ci3
                out[oidx3 - 1] = tr2 + tr5
                out[oidx2 - 1] = tr2 - tr5
                out[oidx3] = ti2 + ti5
                out[oidx2] = ti5 - ti2
                out[oidx5 - 1] = tr3 + tr4
                out[oidx4 - 1] = tr3 - tr4
                out[oidx5] = ti3 + ti4
                out[oidx4] = ti4 - ti3
                i += 2
            }
        }
    }

    /*-------------------------------------------------
       radb5: Real FFT's backward processing of factor 5
      -------------------------------------------------*/
    fun radb5(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        val tr11 = 0.309016994374947451262869435595348477f
        val ti11 = 0.951056516295153531181938433292089030f
        val tr12 = -0.809016994374947340240566973079694435f
        val ti12 = 0.587785252292473248125759255344746634f
        var i: Int
        var ic: Int
        var ci2: Float
        var ci3: Float
        var ci4: Float
        var ci5: Float
        var di3: Float
        var di4: Float
        var di5: Float
        var di2: Float
        var cr2: Float
        var cr3: Float
        var cr5: Float
        var cr4: Float
        var ti2: Float
        var ti3: Float
        var ti4: Float
        var ti5: Float
        var dr3: Float
        var dr4: Float
        var dr5: Float
        var dr2: Float
        var tr2: Float
        var tr3: Float
        var tr4: Float
        var tr5: Float
        var w1r: Float
        var w1i: Float
        var w2r: Float
        var w2i: Float
        var w3r: Float
        var w3i: Float
        var w4r: Float
        var w4i: Float
        val iw2: Int
        val iw3: Int
        val iw4: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        iw3 = iw2 + ido
        iw4 = iw3 + ido
        val idx0 = l1 * ido
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 5 * idx1
            val idx3 = idx2 + ido
            val idx4 = idx3 + ido
            val idx5 = idx4 + ido
            val idx6 = idx5 + ido
            val idx7 = idx1 + idx0
            val idx8 = idx7 + idx0
            val idx9 = idx8 + idx0
            val idx10 = idx9 + idx0
            val idx11 = in_off + ido - 1
            val i1r = `in`[in_off + idx2]
            ti5 = 2 * `in`[in_off + idx4]
            ti4 = 2 * `in`[in_off + idx6]
            tr2 = 2 * `in`[idx11 + idx3]
            tr3 = 2 * `in`[idx11 + idx5]
            cr2 = i1r + tr11 * tr2 + tr12 * tr3
            cr3 = i1r + tr12 * tr2 + tr11 * tr3
            ci5 = ti11 * ti5 + ti12 * ti4
            ci4 = ti12 * ti5 - ti11 * ti4
            out[out_off + idx1] = i1r + tr2 + tr3
            out[out_off + idx7] = cr2 - ci5
            out[out_off + idx8] = cr3 - ci4
            out[out_off + idx9] = cr3 + ci4
            out[out_off + idx10] = cr2 + ci5
        }
        if (ido == 1) return
        for (k in 0 until l1) {
            val idx1 = k * ido
            val idx2 = 5 * idx1
            val idx3 = idx2 + ido
            val idx4 = idx3 + ido
            val idx5 = idx4 + ido
            val idx6 = idx5 + ido
            val idx7 = idx1 + idx0
            val idx8 = idx7 + idx0
            val idx9 = idx8 + idx0
            val idx10 = idx9 + idx0
            i = 2
            while (i < ido) {
                ic = ido - i
                val widx1 = i - 1 + iw1
                val widx2 = i - 1 + iw2
                val widx3 = i - 1 + iw3
                val widx4 = i - 1 + iw4
                w1r = wtableR[widx1 - 1]
                w1i = wtableR[widx1]
                w2r = wtableR[widx2 - 1]
                w2i = wtableR[widx2]
                w3r = wtableR[widx3 - 1]
                w3i = wtableR[widx3]
                w4r = wtableR[widx4 - 1]
                w4i = wtableR[widx4]
                val idx15 = in_off + i
                val idx16 = in_off + ic
                val idx17 = out_off + i
                val iidx1 = idx15 + idx2
                val iidx2 = idx16 + idx3
                val iidx3 = idx15 + idx4
                val iidx4 = idx16 + idx5
                val iidx5 = idx15 + idx6
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val i3i = `in`[iidx3 - 1]
                val i3r = `in`[iidx3]
                val i4i = `in`[iidx4 - 1]
                val i4r = `in`[iidx4]
                val i5i = `in`[iidx5 - 1]
                val i5r = `in`[iidx5]
                ti5 = i3r + i2r
                ti2 = i3r - i2r
                ti4 = i5r + i4r
                ti3 = i5r - i4r
                tr5 = i3i - i2i
                tr2 = i3i + i2i
                tr4 = i5i - i4i
                tr3 = i5i + i4i
                cr2 = i1i + tr11 * tr2 + tr12 * tr3
                ci2 = i1r + tr11 * ti2 + tr12 * ti3
                cr3 = i1i + tr12 * tr2 + tr11 * tr3
                ci3 = i1r + tr12 * ti2 + tr11 * ti3
                cr5 = ti11 * tr5 + ti12 * tr4
                ci5 = ti11 * ti5 + ti12 * ti4
                cr4 = ti12 * tr5 - ti11 * tr4
                ci4 = ti12 * ti5 - ti11 * ti4
                dr3 = cr3 - ci4
                dr4 = cr3 + ci4
                di3 = ci3 + cr4
                di4 = ci3 - cr4
                dr5 = cr2 + ci5
                dr2 = cr2 - ci5
                di5 = ci2 - cr5
                di2 = ci2 + cr5
                val oidx1 = idx17 + idx1
                val oidx2 = idx17 + idx7
                val oidx3 = idx17 + idx8
                val oidx4 = idx17 + idx9
                val oidx5 = idx17 + idx10
                out[oidx1 - 1] = i1i + tr2 + tr3
                out[oidx1] = i1r + ti2 + ti3
                out[oidx2 - 1] = w1r * dr2 - w1i * di2
                out[oidx2] = w1r * di2 + w1i * dr2
                out[oidx3 - 1] = w2r * dr3 - w2i * di3
                out[oidx3] = w2r * di3 + w2i * dr3
                out[oidx4 - 1] = w3r * dr4 - w3i * di4
                out[oidx4] = w3r * di4 + w3i * dr4
                out[oidx5 - 1] = w4r * dr5 - w4i * di5
                out[oidx5] = w4r * di5 + w4i * dr5
                i += 2
            }
        }
    }

    /*---------------------------------------------------------
       radfg: Real FFT's forward processing of general factor
      --------------------------------------------------------*/
    fun radfg(
        ido: Int,
        ip: Int,
        l1: Int,
        idl1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        var idij: Int
        var j2: Int
        var ic: Int
        var jc: Int
        var lc: Int
        var `is`: Int
        var dc2: Float
        var ai1: Float
        var ai2: Float
        var ar1: Float
        var ar2: Float
        var ds2: Float
        val dcp: Float
        val arg: Float
        val dsp: Float
        var ar1h: Float
        var ar2h: Float
        var w1r: Float
        var w1i: Float
        arg = TWO_PI / ip.toFloat()
        dcp = cos(arg.toDouble()).toFloat()
        dsp = sin(arg.toDouble()).toFloat()
        val ipph: Int = (ip + 1) / 2
        val nbd: Int = (ido - 1) / 2
        if (ido != 1) {
            if (idl1 >= 0) System.arraycopy(`in`, in_off, out, out_off, idl1)
            for (j in 1 until ip) {
                val idx1 = j * l1 * ido
                for (k in 0 until l1) {
                    val idx2 = k * ido + idx1
                    out[out_off + idx2] = `in`[in_off + idx2]
                }
            }
            if (nbd <= l1) {
                `is` = -ido
                for (j in 1 until ip) {
                    `is` += ido
                    idij = `is` - 1
                    val idx1 = j * l1 * ido
                    var i = 2
                    while (i < ido) {
                        idij += 2
                        val idx2 = idij + offset
                        val idx4 = in_off + i
                        val idx5 = out_off + i
                        w1r = wtableR[idx2 - 1]
                        w1i = wtableR[idx2]
                        for (k in 0 until l1) {
                            val idx3 = k * ido + idx1
                            val oidx1 = idx5 + idx3
                            val iidx1 = idx4 + idx3
                            val i1i = `in`[iidx1 - 1]
                            val i1r = `in`[iidx1]
                            out[oidx1 - 1] = w1r * i1i + w1i * i1r
                            out[oidx1] = w1r * i1r - w1i * i1i
                        }
                        i += 2
                    }
                }
            } else {
                `is` = -ido
                for (j in 1 until ip) {
                    `is` += ido
                    val idx1 = j * l1 * ido
                    for (k in 0 until l1) {
                        idij = `is` - 1
                        val idx3 = k * ido + idx1
                        var i = 2
                        while (i < ido) {
                            idij += 2
                            val idx2 = idij + offset
                            w1r = wtableR[idx2 - 1]
                            w1i = wtableR[idx2]
                            val oidx1 = out_off + i + idx3
                            val iidx1 = in_off + i + idx3
                            val i1i = `in`[iidx1 - 1]
                            val i1r = `in`[iidx1]
                            out[oidx1 - 1] = w1r * i1i + w1i * i1r
                            out[oidx1] = w1r * i1r - w1i * i1i
                            i += 2
                        }
                    }
                }
            }
            if (nbd >= l1) {
                for (j in 1 until ipph) {
                    jc = ip - j
                    val idx1 = j * l1 * ido
                    val idx2 = jc * l1 * ido
                    for (k in 0 until l1) {
                        val idx3 = k * ido + idx1
                        val idx4 = k * ido + idx2
                        var i = 2
                        while (i < ido) {
                            val idx5 = in_off + i
                            val idx6 = out_off + i
                            val iidx1 = idx5 + idx3
                            val iidx2 = idx5 + idx4
                            val oidx1 = idx6 + idx3
                            val oidx2 = idx6 + idx4
                            val o1i = out[oidx1 - 1]
                            val o1r = out[oidx1]
                            val o2i = out[oidx2 - 1]
                            val o2r = out[oidx2]
                            `in`[iidx1 - 1] = o1i + o2i
                            `in`[iidx1] = o1r + o2r
                            `in`[iidx2 - 1] = o1r - o2r
                            `in`[iidx2] = o2i - o1i
                            i += 2
                        }
                    }
                }
            } else {
                for (j in 1 until ipph) {
                    jc = ip - j
                    val idx1 = j * l1 * ido
                    val idx2 = jc * l1 * ido
                    var i = 2
                    while (i < ido) {
                        val idx5 = in_off + i
                        val idx6 = out_off + i
                        for (k in 0 until l1) {
                            val idx3 = k * ido + idx1
                            val idx4 = k * ido + idx2
                            val iidx1 = idx5 + idx3
                            val iidx2 = idx5 + idx4
                            val oidx1 = idx6 + idx3
                            val oidx2 = idx6 + idx4
                            val o1i = out[oidx1 - 1]
                            val o1r = out[oidx1]
                            val o2i = out[oidx2 - 1]
                            val o2r = out[oidx2]
                            `in`[iidx1 - 1] = o1i + o2i
                            `in`[iidx1] = o1r + o2r
                            `in`[iidx2 - 1] = o1r - o2r
                            `in`[iidx2] = o2i - o1i
                        }
                        i += 2
                    }
                }
            }
        } else {
            System.arraycopy(out, out_off, `in`, in_off, idl1)
        }
        for (j in 1 until ipph) {
            jc = ip - j
            val idx1 = j * l1 * ido
            val idx2 = jc * l1 * ido
            for (k in 0 until l1) {
                val idx3 = k * ido + idx1
                val idx4 = k * ido + idx2
                val oidx1 = out_off + idx3
                val oidx2 = out_off + idx4
                val o1r = out[oidx1]
                val o2r = out[oidx2]
                `in`[in_off + idx3] = o1r + o2r
                `in`[in_off + idx4] = o2r - o1r
            }
        }
        ar1 = 1f
        ai1 = 0f
        val idx0 = (ip - 1) * idl1
        for (l in 1 until ipph) {
            lc = ip - l
            ar1h = dcp * ar1 - dsp * ai1
            ai1 = dcp * ai1 + dsp * ar1
            ar1 = ar1h
            val idx1 = l * idl1
            val idx2 = lc * idl1
            for (ik in 0 until idl1) {
                val idx3 = out_off + ik
                val idx4 = in_off + ik
                out[idx3 + idx1] = `in`[idx4] + ar1 * `in`[idx4 + idl1]
                out[idx3 + idx2] = ai1 * `in`[idx4 + idx0]
            }
            dc2 = ar1
            ds2 = ai1
            ar2 = ar1
            ai2 = ai1
            for (j in 2 until ipph) {
                jc = ip - j
                ar2h = dc2 * ar2 - ds2 * ai2
                ai2 = dc2 * ai2 + ds2 * ar2
                ar2 = ar2h
                val idx3 = j * idl1
                val idx4 = jc * idl1
                for (ik in 0 until idl1) {
                    val idx5 = out_off + ik
                    val idx6 = in_off + ik
                    out[idx5 + idx1] += ar2 * `in`[idx6 + idx3]
                    out[idx5 + idx2] += ai2 * `in`[idx6 + idx4]
                }
            }
        }
        for (j in 1 until ipph) {
            val idx1 = j * idl1
            for (ik in 0 until idl1) {
                out[out_off + ik] += `in`[in_off + ik + idx1]
            }
        }
        if (ido >= l1) {
            for (k in 0 until l1) {
                val idx1 = k * ido
                val idx2 = idx1 * ip
                for (i in 0 until ido) {
                    `in`[in_off + i + idx2] = out[out_off + i + idx1]
                }
            }
        } else {
            for (i in 0 until ido) {
                for (k in 0 until l1) {
                    val idx1 = k * ido
                    `in`[in_off + i + idx1 * ip] = out[out_off + i + idx1]
                }
            }
        }
        val idx01 = ip * ido
        for (j in 1 until ipph) {
            jc = ip - j
            j2 = 2 * j
            val idx1 = j * l1 * ido
            val idx2 = jc * l1 * ido
            val idx3 = j2 * ido
            for (k in 0 until l1) {
                val idx4 = k * ido
                val idx5 = idx4 + idx1
                val idx6 = idx4 + idx2
                val idx7 = k * idx01
                `in`[in_off + ido - 1 + idx3 - ido + idx7] = out[out_off + idx5]
                `in`[in_off + idx3 + idx7] = out[out_off + idx6]
            }
        }
        if (ido == 1) return
        if (nbd >= l1) {
            for (j in 1 until ipph) {
                jc = ip - j
                j2 = 2 * j
                val idx1 = j * l1 * ido
                val idx2 = jc * l1 * ido
                val idx3 = j2 * ido
                for (k in 0 until l1) {
                    val idx4 = k * idx01
                    val idx5 = k * ido
                    var i = 2
                    while (i < ido) {
                        ic = ido - i
                        val idx6 = in_off + i
                        val idx7 = in_off + ic
                        val idx8 = out_off + i
                        val iidx1 = idx6 + idx3 + idx4
                        val iidx2 = idx7 + idx3 - ido + idx4
                        val oidx1 = idx8 + idx5 + idx1
                        val oidx2 = idx8 + idx5 + idx2
                        val o1i = out[oidx1 - 1]
                        val o1r = out[oidx1]
                        val o2i = out[oidx2 - 1]
                        val o2r = out[oidx2]
                        `in`[iidx1 - 1] = o1i + o2i
                        `in`[iidx2 - 1] = o1i - o2i
                        `in`[iidx1] = o1r + o2r
                        `in`[iidx2] = o2r - o1r
                        i += 2
                    }
                }
            }
        } else {
            for (j in 1 until ipph) {
                jc = ip - j
                j2 = 2 * j
                val idx1 = j * l1 * ido
                val idx2 = jc * l1 * ido
                val idx3 = j2 * ido
                var i = 2
                while (i < ido) {
                    ic = ido - i
                    val idx6 = in_off + i
                    val idx7 = in_off + ic
                    val idx8 = out_off + i
                    for (k in 0 until l1) {
                        val idx4 = k * idx01
                        val idx5 = k * ido
                        val iidx1 = idx6 + idx3 + idx4
                        val iidx2 = idx7 + idx3 - ido + idx4
                        val oidx1 = idx8 + idx5 + idx1
                        val oidx2 = idx8 + idx5 + idx2
                        val o1i = out[oidx1 - 1]
                        val o1r = out[oidx1]
                        val o2i = out[oidx2 - 1]
                        val o2r = out[oidx2]
                        `in`[iidx1 - 1] = o1i + o2i
                        `in`[iidx2 - 1] = o1i - o2i
                        `in`[iidx1] = o1r + o2r
                        `in`[iidx2] = o2r - o1r
                    }
                    i += 2
                }
            }
        }
    }

    /*---------------------------------------------------------
       radbg: Real FFT's backward processing of general factor
      --------------------------------------------------------*/
    fun radbg(
        ido: Int,
        ip: Int,
        l1: Int,
        idl1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int
    ) {
        var idij: Int
        var j2: Int
        var ic: Int
        var jc: Int
        var lc: Int
        var `is`: Int
        var dc2: Float
        var ai1: Float
        var ai2: Float
        var ar1: Float
        var ar2: Float
        var ds2: Float
        var w1r: Float
        var w1i: Float
        val dcp: Float
        val arg: Float
        val dsp: Float
        var ar1h: Float
        var ar2h: Float
        arg = TWO_PI / ip.toFloat()
        dcp = cos(arg.toDouble()).toFloat()
        dsp = sin(arg.toDouble()).toFloat()
        val nbd: Int = (ido - 1) / 2
        val ipph: Int = (ip + 1) / 2
        val idx0 = ip * ido
        if (ido >= l1) {
            for (k in 0 until l1) {
                val idx1 = k * ido
                val idx2 = k * idx0
                for (i in 0 until ido) {
                    out[out_off + i + idx1] = `in`[in_off + i + idx2]
                }
            }
        } else {
            for (i in 0 until ido) {
                val idx1 = out_off + i
                val idx2 = in_off + i
                for (k in 0 until l1) {
                    out[idx1 + k * ido] = `in`[idx2 + k * idx0]
                }
            }
        }
        val iidx0 = in_off + ido - 1
        for (j in 1 until ipph) {
            jc = ip - j
            j2 = 2 * j
            val idx1 = j * l1 * ido
            val idx2 = jc * l1 * ido
            val idx3 = j2 * ido
            for (k in 0 until l1) {
                val idx4 = k * ido
                val idx5 = idx4 * ip
                val iidx1 = iidx0 + idx3 + idx5 - ido
                val iidx2 = in_off + idx3 + idx5
                val i1r = `in`[iidx1]
                val i2r = `in`[iidx2]
                out[out_off + idx4 + idx1] = i1r + i1r
                out[out_off + idx4 + idx2] = i2r + i2r
            }
        }
        if (ido != 1) {
            if (nbd >= l1) {
                for (j in 1 until ipph) {
                    jc = ip - j
                    val idx1 = j * l1 * ido
                    val idx2 = jc * l1 * ido
                    val idx3 = 2 * j * ido
                    for (k in 0 until l1) {
                        val idx4 = k * ido + idx1
                        val idx5 = k * ido + idx2
                        val idx6 = k * ip * ido + idx3
                        var i = 2
                        while (i < ido) {
                            ic = ido - i
                            val idx7 = out_off + i
                            val idx8 = in_off + ic
                            val idx9 = in_off + i
                            val oidx1 = idx7 + idx4
                            val oidx2 = idx7 + idx5
                            val iidx1 = idx9 + idx6
                            val iidx2 = idx8 + idx6 - ido
                            val a1i = `in`[iidx1 - 1]
                            val a1r = `in`[iidx1]
                            val a2i = `in`[iidx2 - 1]
                            val a2r = `in`[iidx2]
                            out[oidx1 - 1] = a1i + a2i
                            out[oidx2 - 1] = a1i - a2i
                            out[oidx1] = a1r - a2r
                            out[oidx2] = a1r + a2r
                            i += 2
                        }
                    }
                }
            } else {
                for (j in 1 until ipph) {
                    jc = ip - j
                    val idx1 = j * l1 * ido
                    val idx2 = jc * l1 * ido
                    val idx3 = 2 * j * ido
                    var i = 2
                    while (i < ido) {
                        ic = ido - i
                        val idx7 = out_off + i
                        val idx8 = in_off + ic
                        val idx9 = in_off + i
                        for (k in 0 until l1) {
                            val idx4 = k * ido + idx1
                            val idx5 = k * ido + idx2
                            val idx6 = k * ip * ido + idx3
                            val oidx1 = idx7 + idx4
                            val oidx2 = idx7 + idx5
                            val iidx1 = idx9 + idx6
                            val iidx2 = idx8 + idx6 - ido
                            val a1i = `in`[iidx1 - 1]
                            val a1r = `in`[iidx1]
                            val a2i = `in`[iidx2 - 1]
                            val a2r = `in`[iidx2]
                            out[oidx1 - 1] = a1i + a2i
                            out[oidx2 - 1] = a1i - a2i
                            out[oidx1] = a1r - a2r
                            out[oidx2] = a1r + a2r
                        }
                        i += 2
                    }
                }
            }
        }
        ar1 = 1f
        ai1 = 0f
        val idx01 = (ip - 1) * idl1
        for (l in 1 until ipph) {
            lc = ip - l
            ar1h = dcp * ar1 - dsp * ai1
            ai1 = dcp * ai1 + dsp * ar1
            ar1 = ar1h
            val idx1 = l * idl1
            val idx2 = lc * idl1
            for (ik in 0 until idl1) {
                val idx3 = in_off + ik
                val idx4 = out_off + ik
                `in`[idx3 + idx1] = out[idx4] + ar1 * out[idx4 + idl1]
                `in`[idx3 + idx2] = ai1 * out[idx4 + idx01]
            }
            dc2 = ar1
            ds2 = ai1
            ar2 = ar1
            ai2 = ai1
            for (j in 2 until ipph) {
                jc = ip - j
                ar2h = dc2 * ar2 - ds2 * ai2
                ai2 = dc2 * ai2 + ds2 * ar2
                ar2 = ar2h
                val idx5 = j * idl1
                val idx6 = jc * idl1
                for (ik in 0 until idl1) {
                    val idx7 = in_off + ik
                    val idx8 = out_off + ik
                    `in`[idx7 + idx1] += ar2 * out[idx8 + idx5]
                    `in`[idx7 + idx2] += ai2 * out[idx8 + idx6]
                }
            }
        }
        for (j in 1 until ipph) {
            val idx1 = j * idl1
            for (ik in 0 until idl1) {
                val idx2 = out_off + ik
                out[idx2] += out[idx2 + idx1]
            }
        }
        for (j in 1 until ipph) {
            jc = ip - j
            val idx1 = j * l1 * ido
            val idx2 = jc * l1 * ido
            for (k in 0 until l1) {
                val idx3 = k * ido
                val oidx1 = out_off + idx3
                val iidx1 = in_off + idx3 + idx1
                val iidx2 = in_off + idx3 + idx2
                val i1r = `in`[iidx1]
                val i2r = `in`[iidx2]
                out[oidx1 + idx1] = i1r - i2r
                out[oidx1 + idx2] = i1r + i2r
            }
        }
        if (ido == 1) return
        if (nbd >= l1) {
            for (j in 1 until ipph) {
                jc = ip - j
                val idx1 = j * l1 * ido
                val idx2 = jc * l1 * ido
                for (k in 0 until l1) {
                    val idx3 = k * ido
                    var i = 2
                    while (i < ido) {
                        val idx4 = out_off + i
                        val idx5 = in_off + i
                        val oidx1 = idx4 + idx3 + idx1
                        val oidx2 = idx4 + idx3 + idx2
                        val iidx1 = idx5 + idx3 + idx1
                        val iidx2 = idx5 + idx3 + idx2
                        val i1i = `in`[iidx1 - 1]
                        val i1r = `in`[iidx1]
                        val i2i = `in`[iidx2 - 1]
                        val i2r = `in`[iidx2]
                        out[oidx1 - 1] = i1i - i2r
                        out[oidx2 - 1] = i1i + i2r
                        out[oidx1] = i1r + i2i
                        out[oidx2] = i1r - i2i
                        i += 2
                    }
                }
            }
        } else {
            for (j in 1 until ipph) {
                jc = ip - j
                val idx1 = j * l1 * ido
                val idx2 = jc * l1 * ido
                var i = 2
                while (i < ido) {
                    val idx4 = out_off + i
                    val idx5 = in_off + i
                    for (k in 0 until l1) {
                        val idx3 = k * ido
                        val oidx1 = idx4 + idx3 + idx1
                        val oidx2 = idx4 + idx3 + idx2
                        val iidx1 = idx5 + idx3 + idx1
                        val iidx2 = idx5 + idx3 + idx2
                        val i1i = `in`[iidx1 - 1]
                        val i1r = `in`[iidx1]
                        val i2i = `in`[iidx2 - 1]
                        val i2r = `in`[iidx2]
                        out[oidx1 - 1] = i1i - i2r
                        out[oidx2 - 1] = i1i + i2r
                        out[oidx1] = i1r + i2i
                        out[oidx2] = i1r - i2i
                    }
                    i += 2
                }
            }
        }
        System.arraycopy(out, out_off, `in`, in_off, idl1)
        for (j in 1 until ip) {
            val idx1 = j * l1 * ido
            for (k in 0 until l1) {
                val idx2 = k * ido + idx1
                `in`[in_off + idx2] = out[out_off + idx2]
            }
        }
        if (nbd <= l1) {
            `is` = -ido
            for (j in 1 until ip) {
                `is` += ido
                idij = `is` - 1
                val idx1 = j * l1 * ido
                var i = 2
                while (i < ido) {
                    idij += 2
                    val idx2 = idij + offset
                    w1r = wtableR[idx2 - 1]
                    w1i = wtableR[idx2]
                    val idx4 = in_off + i
                    val idx5 = out_off + i
                    for (k in 0 until l1) {
                        val idx3 = k * ido + idx1
                        val iidx1 = idx4 + idx3
                        val oidx1 = idx5 + idx3
                        val o1i = out[oidx1 - 1]
                        val o1r = out[oidx1]
                        `in`[iidx1 - 1] = w1r * o1i - w1i * o1r
                        `in`[iidx1] = w1r * o1r + w1i * o1i
                    }
                    i += 2
                }
            }
        } else {
            `is` = -ido
            for (j in 1 until ip) {
                `is` += ido
                val idx1 = j * l1 * ido
                for (k in 0 until l1) {
                    idij = `is` - 1
                    val idx3 = k * ido + idx1
                    var i = 2
                    while (i < ido) {
                        idij += 2
                        val idx2 = idij + offset
                        w1r = wtableR[idx2 - 1]
                        w1i = wtableR[idx2]
                        val idx4 = in_off + i
                        val idx5 = out_off + i
                        val iidx1 = idx4 + idx3
                        val oidx1 = idx5 + idx3
                        val o1i = out[oidx1 - 1]
                        val o1r = out[oidx1]
                        `in`[iidx1 - 1] = w1r * o1i - w1i * o1r
                        `in`[iidx1] = w1r * o1r + w1i * o1i
                        i += 2
                    }
                }
            }
        }
    }

    /*---------------------------------------------------------
       cfftf1: further processing of Complex forward FFT
      --------------------------------------------------------*/
    fun cfftf(a: FloatArray, offa: Int, isign: Int) {
        var idot: Int
        var l1: Int
        var l2: Int
        var na: Int
        val nf: Int
        var ip: Int
        var iw: Int
        var ido: Int
        var idl1: Int
        val nac = IntArray(1)
        val twon = 2 * n
        val iw1: Int
        val ch = FloatArray(twon)
        iw1 = twon
        val iw2: Int = 4 * n
        nac[0] = 0
        nf = wtable[1 + iw2].toInt()
        na = 0
        l1 = 1
        iw = iw1
        for (k1 in 2..nf + 1) {
            ip = wtable[k1 + iw2].toInt()
            l2 = ip * l1
            ido = n / l2
            idot = ido + ido
            idl1 = idot * l1
            when (ip) {
                4 -> {
                    if (na == 0) {
                        passf4(idot, l1, a, offa, ch, 0, iw, isign)
                    } else {
                        passf4(idot, l1, ch, 0, a, offa, iw, isign)
                    }
                    na = 1 - na
                }
                2 -> {
                    if (na == 0) {
                        passf2(idot, l1, a, offa, ch, 0, iw, isign)
                    } else {
                        passf2(idot, l1, ch, 0, a, offa, iw, isign)
                    }
                    na = 1 - na
                }
                3 -> {
                    if (na == 0) {
                        passf3(idot, l1, a, offa, ch, 0, iw, isign)
                    } else {
                        passf3(idot, l1, ch, 0, a, offa, iw, isign)
                    }
                    na = 1 - na
                }
                5 -> {
                    if (na == 0) {
                        passf5(idot, l1, a, offa, ch, 0, iw, isign)
                    } else {
                        passf5(idot, l1, ch, 0, a, offa, iw, isign)
                    }
                    na = 1 - na
                }
                else -> {
                    if (na == 0) {
                        passfg(nac, idot, ip, l1, idl1, a, offa, ch, 0, iw, isign)
                    } else {
                        passfg(nac, idot, ip, l1, idl1, ch, 0, a, offa, iw, isign)
                    }
                    if (nac[0] != 0) na = 1 - na
                }
            }
            l1 = l2
            iw += (ip - 1) * idot
        }
        if (na == 0) return
        System.arraycopy(ch, 0, a, offa, twon)
    }

    fun passf2(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int,
        isign: Int
    ) {
        var t1i: Float
        var t1r: Float
        val iw1: Int = offset
        val idx = ido * l1
        if (ido <= 2) {
            for (k in 0 until l1) {
                val idx0 = k * ido
                val iidx1 = in_off + 2 * idx0
                val iidx2 = iidx1 + ido
                val a1r = `in`[iidx1]
                val a1i = `in`[iidx1 + 1]
                val a2r = `in`[iidx2]
                val a2i = `in`[iidx2 + 1]
                val oidx1 = out_off + idx0
                val oidx2 = oidx1 + idx
                out[oidx1] = a1r + a2r
                out[oidx1 + 1] = a1i + a2i
                out[oidx2] = a1r - a2r
                out[oidx2 + 1] = a1i - a2i
            }
        } else {
            for (k in 0 until l1) {
                var i = 0
                while (i < ido - 1) {
                    val idx0 = k * ido
                    val iidx1 = in_off + i + 2 * idx0
                    val iidx2 = iidx1 + ido
                    val i1r = `in`[iidx1]
                    val i1i = `in`[iidx1 + 1]
                    val i2r = `in`[iidx2]
                    val i2i = `in`[iidx2 + 1]
                    val widx1 = i + iw1
                    val w1r = wtable[widx1]
                    val w1i = isign * wtable[widx1 + 1]
                    t1r = i1r - i2r
                    t1i = i1i - i2i
                    val oidx1 = out_off + i + idx0
                    val oidx2 = oidx1 + idx
                    out[oidx1] = i1r + i2r
                    out[oidx1 + 1] = i1i + i2i
                    out[oidx2] = w1r * t1r - w1i * t1i
                    out[oidx2 + 1] = w1r * t1i + w1i * t1r
                    i += 2
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf2: Complex FFT's forward/backward processing of factor 2;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
/*----------------------------------------------------------------------
       passf3: Complex FFT's forward/backward processing of factor 3;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    fun passf3(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int,
        isign: Int
    ) {
        val taur = -0.5f
        val taui = 0.866025403784438707610604524234076962f
        var ci2: Float
        var ci3: Float
        var di2: Float
        var di3: Float
        var cr2: Float
        var cr3: Float
        var dr2: Float
        var dr3: Float
        var ti2: Float
        var tr2: Float
        val iw2: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        val idxt = l1 * ido
        if (ido == 2) {
            for (k in 1..l1) {
                val iidx1 = in_off + (3 * k - 2) * ido
                val iidx2 = iidx1 + ido
                val iidx3 = iidx1 - ido
                val i1r = `in`[iidx1]
                val i1i = `in`[iidx1 + 1]
                val i2r = `in`[iidx2]
                val i2i = `in`[iidx2 + 1]
                val i3r = `in`[iidx3]
                val i3i = `in`[iidx3 + 1]
                tr2 = i1r + i2r
                cr2 = i3r + taur * tr2
                ti2 = i1i + i2i
                ci2 = i3i + taur * ti2
                cr3 = isign * taui * (i1r - i2r)
                ci3 = isign * taui * (i1i - i2i)
                val oidx1 = out_off + (k - 1) * ido
                val oidx2 = oidx1 + idxt
                val oidx3 = oidx2 + idxt
                out[oidx1] = `in`[iidx3] + tr2
                out[oidx1 + 1] = i3i + ti2
                out[oidx2] = cr2 - ci3
                out[oidx2 + 1] = ci2 + cr3
                out[oidx3] = cr2 + ci3
                out[oidx3 + 1] = ci2 - cr3
            }
        } else {
            for (k in 1..l1) {
                val idx1 = in_off + (3 * k - 2) * ido
                val idx2 = out_off + (k - 1) * ido
                var i = 0
                while (i < ido - 1) {
                    val iidx1 = i + idx1
                    val iidx2 = iidx1 + ido
                    val iidx3 = iidx1 - ido
                    val a1r = `in`[iidx1]
                    val a1i = `in`[iidx1 + 1]
                    val a2r = `in`[iidx2]
                    val a2i = `in`[iidx2 + 1]
                    val a3r = `in`[iidx3]
                    val a3i = `in`[iidx3 + 1]
                    tr2 = a1r + a2r
                    cr2 = a3r + taur * tr2
                    ti2 = a1i + a2i
                    ci2 = a3i + taur * ti2
                    cr3 = isign * taui * (a1r - a2r)
                    ci3 = isign * taui * (a1i - a2i)
                    dr2 = cr2 - ci3
                    dr3 = cr2 + ci3
                    di2 = ci2 + cr3
                    di3 = ci2 - cr3
                    val widx1 = i + iw1
                    val widx2 = i + iw2
                    val w1r = wtable[widx1]
                    val w1i = isign * wtable[widx1 + 1]
                    val w2r = wtable[widx2]
                    val w2i = isign * wtable[widx2 + 1]
                    val oidx1 = i + idx2
                    val oidx2 = oidx1 + idxt
                    val oidx3 = oidx2 + idxt
                    out[oidx1] = a3r + tr2
                    out[oidx1 + 1] = a3i + ti2
                    out[oidx2] = w1r * dr2 - w1i * di2
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2
                    out[oidx3] = w2r * dr3 - w2i * di3
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3
                    i += 2
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf4: Complex FFT's forward/backward processing of factor 4;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    fun passf4(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int,
        isign: Int
    ) {
        var ci2: Float
        var ci3: Float
        var ci4: Float
        var cr2: Float
        var cr3: Float
        var cr4: Float
        var ti1: Float
        var ti2: Float
        var ti3: Float
        var ti4: Float
        var tr1: Float
        var tr2: Float
        var tr3: Float
        var tr4: Float
        val iw2: Int
        val iw3: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        iw3 = iw2 + ido
        val idx0 = l1 * ido
        if (ido == 2) {
            for (k in 0 until l1) {
                val idxt1 = k * ido
                val iidx1 = in_off + 4 * idxt1 + 1
                val iidx2 = iidx1 + ido
                val iidx3 = iidx2 + ido
                val iidx4 = iidx3 + ido
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val i3i = `in`[iidx3 - 1]
                val i3r = `in`[iidx3]
                val i4i = `in`[iidx4 - 1]
                val i4r = `in`[iidx4]
                ti1 = i1r - i3r
                ti2 = i1r + i3r
                tr4 = i4r - i2r
                ti3 = i2r + i4r
                tr1 = i1i - i3i
                tr2 = i1i + i3i
                ti4 = i2i - i4i
                tr3 = i2i + i4i
                val oidx1 = out_off + idxt1
                val oidx2 = oidx1 + idx0
                val oidx3 = oidx2 + idx0
                val oidx4 = oidx3 + idx0
                out[oidx1] = tr2 + tr3
                out[oidx1 + 1] = ti2 + ti3
                out[oidx2] = tr1 + isign * tr4
                out[oidx2 + 1] = ti1 + isign * ti4
                out[oidx3] = tr2 - tr3
                out[oidx3 + 1] = ti2 - ti3
                out[oidx4] = tr1 - isign * tr4
                out[oidx4 + 1] = ti1 - isign * ti4
            }
        } else {
            for (k in 0 until l1) {
                val idx1 = k * ido
                val idx2 = in_off + 1 + 4 * idx1
                var i = 0
                while (i < ido - 1) {
                    val iidx1 = i + idx2
                    val iidx2 = iidx1 + ido
                    val iidx3 = iidx2 + ido
                    val iidx4 = iidx3 + ido
                    val i1i = `in`[iidx1 - 1]
                    val i1r = `in`[iidx1]
                    val i2i = `in`[iidx2 - 1]
                    val i2r = `in`[iidx2]
                    val i3i = `in`[iidx3 - 1]
                    val i3r = `in`[iidx3]
                    val i4i = `in`[iidx4 - 1]
                    val i4r = `in`[iidx4]
                    ti1 = i1r - i3r
                    ti2 = i1r + i3r
                    ti3 = i2r + i4r
                    tr4 = i4r - i2r
                    tr1 = i1i - i3i
                    tr2 = i1i + i3i
                    ti4 = i2i - i4i
                    tr3 = i2i + i4i
                    cr3 = tr2 - tr3
                    ci3 = ti2 - ti3
                    cr2 = tr1 + isign * tr4
                    cr4 = tr1 - isign * tr4
                    ci2 = ti1 + isign * ti4
                    ci4 = ti1 - isign * ti4
                    val widx1 = i + iw1
                    val widx2 = i + iw2
                    val widx3 = i + iw3
                    val w1r = wtable[widx1]
                    val w1i = isign * wtable[widx1 + 1]
                    val w2r = wtable[widx2]
                    val w2i = isign * wtable[widx2 + 1]
                    val w3r = wtable[widx3]
                    val w3i = isign * wtable[widx3 + 1]
                    val oidx1 = out_off + i + idx1
                    val oidx2 = oidx1 + idx0
                    val oidx3 = oidx2 + idx0
                    val oidx4 = oidx3 + idx0
                    out[oidx1] = tr2 + tr3
                    out[oidx1 + 1] = ti2 + ti3
                    out[oidx2] = w1r * cr2 - w1i * ci2
                    out[oidx2 + 1] = w1r * ci2 + w1i * cr2
                    out[oidx3] = w2r * cr3 - w2i * ci3
                    out[oidx3 + 1] = w2r * ci3 + w2i * cr3
                    out[oidx4] = w3r * cr4 - w3i * ci4
                    out[oidx4 + 1] = w3r * ci4 + w3i * cr4
                    i += 2
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passf5: Complex FFT's forward/backward processing of factor 5;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    fun passf5(
        ido: Int,
        l1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int,
        isign: Int
    ) /* isign==-1 for forward transform and+1 for backward transform */ {
        val tr11 = 0.309016994374947451262869435595348477f
        val ti11 = 0.951056516295153531181938433292089030f
        val tr12 = -0.809016994374947340240566973079694435f
        val ti12 = 0.587785252292473248125759255344746634f
        var ci2: Float
        var ci3: Float
        var ci4: Float
        var ci5: Float
        var di3: Float
        var di4: Float
        var di5: Float
        var di2: Float
        var cr2: Float
        var cr3: Float
        var cr5: Float
        var cr4: Float
        var ti2: Float
        var ti3: Float
        var ti4: Float
        var ti5: Float
        var dr3: Float
        var dr4: Float
        var dr5: Float
        var dr2: Float
        var tr2: Float
        var tr3: Float
        var tr4: Float
        var tr5: Float
        val iw2: Int
        val iw3: Int
        val iw4: Int
        val iw1: Int = offset
        iw2 = iw1 + ido
        iw3 = iw2 + ido
        iw4 = iw3 + ido
        val idx0 = l1 * ido
        if (ido == 2) {
            for (k in 1..l1) {
                val iidx1 = in_off + (5 * k - 4) * ido + 1
                val iidx2 = iidx1 + ido
                val iidx3 = iidx1 - ido
                val iidx4 = iidx2 + ido
                val iidx5 = iidx4 + ido
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val i3i = `in`[iidx3 - 1]
                val i3r = `in`[iidx3]
                val i4i = `in`[iidx4 - 1]
                val i4r = `in`[iidx4]
                val i5i = `in`[iidx5 - 1]
                val i5r = `in`[iidx5]
                ti5 = i1r - i5r
                ti2 = i1r + i5r
                ti4 = i2r - i4r
                ti3 = i2r + i4r
                tr5 = i1i - i5i
                tr2 = i1i + i5i
                tr4 = i2i - i4i
                tr3 = i2i + i4i
                cr2 = i3i + tr11 * tr2 + tr12 * tr3
                ci2 = i3r + tr11 * ti2 + tr12 * ti3
                cr3 = i3i + tr12 * tr2 + tr11 * tr3
                ci3 = i3r + tr12 * ti2 + tr11 * ti3
                cr5 = isign * (ti11 * tr5 + ti12 * tr4)
                ci5 = isign * (ti11 * ti5 + ti12 * ti4)
                cr4 = isign * (ti12 * tr5 - ti11 * tr4)
                ci4 = isign * (ti12 * ti5 - ti11 * ti4)
                val oidx1 = out_off + (k - 1) * ido
                val oidx2 = oidx1 + idx0
                val oidx3 = oidx2 + idx0
                val oidx4 = oidx3 + idx0
                val oidx5 = oidx4 + idx0
                out[oidx1] = i3i + tr2 + tr3
                out[oidx1 + 1] = i3r + ti2 + ti3
                out[oidx2] = cr2 - ci5
                out[oidx2 + 1] = ci2 + cr5
                out[oidx3] = cr3 - ci4
                out[oidx3 + 1] = ci3 + cr4
                out[oidx4] = cr3 + ci4
                out[oidx4 + 1] = ci3 - cr4
                out[oidx5] = cr2 + ci5
                out[oidx5 + 1] = ci2 - cr5
            }
        } else {
            for (k in 1..l1) {
                val idx1 = in_off + 1 + (k * 5 - 4) * ido
                val idx2 = out_off + (k - 1) * ido
                var i = 0
                while (i < ido - 1) {
                    val iidx1 = i + idx1
                    val iidx2 = iidx1 + ido
                    val iidx3 = iidx1 - ido
                    val iidx4 = iidx2 + ido
                    val iidx5 = iidx4 + ido
                    val i1i = `in`[iidx1 - 1]
                    val i1r = `in`[iidx1]
                    val i2i = `in`[iidx2 - 1]
                    val i2r = `in`[iidx2]
                    val i3i = `in`[iidx3 - 1]
                    val i3r = `in`[iidx3]
                    val i4i = `in`[iidx4 - 1]
                    val i4r = `in`[iidx4]
                    val i5i = `in`[iidx5 - 1]
                    val i5r = `in`[iidx5]
                    ti5 = i1r - i5r
                    ti2 = i1r + i5r
                    ti4 = i2r - i4r
                    ti3 = i2r + i4r
                    tr5 = i1i - i5i
                    tr2 = i1i + i5i
                    tr4 = i2i - i4i
                    tr3 = i2i + i4i
                    cr2 = i3i + tr11 * tr2 + tr12 * tr3
                    ci2 = i3r + tr11 * ti2 + tr12 * ti3
                    cr3 = i3i + tr12 * tr2 + tr11 * tr3
                    ci3 = i3r + tr12 * ti2 + tr11 * ti3
                    cr5 = isign * (ti11 * tr5 + ti12 * tr4)
                    ci5 = isign * (ti11 * ti5 + ti12 * ti4)
                    cr4 = isign * (ti12 * tr5 - ti11 * tr4)
                    ci4 = isign * (ti12 * ti5 - ti11 * ti4)
                    dr3 = cr3 - ci4
                    dr4 = cr3 + ci4
                    di3 = ci3 + cr4
                    di4 = ci3 - cr4
                    dr5 = cr2 + ci5
                    dr2 = cr2 - ci5
                    di5 = ci2 - cr5
                    di2 = ci2 + cr5
                    val widx1 = i + iw1
                    val widx2 = i + iw2
                    val widx3 = i + iw3
                    val widx4 = i + iw4
                    val w1r = wtable[widx1]
                    val w1i = isign * wtable[widx1 + 1]
                    val w2r = wtable[widx2]
                    val w2i = isign * wtable[widx2 + 1]
                    val w3r = wtable[widx3]
                    val w3i = isign * wtable[widx3 + 1]
                    val w4r = wtable[widx4]
                    val w4i = isign * wtable[widx4 + 1]
                    val oidx1 = i + idx2
                    val oidx2 = oidx1 + idx0
                    val oidx3 = oidx2 + idx0
                    val oidx4 = oidx3 + idx0
                    val oidx5 = oidx4 + idx0
                    out[oidx1] = i3i + tr2 + tr3
                    out[oidx1 + 1] = i3r + ti2 + ti3
                    out[oidx2] = w1r * dr2 - w1i * di2
                    out[oidx2 + 1] = w1r * di2 + w1i * dr2
                    out[oidx3] = w2r * dr3 - w2i * di3
                    out[oidx3 + 1] = w2r * di3 + w2i * dr3
                    out[oidx4] = w3r * dr4 - w3i * di4
                    out[oidx4 + 1] = w3r * di4 + w3i * dr4
                    out[oidx5] = w4r * dr5 - w4i * di5
                    out[oidx5 + 1] = w4r * di5 + w4i * dr5
                    i += 2
                }
            }
        }
    }

    /*----------------------------------------------------------------------
       passfg: Complex FFT's forward/backward processing of general factor;
       isign is +1 for backward and -1 for forward transforms
      ----------------------------------------------------------------------*/
    fun passfg(
        nac: IntArray,
        ido: Int,
        ip: Int,
        l1: Int,
        idl1: Int,
        `in`: FloatArray,
        in_off: Int,
        out: FloatArray,
        out_off: Int,
        offset: Int,
        isign: Int
    ) {
        var idij: Int
        var idlj: Int
        var jc: Int
        var lc: Int
        var idj: Int
        var w1r: Float
        var w1i: Float
        var w2i: Float
        var w2r: Float
        val iw1: Int = offset
        val idot: Int = ido / 2
        val ipph: Int = (ip + 1) / 2
        val idp: Int = ip * ido
        if (ido >= l1) {
            for (j in 1 until ipph) {
                jc = ip - j
                val idx1 = j * ido
                val idx2 = jc * ido
                for (k in 0 until l1) {
                    val idx3 = k * ido
                    val idx4 = idx3 + idx1 * l1
                    val idx5 = idx3 + idx2 * l1
                    val idx6 = idx3 * ip
                    for (i in 0 until ido) {
                        val oidx1 = out_off + i
                        val i1r = `in`[in_off + i + idx1 + idx6]
                        val i2r = `in`[in_off + i + idx2 + idx6]
                        out[oidx1 + idx4] = i1r + i2r
                        out[oidx1 + idx5] = i1r - i2r
                    }
                }
            }
            for (k in 0 until l1) {
                val idxt1 = k * ido
                val idxt2 = idxt1 * ip
                for (i in 0 until ido) {
                    out[out_off + i + idxt1] = `in`[in_off + i + idxt2]
                }
            }
        } else {
            for (j in 1 until ipph) {
                jc = ip - j
                val idxt1 = j * l1 * ido
                val idxt2 = jc * l1 * ido
                val idxt3 = j * ido
                val idxt4 = jc * ido
                for (i in 0 until ido) {
                    for (k in 0 until l1) {
                        val idx1 = k * ido
                        val idx2 = idx1 * ip
                        val idx3 = out_off + i
                        val idx4 = in_off + i
                        val i1r = `in`[idx4 + idxt3 + idx2]
                        val i2r = `in`[idx4 + idxt4 + idx2]
                        out[idx3 + idx1 + idxt1] = i1r + i2r
                        out[idx3 + idx1 + idxt2] = i1r - i2r
                    }
                }
            }
            for (i in 0 until ido) {
                for (k in 0 until l1) {
                    val idx1 = k * ido
                    out[out_off + i + idx1] = `in`[in_off + i + idx1 * ip]
                }
            }
        }
        var idl: Int = 2 - ido
        var inc: Int = 0
        val idxt0 = (ip - 1) * idl1
        var l: Int = 1
        while (l < ipph) {
            lc = ip - l
            idl += ido
            val idxt1 = l * idl1
            val idxt2 = lc * idl1
            val idxt3 = idl + iw1
            w1r = wtable[idxt3 - 2]
            w1i = isign * wtable[idxt3 - 1]
            for (ik in 0 until idl1) {
                val idx1 = in_off + ik
                val idx2 = out_off + ik
                `in`[idx1 + idxt1] = out[idx2] + w1r * out[idx2 + idl1]
                `in`[idx1 + idxt2] = w1i * out[idx2 + idxt0]
            }
            idlj = idl
            inc += ido
            for (j in 2 until ipph) {
                jc = ip - j
                idlj += inc
                if (idlj > idp) idlj -= idp
                val idxt4 = idlj + iw1
                w2r = wtable[idxt4 - 2]
                w2i = isign * wtable[idxt4 - 1]
                val idxt5 = j * idl1
                val idxt6 = jc * idl1
                for (ik in 0 until idl1) {
                    val idx1 = in_off + ik
                    val idx2 = out_off + ik
                    `in`[idx1 + idxt1] += w2r * out[idx2 + idxt5]
                    `in`[idx1 + idxt2] += w2i * out[idx2 + idxt6]
                }
            }
            l++
        }
        for (j in 1 until ipph) {
            val idxt1 = j * idl1
            for (ik in 0 until idl1) {
                val idx1 = out_off + ik
                out[idx1] += out[idx1 + idxt1]
            }
        }
        for (j in 1 until ipph) {
            jc = ip - j
            val idx1 = j * idl1
            val idx2 = jc * idl1
            var ik = 1
            while (ik < idl1) {
                val idx3 = out_off + ik
                val idx4 = in_off + ik
                val iidx1 = idx4 + idx1
                val iidx2 = idx4 + idx2
                val i1i = `in`[iidx1 - 1]
                val i1r = `in`[iidx1]
                val i2i = `in`[iidx2 - 1]
                val i2r = `in`[iidx2]
                val oidx1 = idx3 + idx1
                val oidx2 = idx3 + idx2
                out[oidx1 - 1] = i1i - i2r
                out[oidx2 - 1] = i1i + i2r
                out[oidx1] = i1r + i2i
                out[oidx2] = i1r - i2i
                ik += 2
            }
        }
        nac[0] = 1
        if (ido == 2) return
        nac[0] = 0
        System.arraycopy(out, out_off, `in`, in_off, idl1)
        val idx0 = l1 * ido
        for (j in 1 until ip) {
            val idx1 = j * idx0
            for (k in 0 until l1) {
                val idx2 = k * ido
                val oidx1 = out_off + idx2 + idx1
                val iidx1 = in_off + idx2 + idx1
                `in`[iidx1] = out[oidx1]
                `in`[iidx1 + 1] = out[oidx1 + 1]
            }
        }
        if (idot <= l1) {
            idij = 0
            for (j in 1 until ip) {
                idij += 2
                val idx1 = j * l1 * ido
                var i = 3
                while (i < ido) {
                    idij += 2
                    val idx2 = idij + iw1 - 1
                    w1r = wtable[idx2 - 1]
                    w1i = isign * wtable[idx2]
                    val idx3 = in_off + i
                    val idx4 = out_off + i
                    for (k in 0 until l1) {
                        val idx5 = k * ido + idx1
                        val iidx1 = idx3 + idx5
                        val oidx1 = idx4 + idx5
                        val o1i = out[oidx1 - 1]
                        val o1r = out[oidx1]
                        `in`[iidx1 - 1] = w1r * o1i - w1i * o1r
                        `in`[iidx1] = w1r * o1r + w1i * o1i
                    }
                    i += 2
                }
            }
        } else {
            idj = 2 - ido
            for (j in 1 until ip) {
                idj += ido
                val idx1 = j * l1 * ido
                for (k in 0 until l1) {
                    idij = idj
                    val idx3 = k * ido + idx1
                    var i = 3
                    while (i < ido) {
                        idij += 2
                        val idx2 = idij - 1 + iw1
                        w1r = wtable[idx2 - 1]
                        w1i = isign * wtable[idx2]
                        val iidx1 = in_off + i + idx3
                        val oidx1 = out_off + i + idx3
                        val o1i = out[oidx1 - 1]
                        val o1r = out[oidx1]
                        `in`[iidx1 - 1] = w1r * o1i - w1i * o1r
                        `in`[iidx1] = w1r * o1r + w1i * o1i
                        i += 2
                    }
                }
            }
        }
    }

    private fun cftfsub(
        n: Int,
        a: FloatArray,
        offa: Int,
        ip: IntArray,
        nw: Int,
        w: FloatArray
    ) {
        if (n > 8) {
            if (n > 32) {
                cftf1st(n, a, offa, w, nw - (n shr 2))
                if (ConcurrencyUtils.getNumberOfThreads() > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
                    cftrec4_th(n, a, offa, nw, w)
                } else if (n > 512) {
                    cftrec4(n, a, offa, nw, w)
                } else if (n > 128) {
                    cftleaf(n, 1, a, offa, nw, w)
                } else {
                    cftfx41(n, a, offa, nw, w)
                }
                bitrv2(n, ip, a, offa)
            } else if (n == 32) {
                cftf161(a, offa, w, nw - 8)
                bitrv216(a, offa)
            } else {
                cftf081(a, offa, w, 0)
                bitrv208(a, offa)
            }
        } else if (n == 8) {
            cftf040(a, offa)
        } else if (n == 4) {
            cftxb020(a, offa)
        }
    }

    private fun cftbsub(
        n: Int,
        a: FloatArray,
        offa: Int,
        ip: IntArray,
        nw: Int,
        w: FloatArray
    ) {
        if (n > 8) {
            if (n > 32) {
                cftb1st(n, a, offa, w, nw - (n shr 2))
                if (ConcurrencyUtils.getNumberOfThreads() > 1 && n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
                    cftrec4_th(n, a, offa, nw, w)
                } else if (n > 512) {
                    cftrec4(n, a, offa, nw, w)
                } else if (n > 128) {
                    cftleaf(n, 1, a, offa, nw, w)
                } else {
                    cftfx41(n, a, offa, nw, w)
                }
                bitrv2conj(n, ip, a, offa)
            } else if (n == 32) {
                cftf161(a, offa, w, nw - 8)
                bitrv216neg(a, offa)
            } else {
                cftf081(a, offa, w, 0)
                bitrv208neg(a, offa)
            }
        } else if (n == 8) {
            cftb040(a, offa)
        } else if (n == 4) {
            cftxb020(a, offa)
        }
    }

    private fun bitrv2(n: Int, ip: IntArray, a: FloatArray, offa: Int) {
        var j1: Int
        var k1: Int
        var l: Int
        var m: Int
        val nm: Int
        var xr: Float
        var xi: Float
        var yr: Float
        var yi: Float
        var idx0: Int
        var idx1: Int
        var idx2: Int
        m = 1
        l = n shr 2
        while (l > 8) {
            m = m shl 1
            l = l shr 2
        }
        val nh: Int = n shr 1
        nm = 4 * m
        if (l == 8) {
            for (k in 0 until m) {
                idx0 = 4 * k
                for (j in 0 until k) {
                    j1 = 4 * j + 2 * ip[m + k]
                    k1 = idx0 + 2 * ip[m + j]
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nh
                    k1 += 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += 2
                    k1 += nh
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nh
                    k1 -= 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                }
                k1 = idx0 + 2 * ip[m + k]
                j1 = k1 + 2
                k1 += nh
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 += nm
                k1 += 2 * nm
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 += nm
                k1 -= nm
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 -= 2
                k1 -= nh
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 += nh + 2
                k1 += nh + 2
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 -= nh - nm
                k1 += 2 * nm - 2
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
            }
        } else {
            for (k in 0 until m) {
                idx0 = 4 * k
                for (j in 0 until k) {
                    j1 = 4 * j + ip[m + k]
                    k1 = idx0 + ip[m + j]
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nh
                    k1 += 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += 2
                    k1 += nh
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nh
                    k1 -= 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = a[idx1 + 1]
                    yr = a[idx2]
                    yi = a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                }
                k1 = idx0 + ip[m + k]
                j1 = k1 + 2
                k1 += nh
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 += nm
                k1 += nm
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = a[idx1 + 1]
                yr = a[idx2]
                yi = a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
            }
        }
    }

    private fun bitrv2conj(n: Int, ip: IntArray, a: FloatArray, offa: Int) {
        var j1: Int
        var k1: Int
        var l: Int
        var m: Int
        val nm: Int
        var xr: Float
        var xi: Float
        var yr: Float
        var yi: Float
        var idx0: Int
        var idx1: Int
        var idx2: Int
        m = 1
        l = n shr 2
        while (l > 8) {
            m = m shl 1
            l = l shr 2
        }
        val nh: Int = n shr 1
        nm = 4 * m
        if (l == 8) {
            for (k in 0 until m) {
                idx0 = 4 * k
                for (j in 0 until k) {
                    j1 = 4 * j + 2 * ip[m + k]
                    k1 = idx0 + 2 * ip[m + j]
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nh
                    k1 += 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += 2
                    k1 += nh
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nh
                    k1 -= 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= 2 * nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                }
                k1 = idx0 + 2 * ip[m + k]
                j1 = k1 + 2
                k1 += nh
                idx1 = offa + j1
                idx2 = offa + k1
                a[idx1 - 1] = -a[idx1 - 1]
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                a[idx2 + 3] = -a[idx2 + 3]
                j1 += nm
                k1 += 2 * nm
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 += nm
                k1 -= nm
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 -= 2
                k1 -= nh
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 += nh + 2
                k1 += nh + 2
                idx1 = offa + j1
                idx2 = offa + k1
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                j1 -= nh - nm
                k1 += 2 * nm - 2
                idx1 = offa + j1
                idx2 = offa + k1
                a[idx1 - 1] = -a[idx1 - 1]
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                a[idx2 + 3] = -a[idx2 + 3]
            }
        } else {
            for (k in 0 until m) {
                idx0 = 4 * k
                for (j in 0 until k) {
                    j1 = 4 * j + ip[m + k]
                    k1 = idx0 + ip[m + j]
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nh
                    k1 += 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += 2
                    k1 += nh
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 += nm
                    k1 += nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nh
                    k1 -= 2
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                    j1 -= nm
                    k1 -= nm
                    idx1 = offa + j1
                    idx2 = offa + k1
                    xr = a[idx1]
                    xi = -a[idx1 + 1]
                    yr = a[idx2]
                    yi = -a[idx2 + 1]
                    a[idx1] = yr
                    a[idx1 + 1] = yi
                    a[idx2] = xr
                    a[idx2 + 1] = xi
                }
                k1 = idx0 + ip[m + k]
                j1 = k1 + 2
                k1 += nh
                idx1 = offa + j1
                idx2 = offa + k1
                a[idx1 - 1] = -a[idx1 - 1]
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                a[idx2 + 3] = -a[idx2 + 3]
                j1 += nm
                k1 += nm
                idx1 = offa + j1
                idx2 = offa + k1
                a[idx1 - 1] = -a[idx1 - 1]
                xr = a[idx1]
                xi = -a[idx1 + 1]
                yr = a[idx2]
                yi = -a[idx2 + 1]
                a[idx1] = yr
                a[idx1 + 1] = yi
                a[idx2] = xr
                a[idx2 + 1] = xi
                a[idx2 + 3] = -a[idx2 + 3]
            }
        }
    }

    private fun bitrv216(a: FloatArray, offa: Int) {
        val x1r: Float = a[offa + 2]
        val x1i: Float = a[offa + 3]
        val x2r: Float = a[offa + 4]
        val x2i: Float = a[offa + 5]
        val x3r: Float = a[offa + 6]
        val x3i: Float = a[offa + 7]
        val x4r: Float = a[offa + 8]
        val x4i: Float = a[offa + 9]
        val x5r: Float = a[offa + 10]
        val x5i: Float = a[offa + 11]
        val x7r: Float = a[offa + 14]
        val x7i: Float = a[offa + 15]
        val x8r: Float = a[offa + 16]
        val x8i: Float = a[offa + 17]
        val x10r: Float = a[offa + 20]
        val x10i: Float = a[offa + 21]
        val x11r: Float = a[offa + 22]
        val x11i: Float = a[offa + 23]
        val x12r: Float = a[offa + 24]
        val x12i: Float = a[offa + 25]
        val x13r: Float = a[offa + 26]
        val x13i: Float = a[offa + 27]
        val x14r: Float = a[offa + 28]
        val x14i: Float = a[offa + 29]
        a[offa + 2] = x8r
        a[offa + 3] = x8i
        a[offa + 4] = x4r
        a[offa + 5] = x4i
        a[offa + 6] = x12r
        a[offa + 7] = x12i
        a[offa + 8] = x2r
        a[offa + 9] = x2i
        a[offa + 10] = x10r
        a[offa + 11] = x10i
        a[offa + 14] = x14r
        a[offa + 15] = x14i
        a[offa + 16] = x1r
        a[offa + 17] = x1i
        a[offa + 20] = x5r
        a[offa + 21] = x5i
        a[offa + 22] = x13r
        a[offa + 23] = x13i
        a[offa + 24] = x3r
        a[offa + 25] = x3i
        a[offa + 26] = x11r
        a[offa + 27] = x11i
        a[offa + 28] = x7r
        a[offa + 29] = x7i
    }

    private fun bitrv216neg(a: FloatArray, offa: Int) {
        val x1r: Float = a[offa + 2]
        val x1i: Float = a[offa + 3]
        val x2r: Float = a[offa + 4]
        val x2i: Float = a[offa + 5]
        val x3r: Float = a[offa + 6]
        val x3i: Float = a[offa + 7]
        val x4r: Float = a[offa + 8]
        val x4i: Float = a[offa + 9]
        val x5r: Float = a[offa + 10]
        val x5i: Float = a[offa + 11]
        val x6r: Float = a[offa + 12]
        val x6i: Float = a[offa + 13]
        val x7r: Float = a[offa + 14]
        val x7i: Float = a[offa + 15]
        val x8r: Float = a[offa + 16]
        val x8i: Float = a[offa + 17]
        val x9r: Float = a[offa + 18]
        val x9i: Float = a[offa + 19]
        val x10r: Float = a[offa + 20]
        val x10i: Float = a[offa + 21]
        val x11r: Float = a[offa + 22]
        val x11i: Float = a[offa + 23]
        val x12r: Float = a[offa + 24]
        val x12i: Float = a[offa + 25]
        val x13r: Float = a[offa + 26]
        val x13i: Float = a[offa + 27]
        val x14r: Float = a[offa + 28]
        val x14i: Float = a[offa + 29]
        val x15r: Float = a[offa + 30]
        val x15i: Float = a[offa + 31]
        a[offa + 2] = x15r
        a[offa + 3] = x15i
        a[offa + 4] = x7r
        a[offa + 5] = x7i
        a[offa + 6] = x11r
        a[offa + 7] = x11i
        a[offa + 8] = x3r
        a[offa + 9] = x3i
        a[offa + 10] = x13r
        a[offa + 11] = x13i
        a[offa + 12] = x5r
        a[offa + 13] = x5i
        a[offa + 14] = x9r
        a[offa + 15] = x9i
        a[offa + 16] = x1r
        a[offa + 17] = x1i
        a[offa + 18] = x14r
        a[offa + 19] = x14i
        a[offa + 20] = x6r
        a[offa + 21] = x6i
        a[offa + 22] = x10r
        a[offa + 23] = x10i
        a[offa + 24] = x2r
        a[offa + 25] = x2i
        a[offa + 26] = x12r
        a[offa + 27] = x12i
        a[offa + 28] = x4r
        a[offa + 29] = x4i
        a[offa + 30] = x8r
        a[offa + 31] = x8i
    }

    private fun bitrv208(a: FloatArray, offa: Int) {
        val x1r: Float = a[offa + 2]
        val x1i: Float = a[offa + 3]
        val x3r: Float = a[offa + 6]
        val x3i: Float = a[offa + 7]
        val x4r: Float = a[offa + 8]
        val x4i: Float = a[offa + 9]
        val x6r: Float = a[offa + 12]
        val x6i: Float = a[offa + 13]
        a[offa + 2] = x4r
        a[offa + 3] = x4i
        a[offa + 6] = x6r
        a[offa + 7] = x6i
        a[offa + 8] = x1r
        a[offa + 9] = x1i
        a[offa + 12] = x3r
        a[offa + 13] = x3i
    }

    private fun bitrv208neg(a: FloatArray, offa: Int) {
        val x1r: Float = a[offa + 2]
        val x1i: Float = a[offa + 3]
        val x2r: Float = a[offa + 4]
        val x2i: Float = a[offa + 5]
        val x3r: Float = a[offa + 6]
        val x3i: Float = a[offa + 7]
        val x4r: Float = a[offa + 8]
        val x4i: Float = a[offa + 9]
        val x5r: Float = a[offa + 10]
        val x5i: Float = a[offa + 11]
        val x6r: Float = a[offa + 12]
        val x6i: Float = a[offa + 13]
        val x7r: Float = a[offa + 14]
        val x7i: Float = a[offa + 15]
        a[offa + 2] = x7r
        a[offa + 3] = x7i
        a[offa + 4] = x3r
        a[offa + 5] = x3i
        a[offa + 6] = x5r
        a[offa + 7] = x5i
        a[offa + 8] = x1r
        a[offa + 9] = x1i
        a[offa + 10] = x6r
        a[offa + 11] = x6i
        a[offa + 12] = x2r
        a[offa + 13] = x2i
        a[offa + 14] = x4r
        a[offa + 15] = x4i
    }

    private fun cftf1st(n: Int, a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        var j0: Int
        var j1: Int
        var j2: Int
        var j3: Int
        val m: Int
        var wk1r: Float
        var wk1i: Float
        var wk3r: Float
        var wk3i: Float
        var wd1r: Float
        var wd1i: Float
        var wd3r: Float
        var wd3i: Float
        var x0r: Float
        var x0i: Float
        var x1r: Float
        var x1i: Float
        var x2r: Float
        var x2i: Float
        var x3r: Float
        var x3i: Float
        var y0r: Float
        var y0i: Float
        var y1r: Float
        var y1i: Float
        var y2r: Float
        var y2i: Float
        var y3r: Float
        var y3i: Float
        var idx0: Int
        var idx1: Int
        var idx2: Int
        var idx3: Int
        var idx4: Int
        var idx5: Int
        val mh: Int = n shr 3
        m = 2 * mh
        j1 = m
        j2 = j1 + m
        j3 = j2 + m
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[offa] + a[idx2]
        x0i = a[offa + 1] + a[idx2 + 1]
        x1r = a[offa] - a[idx2]
        x1i = a[offa + 1] - a[idx2 + 1]
        x2r = a[idx1] + a[idx3]
        x2i = a[idx1 + 1] + a[idx3 + 1]
        x3r = a[idx1] - a[idx3]
        x3i = a[idx1 + 1] - a[idx3 + 1]
        a[offa] = x0r + x2r
        a[offa + 1] = x0i + x2i
        a[idx1] = x0r - x2r
        a[idx1 + 1] = x0i - x2i
        a[idx2] = x1r - x3i
        a[idx2 + 1] = x1i + x3r
        a[idx3] = x1r + x3i
        a[idx3 + 1] = x1i - x3r
        val wn4r: Float = w[startw + 1]
        val csc1: Float = w[startw + 2]
        val csc3: Float = w[startw + 3]
        wd1r = 1f
        wd1i = 0f
        wd3r = 1f
        wd3i = 0f
        var k: Int = 0
        var j = 2
        while (j < mh - 2) {
            k += 4
            idx4 = startw + k
            wk1r = csc1 * (wd1r + w[idx4])
            wk1i = csc1 * (wd1i + w[idx4 + 1])
            wk3r = csc3 * (wd3r + w[idx4 + 2])
            wk3i = csc3 * (wd3i + w[idx4 + 3])
            wd1r = w[idx4]
            wd1i = w[idx4 + 1]
            wd3r = w[idx4 + 2]
            wd3i = w[idx4 + 3]
            j1 = j + m
            j2 = j1 + m
            j3 = j2 + m
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            idx5 = offa + j
            x0r = a[idx5] + a[idx2]
            x0i = a[idx5 + 1] + a[idx2 + 1]
            x1r = a[idx5] - a[idx2]
            x1i = a[idx5 + 1] - a[idx2 + 1]
            y0r = a[idx5 + 2] + a[idx2 + 2]
            y0i = a[idx5 + 3] + a[idx2 + 3]
            y1r = a[idx5 + 2] - a[idx2 + 2]
            y1i = a[idx5 + 3] - a[idx2 + 3]
            x2r = a[idx1] + a[idx3]
            x2i = a[idx1 + 1] + a[idx3 + 1]
            x3r = a[idx1] - a[idx3]
            x3i = a[idx1 + 1] - a[idx3 + 1]
            y2r = a[idx1 + 2] + a[idx3 + 2]
            y2i = a[idx1 + 3] + a[idx3 + 3]
            y3r = a[idx1 + 2] - a[idx3 + 2]
            y3i = a[idx1 + 3] - a[idx3 + 3]
            a[idx5] = x0r + x2r
            a[idx5 + 1] = x0i + x2i
            a[idx5 + 2] = y0r + y2r
            a[idx5 + 3] = y0i + y2i
            a[idx1] = x0r - x2r
            a[idx1 + 1] = x0i - x2i
            a[idx1 + 2] = y0r - y2r
            a[idx1 + 3] = y0i - y2i
            x0r = x1r - x3i
            x0i = x1i + x3r
            a[idx2] = wk1r * x0r - wk1i * x0i
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r
            x0r = y1r - y3i
            x0i = y1i + y3r
            a[idx2 + 2] = wd1r * x0r - wd1i * x0i
            a[idx2 + 3] = wd1r * x0i + wd1i * x0r
            x0r = x1r + x3i
            x0i = x1i - x3r
            a[idx3] = wk3r * x0r + wk3i * x0i
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r
            x0r = y1r + y3i
            x0i = y1i - y3r
            a[idx3 + 2] = wd3r * x0r + wd3i * x0i
            a[idx3 + 3] = wd3r * x0i - wd3i * x0r
            j0 = m - j
            j1 = j0 + m
            j2 = j1 + m
            j3 = j2 + m
            idx0 = offa + j0
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            x0r = a[idx0] + a[idx2]
            x0i = a[idx0 + 1] + a[idx2 + 1]
            x1r = a[idx0] - a[idx2]
            x1i = a[idx0 + 1] - a[idx2 + 1]
            y0r = a[idx0 - 2] + a[idx2 - 2]
            y0i = a[idx0 - 1] + a[idx2 - 1]
            y1r = a[idx0 - 2] - a[idx2 - 2]
            y1i = a[idx0 - 1] - a[idx2 - 1]
            x2r = a[idx1] + a[idx3]
            x2i = a[idx1 + 1] + a[idx3 + 1]
            x3r = a[idx1] - a[idx3]
            x3i = a[idx1 + 1] - a[idx3 + 1]
            y2r = a[idx1 - 2] + a[idx3 - 2]
            y2i = a[idx1 - 1] + a[idx3 - 1]
            y3r = a[idx1 - 2] - a[idx3 - 2]
            y3i = a[idx1 - 1] - a[idx3 - 1]
            a[idx0] = x0r + x2r
            a[idx0 + 1] = x0i + x2i
            a[idx0 - 2] = y0r + y2r
            a[idx0 - 1] = y0i + y2i
            a[idx1] = x0r - x2r
            a[idx1 + 1] = x0i - x2i
            a[idx1 - 2] = y0r - y2r
            a[idx1 - 1] = y0i - y2i
            x0r = x1r - x3i
            x0i = x1i + x3r
            a[idx2] = wk1i * x0r - wk1r * x0i
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r
            x0r = y1r - y3i
            x0i = y1i + y3r
            a[idx2 - 2] = wd1i * x0r - wd1r * x0i
            a[idx2 - 1] = wd1i * x0i + wd1r * x0r
            x0r = x1r + x3i
            x0i = x1i - x3r
            a[idx3] = wk3i * x0r + wk3r * x0i
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r
            x0r = y1r + y3i
            x0i = y1i - y3r
            a[offa + j3 - 2] = wd3i * x0r + wd3r * x0i
            a[offa + j3 - 1] = wd3i * x0i - wd3r * x0r
            j += 4
        }
        wk1r = csc1 * (wd1r + wn4r)
        wk1i = csc1 * (wd1i + wn4r)
        wk3r = csc3 * (wd3r - wn4r)
        wk3i = csc3 * (wd3i - wn4r)
        j0 = mh
        j1 = j0 + m
        j2 = j1 + m
        j3 = j2 + m
        idx0 = offa + j0
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[idx0 - 2] + a[idx2 - 2]
        x0i = a[idx0 - 1] + a[idx2 - 1]
        x1r = a[idx0 - 2] - a[idx2 - 2]
        x1i = a[idx0 - 1] - a[idx2 - 1]
        x2r = a[idx1 - 2] + a[idx3 - 2]
        x2i = a[idx1 - 1] + a[idx3 - 1]
        x3r = a[idx1 - 2] - a[idx3 - 2]
        x3i = a[idx1 - 1] - a[idx3 - 1]
        a[idx0 - 2] = x0r + x2r
        a[idx0 - 1] = x0i + x2i
        a[idx1 - 2] = x0r - x2r
        a[idx1 - 1] = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        a[idx2 - 2] = wk1r * x0r - wk1i * x0i
        a[idx2 - 1] = wk1r * x0i + wk1i * x0r
        x0r = x1r + x3i
        x0i = x1i - x3r
        a[idx3 - 2] = wk3r * x0r + wk3i * x0i
        a[idx3 - 1] = wk3r * x0i - wk3i * x0r
        x0r = a[idx0] + a[idx2]
        x0i = a[idx0 + 1] + a[idx2 + 1]
        x1r = a[idx0] - a[idx2]
        x1i = a[idx0 + 1] - a[idx2 + 1]
        x2r = a[idx1] + a[idx3]
        x2i = a[idx1 + 1] + a[idx3 + 1]
        x3r = a[idx1] - a[idx3]
        x3i = a[idx1 + 1] - a[idx3 + 1]
        a[idx0] = x0r + x2r
        a[idx0 + 1] = x0i + x2i
        a[idx1] = x0r - x2r
        a[idx1 + 1] = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        a[idx2] = wn4r * (x0r - x0i)
        a[idx2 + 1] = wn4r * (x0i + x0r)
        x0r = x1r + x3i
        x0i = x1i - x3r
        a[idx3] = -wn4r * (x0r + x0i)
        a[idx3 + 1] = -wn4r * (x0i - x0r)
        x0r = a[idx0 + 2] + a[idx2 + 2]
        x0i = a[idx0 + 3] + a[idx2 + 3]
        x1r = a[idx0 + 2] - a[idx2 + 2]
        x1i = a[idx0 + 3] - a[idx2 + 3]
        x2r = a[idx1 + 2] + a[idx3 + 2]
        x2i = a[idx1 + 3] + a[idx3 + 3]
        x3r = a[idx1 + 2] - a[idx3 + 2]
        x3i = a[idx1 + 3] - a[idx3 + 3]
        a[idx0 + 2] = x0r + x2r
        a[idx0 + 3] = x0i + x2i
        a[idx1 + 2] = x0r - x2r
        a[idx1 + 3] = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        a[idx2 + 2] = wk1i * x0r - wk1r * x0i
        a[idx2 + 3] = wk1i * x0i + wk1r * x0r
        x0r = x1r + x3i
        x0i = x1i - x3r
        a[idx3 + 2] = wk3i * x0r + wk3r * x0i
        a[idx3 + 3] = wk3i * x0i - wk3r * x0r
    }

    private fun cftb1st(n: Int, a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        var j0: Int
        var j1: Int
        var j2: Int
        var j3: Int
        val m: Int
        var wk1r: Float
        var wk1i: Float
        var wk3r: Float
        var wk3i: Float
        var wd1r: Float
        var wd1i: Float
        var wd3r: Float
        var wd3i: Float
        var x0r: Float
        var x0i: Float
        var x1r: Float
        var x1i: Float
        var x2r: Float
        var x2i: Float
        var x3r: Float
        var x3i: Float
        var y0r: Float
        var y0i: Float
        var y1r: Float
        var y1i: Float
        var y2r: Float
        var y2i: Float
        var y3r: Float
        var y3i: Float
        var idx0: Int
        var idx1: Int
        var idx2: Int
        var idx3: Int
        var idx4: Int
        var idx5: Int
        val mh: Int = n shr 3
        m = 2 * mh
        j1 = m
        j2 = j1 + m
        j3 = j2 + m
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[offa] + a[idx2]
        x0i = -a[offa + 1] - a[idx2 + 1]
        x1r = a[offa] - a[idx2]
        x1i = -a[offa + 1] + a[idx2 + 1]
        x2r = a[idx1] + a[idx3]
        x2i = a[idx1 + 1] + a[idx3 + 1]
        x3r = a[idx1] - a[idx3]
        x3i = a[idx1 + 1] - a[idx3 + 1]
        a[offa] = x0r + x2r
        a[offa + 1] = x0i - x2i
        a[idx1] = x0r - x2r
        a[idx1 + 1] = x0i + x2i
        a[idx2] = x1r + x3i
        a[idx2 + 1] = x1i + x3r
        a[idx3] = x1r - x3i
        a[idx3 + 1] = x1i - x3r
        val wn4r: Float = w[startw + 1]
        val csc1: Float = w[startw + 2]
        val csc3: Float = w[startw + 3]
        wd1r = 1f
        wd1i = 0f
        wd3r = 1f
        wd3i = 0f
        var k: Int = 0
        var j = 2
        while (j < mh - 2) {
            k += 4
            idx4 = startw + k
            wk1r = csc1 * (wd1r + w[idx4])
            wk1i = csc1 * (wd1i + w[idx4 + 1])
            wk3r = csc3 * (wd3r + w[idx4 + 2])
            wk3i = csc3 * (wd3i + w[idx4 + 3])
            wd1r = w[idx4]
            wd1i = w[idx4 + 1]
            wd3r = w[idx4 + 2]
            wd3i = w[idx4 + 3]
            j1 = j + m
            j2 = j1 + m
            j3 = j2 + m
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            idx5 = offa + j
            x0r = a[idx5] + a[idx2]
            x0i = -a[idx5 + 1] - a[idx2 + 1]
            x1r = a[idx5] - a[offa + j2]
            x1i = -a[idx5 + 1] + a[idx2 + 1]
            y0r = a[idx5 + 2] + a[idx2 + 2]
            y0i = -a[idx5 + 3] - a[idx2 + 3]
            y1r = a[idx5 + 2] - a[idx2 + 2]
            y1i = -a[idx5 + 3] + a[idx2 + 3]
            x2r = a[idx1] + a[idx3]
            x2i = a[idx1 + 1] + a[idx3 + 1]
            x3r = a[idx1] - a[idx3]
            x3i = a[idx1 + 1] - a[idx3 + 1]
            y2r = a[idx1 + 2] + a[idx3 + 2]
            y2i = a[idx1 + 3] + a[idx3 + 3]
            y3r = a[idx1 + 2] - a[idx3 + 2]
            y3i = a[idx1 + 3] - a[idx3 + 3]
            a[idx5] = x0r + x2r
            a[idx5 + 1] = x0i - x2i
            a[idx5 + 2] = y0r + y2r
            a[idx5 + 3] = y0i - y2i
            a[idx1] = x0r - x2r
            a[idx1 + 1] = x0i + x2i
            a[idx1 + 2] = y0r - y2r
            a[idx1 + 3] = y0i + y2i
            x0r = x1r + x3i
            x0i = x1i + x3r
            a[idx2] = wk1r * x0r - wk1i * x0i
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r
            x0r = y1r + y3i
            x0i = y1i + y3r
            a[idx2 + 2] = wd1r * x0r - wd1i * x0i
            a[idx2 + 3] = wd1r * x0i + wd1i * x0r
            x0r = x1r - x3i
            x0i = x1i - x3r
            a[idx3] = wk3r * x0r + wk3i * x0i
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r
            x0r = y1r - y3i
            x0i = y1i - y3r
            a[idx3 + 2] = wd3r * x0r + wd3i * x0i
            a[idx3 + 3] = wd3r * x0i - wd3i * x0r
            j0 = m - j
            j1 = j0 + m
            j2 = j1 + m
            j3 = j2 + m
            idx0 = offa + j0
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            x0r = a[idx0] + a[idx2]
            x0i = -a[idx0 + 1] - a[idx2 + 1]
            x1r = a[idx0] - a[idx2]
            x1i = -a[idx0 + 1] + a[idx2 + 1]
            y0r = a[idx0 - 2] + a[idx2 - 2]
            y0i = -a[idx0 - 1] - a[idx2 - 1]
            y1r = a[idx0 - 2] - a[idx2 - 2]
            y1i = -a[idx0 - 1] + a[idx2 - 1]
            x2r = a[idx1] + a[idx3]
            x2i = a[idx1 + 1] + a[idx3 + 1]
            x3r = a[idx1] - a[idx3]
            x3i = a[idx1 + 1] - a[idx3 + 1]
            y2r = a[idx1 - 2] + a[idx3 - 2]
            y2i = a[idx1 - 1] + a[idx3 - 1]
            y3r = a[idx1 - 2] - a[idx3 - 2]
            y3i = a[idx1 - 1] - a[idx3 - 1]
            a[idx0] = x0r + x2r
            a[idx0 + 1] = x0i - x2i
            a[idx0 - 2] = y0r + y2r
            a[idx0 - 1] = y0i - y2i
            a[idx1] = x0r - x2r
            a[idx1 + 1] = x0i + x2i
            a[idx1 - 2] = y0r - y2r
            a[idx1 - 1] = y0i + y2i
            x0r = x1r + x3i
            x0i = x1i + x3r
            a[idx2] = wk1i * x0r - wk1r * x0i
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r
            x0r = y1r + y3i
            x0i = y1i + y3r
            a[idx2 - 2] = wd1i * x0r - wd1r * x0i
            a[idx2 - 1] = wd1i * x0i + wd1r * x0r
            x0r = x1r - x3i
            x0i = x1i - x3r
            a[idx3] = wk3i * x0r + wk3r * x0i
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r
            x0r = y1r - y3i
            x0i = y1i - y3r
            a[idx3 - 2] = wd3i * x0r + wd3r * x0i
            a[idx3 - 1] = wd3i * x0i - wd3r * x0r
            j += 4
        }
        wk1r = csc1 * (wd1r + wn4r)
        wk1i = csc1 * (wd1i + wn4r)
        wk3r = csc3 * (wd3r - wn4r)
        wk3i = csc3 * (wd3i - wn4r)
        j0 = mh
        j1 = j0 + m
        j2 = j1 + m
        j3 = j2 + m
        idx0 = offa + j0
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[idx0 - 2] + a[idx2 - 2]
        x0i = -a[idx0 - 1] - a[idx2 - 1]
        x1r = a[idx0 - 2] - a[idx2 - 2]
        x1i = -a[idx0 - 1] + a[idx2 - 1]
        x2r = a[idx1 - 2] + a[idx3 - 2]
        x2i = a[idx1 - 1] + a[idx3 - 1]
        x3r = a[idx1 - 2] - a[idx3 - 2]
        x3i = a[idx1 - 1] - a[idx3 - 1]
        a[idx0 - 2] = x0r + x2r
        a[idx0 - 1] = x0i - x2i
        a[idx1 - 2] = x0r - x2r
        a[idx1 - 1] = x0i + x2i
        x0r = x1r + x3i
        x0i = x1i + x3r
        a[idx2 - 2] = wk1r * x0r - wk1i * x0i
        a[idx2 - 1] = wk1r * x0i + wk1i * x0r
        x0r = x1r - x3i
        x0i = x1i - x3r
        a[idx3 - 2] = wk3r * x0r + wk3i * x0i
        a[idx3 - 1] = wk3r * x0i - wk3i * x0r
        x0r = a[idx0] + a[idx2]
        x0i = -a[idx0 + 1] - a[idx2 + 1]
        x1r = a[idx0] - a[idx2]
        x1i = -a[idx0 + 1] + a[idx2 + 1]
        x2r = a[idx1] + a[idx3]
        x2i = a[idx1 + 1] + a[idx3 + 1]
        x3r = a[idx1] - a[idx3]
        x3i = a[idx1 + 1] - a[idx3 + 1]
        a[idx0] = x0r + x2r
        a[idx0 + 1] = x0i - x2i
        a[idx1] = x0r - x2r
        a[idx1 + 1] = x0i + x2i
        x0r = x1r + x3i
        x0i = x1i + x3r
        a[idx2] = wn4r * (x0r - x0i)
        a[idx2 + 1] = wn4r * (x0i + x0r)
        x0r = x1r - x3i
        x0i = x1i - x3r
        a[idx3] = -wn4r * (x0r + x0i)
        a[idx3 + 1] = -wn4r * (x0i - x0r)
        x0r = a[idx0 + 2] + a[idx2 + 2]
        x0i = -a[idx0 + 3] - a[idx2 + 3]
        x1r = a[idx0 + 2] - a[idx2 + 2]
        x1i = -a[idx0 + 3] + a[idx2 + 3]
        x2r = a[idx1 + 2] + a[idx3 + 2]
        x2i = a[idx1 + 3] + a[idx3 + 3]
        x3r = a[idx1 + 2] - a[idx3 + 2]
        x3i = a[idx1 + 3] - a[idx3 + 3]
        a[idx0 + 2] = x0r + x2r
        a[idx0 + 3] = x0i - x2i
        a[idx1 + 2] = x0r - x2r
        a[idx1 + 3] = x0i + x2i
        x0r = x1r + x3i
        x0i = x1i + x3r
        a[idx2 + 2] = wk1i * x0r - wk1r * x0i
        a[idx2 + 3] = wk1i * x0i + wk1r * x0r
        x0r = x1r - x3i
        x0i = x1i - x3r
        a[idx3 + 2] = wk3i * x0r + wk3r * x0i
        a[idx3 + 3] = wk3i * x0i - wk3r * x0r
    }

    private fun cftrec4_th(n: Int, a: FloatArray, offa: Int, nw: Int, w: FloatArray) {
        var idiv4: Int
        var m: Int
        var nthreads: Int
        nthreads = 2
        idiv4 = 0
        m = n shr 1
        if (n > ConcurrencyUtils.getThreadsBeginN_1D_FFT_4Threads()) {
            nthreads = 4
            idiv4 = 1
            m = m shr 1
        }
        val mf = m
        var i: Int = 0
        val futures = Array<Future<*>>(nthreads) { i ->
            val firstIdx = offa + i * m
            if (i != idiv4) {
                return@Array ConcurrencyUtils.submit {
                    var isplt: Int
                    var j: Int
                    var m12: Int
                    val idx1 = firstIdx + mf
                    m12 = n
                    while (m12 > 512) {
                        m12 = m12 shr 2
                        cftmdl1(m12, a, idx1 - m12, w, nw - (m12 shr 1))
                    }
                    cftleaf(m12, 1, a, idx1 - m12, nw, w)
                    var k: Int = 0
                    val idx2 = firstIdx - m12
                    j = mf - m12
                    while (j > 0) {
                        k++
                        isplt = cfttree(m12, j, k, a, firstIdx, nw, w)
                        cftleaf(m12, isplt, a, idx2 + j, nw, w)
                        j -= m12
                    }
                }
            } else {
                return@Array ConcurrencyUtils.submit {
                    var isplt: Int
                    var j: Int
                    var k: Int
                    var m1: Int
                    val idx1 = firstIdx + mf
                    k = 1
                    m1 = n
                    while (m1 > 512) {
                        m1 = m1 shr 2
                        k = k shl 2
                        cftmdl2(m1, a, idx1 - m1, w, nw - m1)
                    }
                    cftleaf(m1, 0, a, idx1 - m1, nw, w)
                    k = k shr 1
                    val idx2 = firstIdx - m1
                    j = mf - m1
                    while (j > 0) {
                        k++
                        isplt = cfttree(m1, j, k, a, firstIdx, nw, w)
                        cftleaf(m1, isplt, a, idx2 + j, nw, w)
                        j -= m1
                    }
                }
            }
        }
        ConcurrencyUtils.waitForCompletion(futures)
    }

    private fun cftrec4(n: Int, a: FloatArray, offa: Int, nw: Int, w: FloatArray) {
        var isplt: Int
        var j: Int
        var m: Int
        m = n
        val idx1 = offa + n
        while (m > 512) {
            m = m shr 2
            cftmdl1(m, a, idx1 - m, w, nw - (m shr 1))
        }
        cftleaf(m, 1, a, idx1 - m, nw, w)
        var k: Int = 0
        val idx2 = offa - m
        j = n - m
        while (j > 0) {
            k++
            isplt = cfttree(m, j, k, a, offa, nw, w)
            cftleaf(m, isplt, a, idx2 + j, nw, w)
            j -= m
        }
    }

    private fun cfttree(n: Int, j: Int, k: Int, a: FloatArray, offa: Int, nw: Int, w: FloatArray): Int {
        var i: Int
        val isplt: Int
        var m: Int
        val idx1 = offa - n
        if (k and 3 != 0) {
            isplt = k and 1
            if (isplt != 0) {
                cftmdl1(n, a, idx1 + j, w, nw - (n shr 1))
            } else {
                cftmdl2(n, a, idx1 + j, w, nw - n)
            }
        } else {
            m = n
            i = k
            while (i and 3 == 0) {
                m = m shl 2
                i = i shr 2
            }
            isplt = i and 1
            val idx2 = offa + j
            if (isplt != 0) {
                while (m > 128) {
                    cftmdl1(m, a, idx2 - m, w, nw - (m shr 1))
                    m = m shr 2
                }
            } else {
                while (m > 128) {
                    cftmdl2(m, a, idx2 - m, w, nw - m)
                    m = m shr 2
                }
            }
        }
        return isplt
    }

    private fun cftleaf(
        n: Int,
        isplt: Int,
        a: FloatArray,
        offa: Int,
        nw: Int,
        w: FloatArray
    ) {
        if (n == 512) {
            cftmdl1(128, a, offa, w, nw - 64)
            cftf161(a, offa, w, nw - 8)
            cftf162(a, offa + 32, w, nw - 32)
            cftf161(a, offa + 64, w, nw - 8)
            cftf161(a, offa + 96, w, nw - 8)
            cftmdl2(128, a, offa + 128, w, nw - 128)
            cftf161(a, offa + 128, w, nw - 8)
            cftf162(a, offa + 160, w, nw - 32)
            cftf161(a, offa + 192, w, nw - 8)
            cftf162(a, offa + 224, w, nw - 32)
            cftmdl1(128, a, offa + 256, w, nw - 64)
            cftf161(a, offa + 256, w, nw - 8)
            cftf162(a, offa + 288, w, nw - 32)
            cftf161(a, offa + 320, w, nw - 8)
            cftf161(a, offa + 352, w, nw - 8)
            if (isplt != 0) {
                cftmdl1(128, a, offa + 384, w, nw - 64)
                cftf161(a, offa + 480, w, nw - 8)
            } else {
                cftmdl2(128, a, offa + 384, w, nw - 128)
                cftf162(a, offa + 480, w, nw - 32)
            }
            cftf161(a, offa + 384, w, nw - 8)
            cftf162(a, offa + 416, w, nw - 32)
            cftf161(a, offa + 448, w, nw - 8)
        } else {
            cftmdl1(64, a, offa, w, nw - 32)
            cftf081(a, offa, w, nw - 8)
            cftf082(a, offa + 16, w, nw - 8)
            cftf081(a, offa + 32, w, nw - 8)
            cftf081(a, offa + 48, w, nw - 8)
            cftmdl2(64, a, offa + 64, w, nw - 64)
            cftf081(a, offa + 64, w, nw - 8)
            cftf082(a, offa + 80, w, nw - 8)
            cftf081(a, offa + 96, w, nw - 8)
            cftf082(a, offa + 112, w, nw - 8)
            cftmdl1(64, a, offa + 128, w, nw - 32)
            cftf081(a, offa + 128, w, nw - 8)
            cftf082(a, offa + 144, w, nw - 8)
            cftf081(a, offa + 160, w, nw - 8)
            cftf081(a, offa + 176, w, nw - 8)
            if (isplt != 0) {
                cftmdl1(64, a, offa + 192, w, nw - 32)
                cftf081(a, offa + 240, w, nw - 8)
            } else {
                cftmdl2(64, a, offa + 192, w, nw - 64)
                cftf082(a, offa + 240, w, nw - 8)
            }
            cftf081(a, offa + 192, w, nw - 8)
            cftf082(a, offa + 208, w, nw - 8)
            cftf081(a, offa + 224, w, nw - 8)
        }
    }

    private fun cftmdl1(n: Int, a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        var j0: Int
        var j1: Int
        var j2: Int
        var j3: Int
        val m: Int
        var wk1r: Float
        var wk1i: Float
        var wk3r: Float
        var wk3i: Float
        var x0r: Float
        var x0i: Float
        var x1r: Float
        var x1i: Float
        var x2r: Float
        var x2i: Float
        var x3r: Float
        var x3i: Float
        var idx0: Int
        var idx1: Int
        var idx2: Int
        var idx3: Int
        var idx4: Int
        var idx5: Int
        val mh: Int = n shr 3
        m = 2 * mh
        j1 = m
        j2 = j1 + m
        j3 = j2 + m
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[offa] + a[idx2]
        x0i = a[offa + 1] + a[idx2 + 1]
        x1r = a[offa] - a[idx2]
        x1i = a[offa + 1] - a[idx2 + 1]
        x2r = a[idx1] + a[idx3]
        x2i = a[idx1 + 1] + a[idx3 + 1]
        x3r = a[idx1] - a[idx3]
        x3i = a[idx1 + 1] - a[idx3 + 1]
        a[offa] = x0r + x2r
        a[offa + 1] = x0i + x2i
        a[idx1] = x0r - x2r
        a[idx1 + 1] = x0i - x2i
        a[idx2] = x1r - x3i
        a[idx2 + 1] = x1i + x3r
        a[idx3] = x1r + x3i
        a[idx3 + 1] = x1i - x3r
        val wn4r: Float = w[startw + 1]
        var k: Int = 0
        var j = 2
        while (j < mh) {
            k += 4
            idx4 = startw + k
            wk1r = w[idx4]
            wk1i = w[idx4 + 1]
            wk3r = w[idx4 + 2]
            wk3i = w[idx4 + 3]
            j1 = j + m
            j2 = j1 + m
            j3 = j2 + m
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            idx5 = offa + j
            x0r = a[idx5] + a[idx2]
            x0i = a[idx5 + 1] + a[idx2 + 1]
            x1r = a[idx5] - a[idx2]
            x1i = a[idx5 + 1] - a[idx2 + 1]
            x2r = a[idx1] + a[idx3]
            x2i = a[idx1 + 1] + a[idx3 + 1]
            x3r = a[idx1] - a[idx3]
            x3i = a[idx1 + 1] - a[idx3 + 1]
            a[idx5] = x0r + x2r
            a[idx5 + 1] = x0i + x2i
            a[idx1] = x0r - x2r
            a[idx1 + 1] = x0i - x2i
            x0r = x1r - x3i
            x0i = x1i + x3r
            a[idx2] = wk1r * x0r - wk1i * x0i
            a[idx2 + 1] = wk1r * x0i + wk1i * x0r
            x0r = x1r + x3i
            x0i = x1i - x3r
            a[idx3] = wk3r * x0r + wk3i * x0i
            a[idx3 + 1] = wk3r * x0i - wk3i * x0r
            j0 = m - j
            j1 = j0 + m
            j2 = j1 + m
            j3 = j2 + m
            idx0 = offa + j0
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            x0r = a[idx0] + a[idx2]
            x0i = a[idx0 + 1] + a[idx2 + 1]
            x1r = a[idx0] - a[idx2]
            x1i = a[idx0 + 1] - a[idx2 + 1]
            x2r = a[idx1] + a[idx3]
            x2i = a[idx1 + 1] + a[idx3 + 1]
            x3r = a[idx1] - a[idx3]
            x3i = a[idx1 + 1] - a[idx3 + 1]
            a[idx0] = x0r + x2r
            a[idx0 + 1] = x0i + x2i
            a[idx1] = x0r - x2r
            a[idx1 + 1] = x0i - x2i
            x0r = x1r - x3i
            x0i = x1i + x3r
            a[idx2] = wk1i * x0r - wk1r * x0i
            a[idx2 + 1] = wk1i * x0i + wk1r * x0r
            x0r = x1r + x3i
            x0i = x1i - x3r
            a[idx3] = wk3i * x0r + wk3r * x0i
            a[idx3 + 1] = wk3i * x0i - wk3r * x0r
            j += 2
        }
        j0 = mh
        j1 = j0 + m
        j2 = j1 + m
        j3 = j2 + m
        idx0 = offa + j0
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[idx0] + a[idx2]
        x0i = a[idx0 + 1] + a[idx2 + 1]
        x1r = a[idx0] - a[idx2]
        x1i = a[idx0 + 1] - a[idx2 + 1]
        x2r = a[idx1] + a[idx3]
        x2i = a[idx1 + 1] + a[idx3 + 1]
        x3r = a[idx1] - a[idx3]
        x3i = a[idx1 + 1] - a[idx3 + 1]
        a[idx0] = x0r + x2r
        a[idx0 + 1] = x0i + x2i
        a[idx1] = x0r - x2r
        a[idx1 + 1] = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        a[idx2] = wn4r * (x0r - x0i)
        a[idx2 + 1] = wn4r * (x0i + x0r)
        x0r = x1r + x3i
        x0i = x1i - x3r
        a[idx3] = -wn4r * (x0r + x0i)
        a[idx3 + 1] = -wn4r * (x0i - x0r)
    }

    private fun cftmdl2(n: Int, a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        var j0: Int
        var j1: Int
        var j2: Int
        var j3: Int
        var kr: Int
        val m: Int
        var wk1r: Float
        var wk1i: Float
        var wk3r: Float
        var wk3i: Float
        var wd1r: Float
        var wd1i: Float
        var wd3r: Float
        var wd3i: Float
        var x0r: Float
        var x0i: Float
        var x1r: Float
        var x1i: Float
        var x2r: Float
        var x2i: Float
        var x3r: Float
        var x3i: Float
        var y0r: Float
        var y0i: Float
        var y2r: Float
        var y2i: Float
        var idx0: Int
        var idx1: Int
        var idx2: Int
        var idx3: Int
        var idx4: Int
        var idx5: Int
        var idx6: Int
        val mh: Int = n shr 3
        m = 2 * mh
        val wn4r: Float = w[startw + 1]
        j1 = m
        j2 = j1 + m
        j3 = j2 + m
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[offa] - a[idx2 + 1]
        x0i = a[offa + 1] + a[idx2]
        x1r = a[offa] + a[idx2 + 1]
        x1i = a[offa + 1] - a[idx2]
        x2r = a[idx1] - a[idx3 + 1]
        x2i = a[idx1 + 1] + a[idx3]
        x3r = a[idx1] + a[idx3 + 1]
        x3i = a[idx1 + 1] - a[idx3]
        y0r = wn4r * (x2r - x2i)
        y0i = wn4r * (x2i + x2r)
        a[offa] = x0r + y0r
        a[offa + 1] = x0i + y0i
        a[idx1] = x0r - y0r
        a[idx1 + 1] = x0i - y0i
        y0r = wn4r * (x3r - x3i)
        y0i = wn4r * (x3i + x3r)
        a[idx2] = x1r - y0i
        a[idx2 + 1] = x1i + y0r
        a[idx3] = x1r + y0i
        a[idx3 + 1] = x1i - y0r
        var k: Int = 0
        kr = 2 * m
        var j = 2
        while (j < mh) {
            k += 4
            idx4 = startw + k
            wk1r = w[idx4]
            wk1i = w[idx4 + 1]
            wk3r = w[idx4 + 2]
            wk3i = w[idx4 + 3]
            kr -= 4
            idx5 = startw + kr
            wd1i = w[idx5]
            wd1r = w[idx5 + 1]
            wd3i = w[idx5 + 2]
            wd3r = w[idx5 + 3]
            j1 = j + m
            j2 = j1 + m
            j3 = j2 + m
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            idx6 = offa + j
            x0r = a[idx6] - a[idx2 + 1]
            x0i = a[idx6 + 1] + a[idx2]
            x1r = a[idx6] + a[idx2 + 1]
            x1i = a[idx6 + 1] - a[idx2]
            x2r = a[idx1] - a[idx3 + 1]
            x2i = a[idx1 + 1] + a[idx3]
            x3r = a[idx1] + a[idx3 + 1]
            x3i = a[idx1 + 1] - a[idx3]
            y0r = wk1r * x0r - wk1i * x0i
            y0i = wk1r * x0i + wk1i * x0r
            y2r = wd1r * x2r - wd1i * x2i
            y2i = wd1r * x2i + wd1i * x2r
            a[idx6] = y0r + y2r
            a[idx6 + 1] = y0i + y2i
            a[idx1] = y0r - y2r
            a[idx1 + 1] = y0i - y2i
            y0r = wk3r * x1r + wk3i * x1i
            y0i = wk3r * x1i - wk3i * x1r
            y2r = wd3r * x3r + wd3i * x3i
            y2i = wd3r * x3i - wd3i * x3r
            a[idx2] = y0r + y2r
            a[idx2 + 1] = y0i + y2i
            a[idx3] = y0r - y2r
            a[idx3 + 1] = y0i - y2i
            j0 = m - j
            j1 = j0 + m
            j2 = j1 + m
            j3 = j2 + m
            idx0 = offa + j0
            idx1 = offa + j1
            idx2 = offa + j2
            idx3 = offa + j3
            x0r = a[idx0] - a[idx2 + 1]
            x0i = a[idx0 + 1] + a[idx2]
            x1r = a[idx0] + a[idx2 + 1]
            x1i = a[idx0 + 1] - a[idx2]
            x2r = a[idx1] - a[idx3 + 1]
            x2i = a[idx1 + 1] + a[idx3]
            x3r = a[idx1] + a[idx3 + 1]
            x3i = a[idx1 + 1] - a[idx3]
            y0r = wd1i * x0r - wd1r * x0i
            y0i = wd1i * x0i + wd1r * x0r
            y2r = wk1i * x2r - wk1r * x2i
            y2i = wk1i * x2i + wk1r * x2r
            a[idx0] = y0r + y2r
            a[idx0 + 1] = y0i + y2i
            a[idx1] = y0r - y2r
            a[idx1 + 1] = y0i - y2i
            y0r = wd3i * x1r + wd3r * x1i
            y0i = wd3i * x1i - wd3r * x1r
            y2r = wk3i * x3r + wk3r * x3i
            y2i = wk3i * x3i - wk3r * x3r
            a[idx2] = y0r + y2r
            a[idx2 + 1] = y0i + y2i
            a[idx3] = y0r - y2r
            a[idx3 + 1] = y0i - y2i
            j += 2
        }
        wk1r = w[startw + m]
        wk1i = w[startw + m + 1]
        j0 = mh
        j1 = j0 + m
        j2 = j1 + m
        j3 = j2 + m
        idx0 = offa + j0
        idx1 = offa + j1
        idx2 = offa + j2
        idx3 = offa + j3
        x0r = a[idx0] - a[idx2 + 1]
        x0i = a[idx0 + 1] + a[idx2]
        x1r = a[idx0] + a[idx2 + 1]
        x1i = a[idx0 + 1] - a[idx2]
        x2r = a[idx1] - a[idx3 + 1]
        x2i = a[idx1 + 1] + a[idx3]
        x3r = a[idx1] + a[idx3 + 1]
        x3i = a[idx1 + 1] - a[idx3]
        y0r = wk1r * x0r - wk1i * x0i
        y0i = wk1r * x0i + wk1i * x0r
        y2r = wk1i * x2r - wk1r * x2i
        y2i = wk1i * x2i + wk1r * x2r
        a[idx0] = y0r + y2r
        a[idx0 + 1] = y0i + y2i
        a[idx1] = y0r - y2r
        a[idx1 + 1] = y0i - y2i
        y0r = wk1i * x1r - wk1r * x1i
        y0i = wk1i * x1i + wk1r * x1r
        y2r = wk1r * x3r - wk1i * x3i
        y2i = wk1r * x3i + wk1i * x3r
        a[idx2] = y0r - y2r
        a[idx2 + 1] = y0i - y2i
        a[idx3] = y0r + y2r
        a[idx3 + 1] = y0i + y2i
    }

    private fun cftfx41(n: Int, a: FloatArray, offa: Int, nw: Int, w: FloatArray) {
        if (n == 128) {
            cftf161(a, offa, w, nw - 8)
            cftf162(a, offa + 32, w, nw - 32)
            cftf161(a, offa + 64, w, nw - 8)
            cftf161(a, offa + 96, w, nw - 8)
        } else {
            cftf081(a, offa, w, nw - 8)
            cftf082(a, offa + 16, w, nw - 8)
            cftf081(a, offa + 32, w, nw - 8)
            cftf081(a, offa + 48, w, nw - 8)
        }
    }

    private fun cftf161(a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        val y0r: Float
        val y0i: Float
        val y1r: Float
        val y1i: Float
        val y2r: Float
        val y2i: Float
        val y3r: Float
        val y3i: Float
        val y4r: Float
        val y4i: Float
        val y5r: Float
        val y5i: Float
        val y6r: Float
        val y6i: Float
        val y7r: Float
        val y7i: Float
        val y8r: Float
        val y8i: Float
        val y9r: Float
        val y9i: Float
        val y10r: Float
        val y10i: Float
        val y11r: Float
        val y11i: Float
        val y12r: Float
        val y12i: Float
        val y13r: Float
        val y13i: Float
        val y14r: Float
        val y14i: Float
        val y15r: Float
        val y15i: Float
        val wn4r: Float = w[startw + 1]
        val wk1r: Float = w[startw + 2]
        val wk1i: Float = w[startw + 3]
        var x0r: Float = a[offa] + a[offa + 16]
        var x0i: Float = a[offa + 1] + a[offa + 17]
        var x1r: Float = a[offa] - a[offa + 16]
        var x1i: Float = a[offa + 1] - a[offa + 17]
        var x2r: Float = a[offa + 8] + a[offa + 24]
        var x2i: Float = a[offa + 9] + a[offa + 25]
        var x3r: Float = a[offa + 8] - a[offa + 24]
        var x3i: Float = a[offa + 9] - a[offa + 25]
        y0r = x0r + x2r
        y0i = x0i + x2i
        y4r = x0r - x2r
        y4i = x0i - x2i
        y8r = x1r - x3i
        y8i = x1i + x3r
        y12r = x1r + x3i
        y12i = x1i - x3r
        x0r = a[offa + 2] + a[offa + 18]
        x0i = a[offa + 3] + a[offa + 19]
        x1r = a[offa + 2] - a[offa + 18]
        x1i = a[offa + 3] - a[offa + 19]
        x2r = a[offa + 10] + a[offa + 26]
        x2i = a[offa + 11] + a[offa + 27]
        x3r = a[offa + 10] - a[offa + 26]
        x3i = a[offa + 11] - a[offa + 27]
        y1r = x0r + x2r
        y1i = x0i + x2i
        y5r = x0r - x2r
        y5i = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        y9r = wk1r * x0r - wk1i * x0i
        y9i = wk1r * x0i + wk1i * x0r
        x0r = x1r + x3i
        x0i = x1i - x3r
        y13r = wk1i * x0r - wk1r * x0i
        y13i = wk1i * x0i + wk1r * x0r
        x0r = a[offa + 4] + a[offa + 20]
        x0i = a[offa + 5] + a[offa + 21]
        x1r = a[offa + 4] - a[offa + 20]
        x1i = a[offa + 5] - a[offa + 21]
        x2r = a[offa + 12] + a[offa + 28]
        x2i = a[offa + 13] + a[offa + 29]
        x3r = a[offa + 12] - a[offa + 28]
        x3i = a[offa + 13] - a[offa + 29]
        y2r = x0r + x2r
        y2i = x0i + x2i
        y6r = x0r - x2r
        y6i = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        y10r = wn4r * (x0r - x0i)
        y10i = wn4r * (x0i + x0r)
        x0r = x1r + x3i
        x0i = x1i - x3r
        y14r = wn4r * (x0r + x0i)
        y14i = wn4r * (x0i - x0r)
        x0r = a[offa + 6] + a[offa + 22]
        x0i = a[offa + 7] + a[offa + 23]
        x1r = a[offa + 6] - a[offa + 22]
        x1i = a[offa + 7] - a[offa + 23]
        x2r = a[offa + 14] + a[offa + 30]
        x2i = a[offa + 15] + a[offa + 31]
        x3r = a[offa + 14] - a[offa + 30]
        x3i = a[offa + 15] - a[offa + 31]
        y3r = x0r + x2r
        y3i = x0i + x2i
        y7r = x0r - x2r
        y7i = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        y11r = wk1i * x0r - wk1r * x0i
        y11i = wk1i * x0i + wk1r * x0r
        x0r = x1r + x3i
        x0i = x1i - x3r
        y15r = wk1r * x0r - wk1i * x0i
        y15i = wk1r * x0i + wk1i * x0r
        x0r = y12r - y14r
        x0i = y12i - y14i
        x1r = y12r + y14r
        x1i = y12i + y14i
        x2r = y13r - y15r
        x2i = y13i - y15i
        x3r = y13r + y15r
        x3i = y13i + y15i
        a[offa + 24] = x0r + x2r
        a[offa + 25] = x0i + x2i
        a[offa + 26] = x0r - x2r
        a[offa + 27] = x0i - x2i
        a[offa + 28] = x1r - x3i
        a[offa + 29] = x1i + x3r
        a[offa + 30] = x1r + x3i
        a[offa + 31] = x1i - x3r
        x0r = y8r + y10r
        x0i = y8i + y10i
        x1r = y8r - y10r
        x1i = y8i - y10i
        x2r = y9r + y11r
        x2i = y9i + y11i
        x3r = y9r - y11r
        x3i = y9i - y11i
        a[offa + 16] = x0r + x2r
        a[offa + 17] = x0i + x2i
        a[offa + 18] = x0r - x2r
        a[offa + 19] = x0i - x2i
        a[offa + 20] = x1r - x3i
        a[offa + 21] = x1i + x3r
        a[offa + 22] = x1r + x3i
        a[offa + 23] = x1i - x3r
        x0r = y5r - y7i
        x0i = y5i + y7r
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        x0r = y5r + y7i
        x0i = y5i - y7r
        x3r = wn4r * (x0r - x0i)
        x3i = wn4r * (x0i + x0r)
        x0r = y4r - y6i
        x0i = y4i + y6r
        x1r = y4r + y6i
        x1i = y4i - y6r
        a[offa + 8] = x0r + x2r
        a[offa + 9] = x0i + x2i
        a[offa + 10] = x0r - x2r
        a[offa + 11] = x0i - x2i
        a[offa + 12] = x1r - x3i
        a[offa + 13] = x1i + x3r
        a[offa + 14] = x1r + x3i
        a[offa + 15] = x1i - x3r
        x0r = y0r + y2r
        x0i = y0i + y2i
        x1r = y0r - y2r
        x1i = y0i - y2i
        x2r = y1r + y3r
        x2i = y1i + y3i
        x3r = y1r - y3r
        x3i = y1i - y3i
        a[offa] = x0r + x2r
        a[offa + 1] = x0i + x2i
        a[offa + 2] = x0r - x2r
        a[offa + 3] = x0i - x2i
        a[offa + 4] = x1r - x3i
        a[offa + 5] = x1i + x3r
        a[offa + 6] = x1r + x3i
        a[offa + 7] = x1i - x3r
    }

    private fun cftf162(a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        var x2r: Float
        var x2i: Float
        val y0r: Float
        val y0i: Float
        val y1r: Float
        val y1i: Float
        val y2r: Float
        val y2i: Float
        val y3r: Float
        val y3i: Float
        val y4r: Float
        val y4i: Float
        val y5r: Float
        val y5i: Float
        val y6r: Float
        val y6i: Float
        val y7r: Float
        val y7i: Float
        val y8r: Float
        val y8i: Float
        val y9r: Float
        val y9i: Float
        val y10r: Float
        val y10i: Float
        val y11r: Float
        val y11i: Float
        val y12r: Float
        val y12i: Float
        val y13r: Float
        val y13i: Float
        val y14r: Float
        val y14i: Float
        val y15r: Float
        val y15i: Float
        val wn4r: Float = w[startw + 1]
        val wk1r: Float = w[startw + 4]
        val wk1i: Float = w[startw + 5]
        val wk3r: Float = w[startw + 6]
        val wk3i: Float = -w[startw + 7]
        val wk2r: Float = w[startw + 8]
        val wk2i: Float = w[startw + 9]
        var x1r: Float = a[offa] - a[offa + 17]
        var x1i: Float = a[offa + 1] + a[offa + 16]
        var x0r: Float = a[offa + 8] - a[offa + 25]
        var x0i: Float = a[offa + 9] + a[offa + 24]
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        y0r = x1r + x2r
        y0i = x1i + x2i
        y4r = x1r - x2r
        y4i = x1i - x2i
        x1r = a[offa] + a[offa + 17]
        x1i = a[offa + 1] - a[offa + 16]
        x0r = a[offa + 8] + a[offa + 25]
        x0i = a[offa + 9] - a[offa + 24]
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        y8r = x1r - x2i
        y8i = x1i + x2r
        y12r = x1r + x2i
        y12i = x1i - x2r
        x0r = a[offa + 2] - a[offa + 19]
        x0i = a[offa + 3] + a[offa + 18]
        x1r = wk1r * x0r - wk1i * x0i
        x1i = wk1r * x0i + wk1i * x0r
        x0r = a[offa + 10] - a[offa + 27]
        x0i = a[offa + 11] + a[offa + 26]
        x2r = wk3i * x0r - wk3r * x0i
        x2i = wk3i * x0i + wk3r * x0r
        y1r = x1r + x2r
        y1i = x1i + x2i
        y5r = x1r - x2r
        y5i = x1i - x2i
        x0r = a[offa + 2] + a[offa + 19]
        x0i = a[offa + 3] - a[offa + 18]
        x1r = wk3r * x0r - wk3i * x0i
        x1i = wk3r * x0i + wk3i * x0r
        x0r = a[offa + 10] + a[offa + 27]
        x0i = a[offa + 11] - a[offa + 26]
        x2r = wk1r * x0r + wk1i * x0i
        x2i = wk1r * x0i - wk1i * x0r
        y9r = x1r - x2r
        y9i = x1i - x2i
        y13r = x1r + x2r
        y13i = x1i + x2i
        x0r = a[offa + 4] - a[offa + 21]
        x0i = a[offa + 5] + a[offa + 20]
        x1r = wk2r * x0r - wk2i * x0i
        x1i = wk2r * x0i + wk2i * x0r
        x0r = a[offa + 12] - a[offa + 29]
        x0i = a[offa + 13] + a[offa + 28]
        x2r = wk2i * x0r - wk2r * x0i
        x2i = wk2i * x0i + wk2r * x0r
        y2r = x1r + x2r
        y2i = x1i + x2i
        y6r = x1r - x2r
        y6i = x1i - x2i
        x0r = a[offa + 4] + a[offa + 21]
        x0i = a[offa + 5] - a[offa + 20]
        x1r = wk2i * x0r - wk2r * x0i
        x1i = wk2i * x0i + wk2r * x0r
        x0r = a[offa + 12] + a[offa + 29]
        x0i = a[offa + 13] - a[offa + 28]
        x2r = wk2r * x0r - wk2i * x0i
        x2i = wk2r * x0i + wk2i * x0r
        y10r = x1r - x2r
        y10i = x1i - x2i
        y14r = x1r + x2r
        y14i = x1i + x2i
        x0r = a[offa + 6] - a[offa + 23]
        x0i = a[offa + 7] + a[offa + 22]
        x1r = wk3r * x0r - wk3i * x0i
        x1i = wk3r * x0i + wk3i * x0r
        x0r = a[offa + 14] - a[offa + 31]
        x0i = a[offa + 15] + a[offa + 30]
        x2r = wk1i * x0r - wk1r * x0i
        x2i = wk1i * x0i + wk1r * x0r
        y3r = x1r + x2r
        y3i = x1i + x2i
        y7r = x1r - x2r
        y7i = x1i - x2i
        x0r = a[offa + 6] + a[offa + 23]
        x0i = a[offa + 7] - a[offa + 22]
        x1r = wk1i * x0r + wk1r * x0i
        x1i = wk1i * x0i - wk1r * x0r
        x0r = a[offa + 14] + a[offa + 31]
        x0i = a[offa + 15] - a[offa + 30]
        x2r = wk3i * x0r - wk3r * x0i
        x2i = wk3i * x0i + wk3r * x0r
        y11r = x1r + x2r
        y11i = x1i + x2i
        y15r = x1r - x2r
        y15i = x1i - x2i
        x1r = y0r + y2r
        x1i = y0i + y2i
        x2r = y1r + y3r
        x2i = y1i + y3i
        a[offa] = x1r + x2r
        a[offa + 1] = x1i + x2i
        a[offa + 2] = x1r - x2r
        a[offa + 3] = x1i - x2i
        x1r = y0r - y2r
        x1i = y0i - y2i
        x2r = y1r - y3r
        x2i = y1i - y3i
        a[offa + 4] = x1r - x2i
        a[offa + 5] = x1i + x2r
        a[offa + 6] = x1r + x2i
        a[offa + 7] = x1i - x2r
        x1r = y4r - y6i
        x1i = y4i + y6r
        x0r = y5r - y7i
        x0i = y5i + y7r
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        a[offa + 8] = x1r + x2r
        a[offa + 9] = x1i + x2i
        a[offa + 10] = x1r - x2r
        a[offa + 11] = x1i - x2i
        x1r = y4r + y6i
        x1i = y4i - y6r
        x0r = y5r + y7i
        x0i = y5i - y7r
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        a[offa + 12] = x1r - x2i
        a[offa + 13] = x1i + x2r
        a[offa + 14] = x1r + x2i
        a[offa + 15] = x1i - x2r
        x1r = y8r + y10r
        x1i = y8i + y10i
        x2r = y9r - y11r
        x2i = y9i - y11i
        a[offa + 16] = x1r + x2r
        a[offa + 17] = x1i + x2i
        a[offa + 18] = x1r - x2r
        a[offa + 19] = x1i - x2i
        x1r = y8r - y10r
        x1i = y8i - y10i
        x2r = y9r + y11r
        x2i = y9i + y11i
        a[offa + 20] = x1r - x2i
        a[offa + 21] = x1i + x2r
        a[offa + 22] = x1r + x2i
        a[offa + 23] = x1i - x2r
        x1r = y12r - y14i
        x1i = y12i + y14r
        x0r = y13r + y15i
        x0i = y13i - y15r
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        a[offa + 24] = x1r + x2r
        a[offa + 25] = x1i + x2i
        a[offa + 26] = x1r - x2r
        a[offa + 27] = x1i - x2i
        x1r = y12r + y14i
        x1i = y12i - y14r
        x0r = y13r - y15i
        x0i = y13i + y15r
        x2r = wn4r * (x0r - x0i)
        x2i = wn4r * (x0i + x0r)
        a[offa + 28] = x1r - x2i
        a[offa + 29] = x1i + x2r
        a[offa + 30] = x1r + x2i
        a[offa + 31] = x1i - x2r
    }

    private fun cftf081(a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        val y0r: Float
        val y0i: Float
        val y1r: Float
        val y1i: Float
        val y2r: Float
        val y2i: Float
        val y3r: Float
        val y3i: Float
        val y4r: Float
        val y4i: Float
        val y5r: Float
        val y5i: Float
        val y6r: Float
        val y6i: Float
        val y7r: Float
        val y7i: Float
        val wn4r: Float = w[startw + 1]
        var x0r: Float = a[offa] + a[offa + 8]
        var x0i: Float = a[offa + 1] + a[offa + 9]
        var x1r: Float = a[offa] - a[offa + 8]
        var x1i: Float = a[offa + 1] - a[offa + 9]
        var x2r: Float = a[offa + 4] + a[offa + 12]
        var x2i: Float = a[offa + 5] + a[offa + 13]
        var x3r: Float = a[offa + 4] - a[offa + 12]
        var x3i: Float = a[offa + 5] - a[offa + 13]
        y0r = x0r + x2r
        y0i = x0i + x2i
        y2r = x0r - x2r
        y2i = x0i - x2i
        y1r = x1r - x3i
        y1i = x1i + x3r
        y3r = x1r + x3i
        y3i = x1i - x3r
        x0r = a[offa + 2] + a[offa + 10]
        x0i = a[offa + 3] + a[offa + 11]
        x1r = a[offa + 2] - a[offa + 10]
        x1i = a[offa + 3] - a[offa + 11]
        x2r = a[offa + 6] + a[offa + 14]
        x2i = a[offa + 7] + a[offa + 15]
        x3r = a[offa + 6] - a[offa + 14]
        x3i = a[offa + 7] - a[offa + 15]
        y4r = x0r + x2r
        y4i = x0i + x2i
        y6r = x0r - x2r
        y6i = x0i - x2i
        x0r = x1r - x3i
        x0i = x1i + x3r
        x2r = x1r + x3i
        x2i = x1i - x3r
        y5r = wn4r * (x0r - x0i)
        y5i = wn4r * (x0r + x0i)
        y7r = wn4r * (x2r - x2i)
        y7i = wn4r * (x2r + x2i)
        a[offa + 8] = y1r + y5r
        a[offa + 9] = y1i + y5i
        a[offa + 10] = y1r - y5r
        a[offa + 11] = y1i - y5i
        a[offa + 12] = y3r - y7i
        a[offa + 13] = y3i + y7r
        a[offa + 14] = y3r + y7i
        a[offa + 15] = y3i - y7r
        a[offa] = y0r + y4r
        a[offa + 1] = y0i + y4i
        a[offa + 2] = y0r - y4r
        a[offa + 3] = y0i - y4i
        a[offa + 4] = y2r - y6i
        a[offa + 5] = y2i + y6r
        a[offa + 6] = y2r + y6i
        a[offa + 7] = y2i - y6r
    }

    private fun cftf082(a: FloatArray, offa: Int, w: FloatArray, startw: Int) {
        var x1r: Float
        var x1i: Float
        val y2r: Float
        val y2i: Float
        val y3r: Float
        val y3i: Float
        val y4r: Float
        val y4i: Float
        val y5r: Float
        val y5i: Float
        val y6r: Float
        val y6i: Float
        val y7r: Float
        val y7i: Float
        val wn4r: Float = w[startw + 1]
        val wk1r: Float = w[startw + 2]
        val wk1i: Float = w[startw + 3]
        val y0r: Float = a[offa] - a[offa + 9]
        val y0i: Float = a[offa + 1] + a[offa + 8]
        val y1r: Float = a[offa] + a[offa + 9]
        val y1i: Float = a[offa + 1] - a[offa + 8]
        var x0r: Float = a[offa + 4] - a[offa + 13]
        var x0i: Float = a[offa + 5] + a[offa + 12]
        y2r = wn4r * (x0r - x0i)
        y2i = wn4r * (x0i + x0r)
        x0r = a[offa + 4] + a[offa + 13]
        x0i = a[offa + 5] - a[offa + 12]
        y3r = wn4r * (x0r - x0i)
        y3i = wn4r * (x0i + x0r)
        x0r = a[offa + 2] - a[offa + 11]
        x0i = a[offa + 3] + a[offa + 10]
        y4r = wk1r * x0r - wk1i * x0i
        y4i = wk1r * x0i + wk1i * x0r
        x0r = a[offa + 2] + a[offa + 11]
        x0i = a[offa + 3] - a[offa + 10]
        y5r = wk1i * x0r - wk1r * x0i
        y5i = wk1i * x0i + wk1r * x0r
        x0r = a[offa + 6] - a[offa + 15]
        x0i = a[offa + 7] + a[offa + 14]
        y6r = wk1i * x0r - wk1r * x0i
        y6i = wk1i * x0i + wk1r * x0r
        x0r = a[offa + 6] + a[offa + 15]
        x0i = a[offa + 7] - a[offa + 14]
        y7r = wk1r * x0r - wk1i * x0i
        y7i = wk1r * x0i + wk1i * x0r
        x0r = y0r + y2r
        x0i = y0i + y2i
        x1r = y4r + y6r
        x1i = y4i + y6i
        a[offa] = x0r + x1r
        a[offa + 1] = x0i + x1i
        a[offa + 2] = x0r - x1r
        a[offa + 3] = x0i - x1i
        x0r = y0r - y2r
        x0i = y0i - y2i
        x1r = y4r - y6r
        x1i = y4i - y6i
        a[offa + 4] = x0r - x1i
        a[offa + 5] = x0i + x1r
        a[offa + 6] = x0r + x1i
        a[offa + 7] = x0i - x1r
        x0r = y1r - y3i
        x0i = y1i + y3r
        x1r = y5r - y7r
        x1i = y5i - y7i
        a[offa + 8] = x0r + x1r
        a[offa + 9] = x0i + x1i
        a[offa + 10] = x0r - x1r
        a[offa + 11] = x0i - x1i
        x0r = y1r + y3i
        x0i = y1i - y3r
        x1r = y5r + y7r
        x1i = y5i + y7i
        a[offa + 12] = x0r - x1i
        a[offa + 13] = x0i + x1r
        a[offa + 14] = x0r + x1i
        a[offa + 15] = x0i - x1r
    }

    private fun cftf040(a: FloatArray, offa: Int) {
        val x0r: Float = a[offa] + a[offa + 4]
        val x0i: Float = a[offa + 1] + a[offa + 5]
        val x1r: Float = a[offa] - a[offa + 4]
        val x1i: Float = a[offa + 1] - a[offa + 5]
        val x2r: Float = a[offa + 2] + a[offa + 6]
        val x2i: Float = a[offa + 3] + a[offa + 7]
        val x3r: Float = a[offa + 2] - a[offa + 6]
        val x3i: Float = a[offa + 3] - a[offa + 7]
        a[offa] = x0r + x2r
        a[offa + 1] = x0i + x2i
        a[offa + 2] = x1r - x3i
        a[offa + 3] = x1i + x3r
        a[offa + 4] = x0r - x2r
        a[offa + 5] = x0i - x2i
        a[offa + 6] = x1r + x3i
        a[offa + 7] = x1i - x3r
    }

    private fun cftb040(a: FloatArray, offa: Int) {
        val x0r: Float = a[offa] + a[offa + 4]
        val x0i: Float = a[offa + 1] + a[offa + 5]
        val x1r: Float = a[offa] - a[offa + 4]
        val x1i: Float = a[offa + 1] - a[offa + 5]
        val x2r: Float = a[offa + 2] + a[offa + 6]
        val x2i: Float = a[offa + 3] + a[offa + 7]
        val x3r: Float = a[offa + 2] - a[offa + 6]
        val x3i: Float = a[offa + 3] - a[offa + 7]
        a[offa] = x0r + x2r
        a[offa + 1] = x0i + x2i
        a[offa + 2] = x1r + x3i
        a[offa + 3] = x1i - x3r
        a[offa + 4] = x0r - x2r
        a[offa + 5] = x0i - x2i
        a[offa + 6] = x1r - x3i
        a[offa + 7] = x1i + x3r
    }

    private fun cftx020(a: FloatArray, offa: Int) {
        val x0r: Float = a[offa] - a[offa + 2]
        val x0i: Float = -a[offa + 1] + a[offa + 3]
        a[offa] += a[offa + 2]
        a[offa + 1] += a[offa + 3]
        a[offa + 2] = x0r
        a[offa + 3] = x0i
    }

    private fun cftxb020(a: FloatArray, offa: Int) {
        val x0r: Float = a[offa] - a[offa + 2]
        val x0i: Float = a[offa + 1] - a[offa + 3]
        a[offa] += a[offa + 2]
        a[offa + 1] += a[offa + 3]
        a[offa + 2] = x0r
        a[offa + 3] = x0i
    }

    private fun cftxc020(a: FloatArray, offa: Int) {
        val x0r: Float = a[offa] - a[offa + 2]
        val x0i: Float = a[offa + 1] + a[offa + 3]
        a[offa] += a[offa + 2]
        a[offa + 1] -= a[offa + 3]
        a[offa + 2] = x0r
        a[offa + 3] = x0i
    }

    private fun rftfsub(
        n: Int,
        a: FloatArray,
        offa: Int,
        nc: Int,
        c: FloatArray,
        startc: Int
    ) {
        var k: Int
        val ks: Int
        var wkr: Float
        var wki: Float
        var xr: Float
        var xi: Float
        var yr: Float
        var yi: Float
        var idx1: Int
        var idx2: Int
        val m: Int = n shr 1
        ks = 2 * nc / m
        var kk: Int = 0
        var j = 2
        while (j < m) {
            k = n - j
            kk += ks
            wkr = (0.5 - c[startc + nc - kk]).toFloat()
            wki = c[startc + kk]
            idx1 = offa + j
            idx2 = offa + k
            xr = a[idx1] - a[idx2]
            xi = a[idx1 + 1] + a[idx2 + 1]
            yr = wkr * xr - wki * xi
            yi = wkr * xi + wki * xr
            a[idx1] -= yr
            a[idx1 + 1] = yi - a[idx1 + 1]
            a[idx2] += yr
            a[idx2 + 1] = yi - a[idx2 + 1]
            j += 2
        }
        a[offa + m + 1] = -a[offa + m + 1]
    }

    private fun rftbsub(
        n: Int,
        a: FloatArray,
        offa: Int,
        nc: Int,
        c: FloatArray,
        startc: Int
    ) {
        var k: Int
        val ks: Int
        var wkr: Float
        var wki: Float
        var xr: Float
        var xi: Float
        var yr: Float
        var yi: Float
        var idx1: Int
        var idx2: Int
        val m: Int = n shr 1
        ks = 2 * nc / m
        var kk: Int = 0
        var j = 2
        while (j < m) {
            k = n - j
            kk += ks
            wkr = (0.5 - c[startc + nc - kk]).toFloat()
            wki = c[startc + kk]
            idx1 = offa + j
            idx2 = offa + k
            xr = a[idx1] - a[idx2]
            xi = a[idx1 + 1] + a[idx2 + 1]
            yr = wkr * xr - wki * xi
            yi = wkr * xi + wki * xr
            a[idx1] -= yr
            a[idx1 + 1] -= yi
            a[idx2] += yr
            a[idx2 + 1] -= yi
            j += 2
        }
    }

    private fun scale(m: Float, a: FloatArray, offa: Int, complex: Boolean) {
        val norm = (1.0 / m).toFloat()
        val n2: Int = if (complex) {
            2 * n
        } else {
            n
        }
        val nthreads = ConcurrencyUtils.getNumberOfThreads()
        if (nthreads > 1 && n2 >= ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            val k = n2 / nthreads
            val futures = Array<Future<*>>(nthreads) { i ->
                val firstIdx = offa + i * k
                val lastIdx = if (i == nthreads - 1) offa + n2 else firstIdx + k
                return@Array ConcurrencyUtils.submit {
                    for (i1 in firstIdx until lastIdx) {
                        a[i1] *= norm
                    }
                }
            }
            ConcurrencyUtils.waitForCompletion(futures)
        } else {
            for (i in offa until offa + n2) {
                a[i] *= norm
            }
        }
    }

    private enum class Plans {
        SPLIT_RADIX, MIXED_RADIX, BLUESTEIN
    }

    companion object {
        private val DEFAULT_FACTORS = intArrayOf(4, 2, 3, 5)
        private fun getReminder(n: Int, factors: IntArray = DEFAULT_FACTORS): Int {
            var reminder = n
            require(n > 0) { "n must be positive integer" }
            var i = 0
            while (i < factors.size && reminder != 1) {
                val factor = factors[i]
                while (reminder % factor == 0) {
                    reminder /= factor
                }
                i++
            }
            return reminder
        }
    }

    /**
     * Creates new instance of FloatFFT.
     *
     * @param n size of data
     */
    init {
        require(n > 0) { "n must be greater than 0" }
        if (!ConcurrencyUtils.isPowerOf2(n)) {
            if (getReminder(n, DEFAULT_FACTORS) >= 211) {
                plan = Plans.BLUESTEIN
                wtable = FloatArray(0)
                wtableR = FloatArray(0)
                nBluestein = ConcurrencyUtils.nextPow2(n * 2 - 1)
                bk1 = FloatArray(2 * nBluestein)
                bk2 = FloatArray(2 * nBluestein)
                ip = IntArray(
                    2 + ceil(
                        2 + (1 shl (ln(nBluestein + 0.5) / ln(2.0)).toInt() / 2).toDouble()
                    ).toInt()
                )
                w = FloatArray(nBluestein)
                val twon = 2 * nBluestein
                nw = ip[0]
                if (twon > nw shl 2) {
                    nw = twon shr 2
                    makewt(nw)
                }
                nc = ip[1]
                if (nBluestein > nc shl 2) {
                    nc = nBluestein shr 2
                    makect(nc, w, nw)
                }
                bluesteini()
            } else {
                ip = IntArray(0)
                w = FloatArray(0)
                bk1 = FloatArray(0)
                bk2 = FloatArray(0)
                plan = Plans.MIXED_RADIX
                wtable = FloatArray(4 * n + 15)
                wtableR = FloatArray(2 * n + 15)
                cffti()
                rffti()
            }
        } else {
            plan = Plans.SPLIT_RADIX
            bk1 = FloatArray(0)
            bk2 = FloatArray(0)
            wtable = FloatArray(0)
            wtableR = FloatArray(0)
            ip =
                IntArray(2 + ceil(2 + (1 shl (ln(n + 0.5) / ln(2.0)).toInt() / 2).toDouble()).toInt())
            w = FloatArray(n)
            val twon = 2 * n
            nw = ip[0]
            if (twon > nw shl 2) {
                nw = twon shr 2
                makewt(nw)
            }
            nc = ip[1]
            if (n > nc shl 2) {
                nc = n shr 2
                makect(nc, w, nw)
            }
        }
    }
}