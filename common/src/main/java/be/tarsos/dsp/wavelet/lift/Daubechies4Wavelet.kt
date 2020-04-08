package be.tarsos.dsp.wavelet.lift

import kotlin.math.sqrt

/**
 * @author Ian Kaplan
 */
class Daubechies4Wavelet : LiftingSchemeBaseWavelet() {
    protected fun normalize(
        S: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        for (n in 0 until half) {
            if (direction === TransformDirection.FORWARD) {
                S[n] =
                    (sqrt3 - 1.0f) / sqrt2 * S[n]
                S[n + half] =
                    (sqrt3 + 1.0f) / sqrt2 * S[n + half]
            } else if (direction === TransformDirection.INVERSE) {
                S[n] =
                    (sqrt3 + 1.0f) / sqrt2 * S[n]
                S[n + half] =
                    (sqrt3 - 1.0f) / sqrt2 * S[n + half]
            } else {
                println("Daubechies4Wavelet::normalize: bad direction value")
                break
            }
        }
    }

    override fun predict(
        vec: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        if (direction === TransformDirection.FORWARD) {
            vec[half] = vec[half] - sqrt3 / 4.0f * vec[0] - (sqrt3 - 2) / 4.0f * vec[half - 1]
        } else if (direction === TransformDirection.INVERSE) {
            vec[half] = vec[half] + sqrt3 / 4.0f * vec[0] + (sqrt3 - 2) / 4.0f * vec[half - 1]
        } else {
            println("Daubechies4Wavelet::predict: bad direction value")
        }

        // predict, forward
        for (n in 1 until half) {
            if (direction === TransformDirection.FORWARD) {
                vec[half + n] =
                    vec[half + n] - sqrt3 / 4.0f * vec[n] - (sqrt3 - 2) / 4.0f * vec[n - 1]
            } else if (direction === TransformDirection.INVERSE) {
                vec[half + n] =
                    vec[half + n] + sqrt3 / 4.0f * vec[n] + (sqrt3 - 2) / 4.0f * vec[n - 1]
            } else {
                break
            }
        }
    }

    protected fun updateOne(
        S: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        for (n in 0 until half) {
            val updateVal = sqrt3 * S[half + n]
            if (direction === TransformDirection.FORWARD) {
                S[n] = S[n] + updateVal
            } else if (direction === TransformDirection.INVERSE) {
                S[n] = S[n] - updateVal
            } else {
                println("Daubechies4Wavelet::updateOne: bad direction value")
                break
            }
        }
    }

    override fun update(
        vec: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        for (n in 0 until half - 1) {
            when (direction) {
                TransformDirection.FORWARD -> {
                    vec[n] = vec[n] - vec[half + n + 1]
                }
                TransformDirection.INVERSE -> {
                    vec[n] = vec[n] + vec[half + n + 1]
                }
            }
        }
        vec[half - 1] = when (direction) {
            TransformDirection.FORWARD -> vec[half - 1] - vec[half]
            TransformDirection.INVERSE -> vec[half - 1] + vec[half]
        }
    }

    override fun forwardTrans(vec: FloatArray) {
        val N = vec.size
        var n = N
        while (n > 1) {
            split(vec, n)
            updateOne(vec, n, TransformDirection.FORWARD) // update 1
            predict(vec, n, TransformDirection.FORWARD)
            update(vec, n, TransformDirection.FORWARD) // update 2
            normalize(vec, n, TransformDirection.FORWARD)
            n = n shr 1
        }
    }

    /**
     *
     *
     * Default two step Lifting Scheme inverse wavelet transform
     *
     *
     *
     *
     * inverseTrans is passed the result of an ordered wavelet transform,
     * consisting of an average and a set of wavelet coefficients. The inverse
     * transform is calculated in-place and the result is returned in the
     * argument array.
     *
     */
    override fun inverseTrans(vec: FloatArray) {
        val N = vec.size
        var n = 2
        while (n <= N) {
            normalize(vec, n, TransformDirection.INVERSE)
            update(vec, n, TransformDirection.INVERSE)
            predict(vec, n, TransformDirection.INVERSE)
            updateOne(vec, n, TransformDirection.INVERSE)
            merge(vec, n)
            n = n shl 1
        }
    }

    companion object {
        val sqrt3 = sqrt(3.0).toFloat()
        val sqrt2 = sqrt(2.0).toFloat()
    }
}