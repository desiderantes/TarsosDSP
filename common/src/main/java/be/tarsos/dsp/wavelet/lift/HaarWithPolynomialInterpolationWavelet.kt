package be.tarsos.dsp.wavelet.lift

/**
 *
 *
 * HaarWavelet transform extended with a polynomial interpolation step
 *
 *
 *
 * This wavelet transform extends the HaarWavelet transform with a polynomial
 * wavelet function.
 *
 *
 *
 * The polynomial wavelet uses 4-point polynomial interpolation to "predict" an
 * odd point from four even point values.
 *
 *
 *
 * This class extends the HaarWavelet transform with an interpolation stage
 * which follows the predict and update stages of the HaarWavelet transform. The
 * predict value is calculated from the even points, which in this case are the
 * smoothed values calculated by the scaling function (e.g., the averages of the
 * even and odd values).
 *
 *
 *
 *
 * The predict value is subtracted from the current odd value, which is the
 * result of the HaarWavelet wavelet function (e.g., the difference between the
 * odd value and the even value). This tends to result in large odd values after
 * the interpolation stage, which is a weakness in this algorithm.
 *
 *
 *
 *
 * This algorithm was suggested by Wim Sweldens' tutorial *Building Your Own
 * Wavelets at Home*.
 *
 *
 *
 *
 * <pre>
 * [
 * http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html](http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html)
</pre> *
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
class HaarWithPolynomialInterpolationWavelet : HaarWavelet() {
    private val fourPt: PolynomialInterpolation

    /**
     *
     *
     * Copy four points or *N* (which ever is less) data points from
     * *vec* into *d* These points are the "known" points used in the
     * polynomial interpolation.
     *
     *
     * @param vec   the input data set on which the wavelet is calculated
     * @param d     an array into which *N* data points, starting at
     * *start* are copied.
     * @param N     the number of polynomial interpolation points
     * @param start the index in *vec* from which copying starts
     */
    private fun fill(
        vec: FloatArray,
        d: FloatArray,
        N: Int,
        start: Int
    ) {
        var n = numPts
        if (n > N) n = N
        val end = start + n
        for ((j, i) in (start until end).withIndex()) {
            d[j] = vec[i]
        }
    } // fill

    /**
     *
     *
     * Predict an odd point from the even points, using 4-point polynomial
     * interpolation.
     *
     *
     *
     * The four points used in the polynomial interpolation are the even points.
     * We pretend that these four points are located at the x-coordinates
     * 0,1,2,3. The first odd point interpolated will be located between the
     * first and second even point, at 0.5. The next N-3 points are located at
     * 1.5 (in the middle of the four points). The last two points are located
     * at 2.5 and 3.5. For complete documentation see
     *
     *
     * <pre>
     * [
 * http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html](http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html)
    </pre> *
     *
     *
     *
     * The difference between the predicted (interpolated) value and the actual
     * odd value replaces the odd value in the forward transform.
     *
     *
     *
     *
     * As the recursive steps proceed, N will eventually be 4 and then 2. When N
     * = 4, linear interpolation is used. When N = 2, HaarWavelet interpolation
     * is used (the prediction for the odd value is that it is equal to the even
     * value).
     *
     *
     * @param vec       the input data on which the forward or inverse transform is
     * calculated.
     * @param N         the area of vec over which the transform is calculated
     * @param direction forward or inverse transform
     */
    protected fun interp(
        vec: FloatArray,
        N: Int,
        direction: TransformDirection
    ) {
        val half = N shr 1
        val d =
            FloatArray(numPts)

        // int k = 42;
        for (i in 0 until half) {
            var predictVal: Float
            predictVal = if (i == 0) {
                if (half == 1) {
                    // e.g., N == 2, and we use HaarWavelet interpolation
                    vec[0]
                } else {
                    fill(vec, d, N, 0)
                    fourPt.interpPoint(0.5f, half, d)
                }
            } else if (i == 1) {
                fourPt.interpPoint(1.5f, half, d)
            } else if (i == half - 2) {
                fourPt.interpPoint(2.5f, half, d)
            } else if (i == half - 1) {
                fourPt.interpPoint(3.5f, half, d)
            } else {
                fill(vec, d, N, i - 1)
                fourPt.interpPoint(1.5f, half, d)
            }
            val j = i + half
            when (direction) {
                TransformDirection.FORWARD -> vec[j] = vec[j] - predictVal
                TransformDirection.INVERSE -> vec[j] = vec[j] + predictVal
            }
        }
    } // interp

    /**
     *
     *
     * HaarWavelet transform extened with polynomial interpolation forward
     * transform.
     *
     *
     *
     * This version of the forwardTrans function overrides the function in the
     * LiftingSchemeBaseWavelet base class. This function introduces an extra
     * polynomial interpolation stage at the end of the transform.
     *
     */
    override fun forwardTrans(vec: FloatArray) {
        val N = vec.size
        var n = N
        while (n > 1) {
            split(vec, n)
            predict(vec, n, TransformDirection.FORWARD)
            update(vec, n, TransformDirection.FORWARD)
            interp(vec, n, TransformDirection.FORWARD)
            n = n shr 1
        }
    } // forwardTrans

    /**
     *
     *
     * HaarWavelet transform extened with polynomial interpolation inverse
     * transform.
     *
     *
     *
     * This version of the inverseTrans function overrides the function in the
     * LiftingSchemeBaseWavelet base class. This function introduces an inverse
     * polynomial interpolation stage at the start of the inverse transform.
     *
     */
    override fun inverseTrans(vec: FloatArray) {
        val N = vec.size
        var n = 2
        while (n <= N) {
            interp(vec, n, TransformDirection.INVERSE)
            update(vec, n, TransformDirection.INVERSE)
            predict(vec, n, TransformDirection.INVERSE)
            merge(vec, n)
            n = n shl 1
        }
    } // inverseTrans

    companion object {
        const val numPts = 4
    }

    /**
     * HaarWithPolynomialInterpolationWavelet class constructor
     */
    init {
        fourPt = PolynomialInterpolation()
    }
} // HaarWithPolynomialInterpolationWavelet
