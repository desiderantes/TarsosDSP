package be.tarsos.dsp.wavelet.lift

/**
 *
 *
 * HaarWavelet (flat LineWavelet) wavelet.
 *
 *
 *
 *
 * As with all Lifting scheme wavelet transform functions, the first stage of a
 * transform step is the split stage. The split step moves the even element to
 * the first half of an N element region and the odd elements to the second half
 * of the N element region.
 *
 *
 *
 *
 * The Lifting Scheme version of the HaarWavelet transform uses a wavelet
 * function (predict stage) that "predicts" that an odd element will have the
 * same value as it preceeding even element. Stated another way, the odd element
 * is "predicted" to be on a flat (zero slope LineWavelet) shared with the even
 * point. The difference between this "prediction" and the actual odd value
 * replaces the odd element.
 *
 *
 *
 *
 * The wavelet scaling function (a.k.a. smoothing function) used in the update
 * stage calculates the average between an even and an odd element.
 *
 *
 *
 *
 * The merge stage at the end of the inverse transform interleaves odd and even
 * elements from the two halves of the array (e.g., ordering them
 * even<sub>0</sub>, odd<sub>0</sub>, even<sub>1</sub>, odd<sub>1</sub>, ...)
 *
 *
 * <h4>
 * Copyright and Use</h4>
 *
 *
 *
 * You may use this source code without limitation and without fee as long as
 * you include:
 *
 * <blockquote> This software was written and is copyrighted by Ian Kaplan, Bear
 * Products International, www.bearcave.com, 2001. </blockquote>
 *
 *
 * This software is provided "as is", without any warrenty or claim as to its
 * usefulness. Anyone who uses this source code uses it at their own risk. Nor
 * is any support provided by Ian Kaplan and Bear Products International.
 *
 *
 * Please send any bug fixes or suggested source changes to:
 *
 * <pre>
 * iank@bearcave.com
</pre> *
 *
 * @author Ian Kaplan
 */
open class HaarWavelet : LiftingSchemeBaseWavelet() {
    /**
     * HaarWavelet predict step
     */
    override fun predict(
        vec: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        for (i in 0 until half) {
            val predictVal = vec[i]
            val j = i + half
            when (direction) {
                TransformDirection.FORWARD -> {
                    vec[j] = vec[j] - predictVal
                }
                TransformDirection.INVERSE -> {
                    vec[j] = vec[j] + predictVal
                }
            }
        }
    }

    fun forwardTransOne(vec: FloatArray) {
        val N = vec.size
        split(vec, N)
        predict(vec, N, TransformDirection.FORWARD)
        update(vec, N, TransformDirection.FORWARD)
    }

    /**
     *
     *
     * Update step of the HaarWavelet wavelet transform.
     *
     *
     *
     * The wavelet transform calculates a set of detail or difference
     * coefficients in the predict step. These are stored in the upper half of
     * the array. The update step calculates an average from the even-odd
     * element pairs. The averages will replace the even elements in the lower
     * half of the array.
     *
     *
     *
     * The HaarWavelet wavelet calculation used in the Lifting Scheme is
     *
     *
     * <pre>
     * d<sub>j+1, i</sub> = odd<sub>j+1, i</sub> = odd<sub>j, i</sub> - even<sub>j, i</sub>
     * a<sub>j+1, i</sub> = even<sub>j, i</sub> = (even<sub>j, i</sub> + odd<sub>j, i</sub>)/2
    </pre> *
     *
     *
     * Note that the Lifting Scheme uses an in-place algorithm. The odd elements
     * have been replaced by the detail coefficients in the predict step. With a
     * little algebra we can substitute the coefficient calculation into the
     * average calculation, which gives us
     *
     *
     * <pre>
     * a<sub>j+1, i</sub> = even<sub>j, i</sub> = even<sub>j, i</sub> + (odd<sub>j, i</sub>/2)
    </pre> *
     */
    override fun update(
        vec: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        for (i in 0 until half) {
            val j = i + half
            val updateVal = vec[j] / 2.0f
            when (direction) {
                TransformDirection.FORWARD -> {
                    vec[i] = vec[i] + updateVal
                }
                TransformDirection.INVERSE -> {
                    vec[i] = vec[i] - updateVal
                }
            }
        }
    }
} // HaarWavelet
