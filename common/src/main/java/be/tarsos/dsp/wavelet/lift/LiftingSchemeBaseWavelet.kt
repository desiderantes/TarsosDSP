package be.tarsos.dsp.wavelet.lift

/**
 *
 *
 * class LiftingSchemeBaseWavelet: base class for simple Lifting Scheme wavelets
 * using split, predict, update or update, predict, merge steps.
 *
 *
 *
 *
 * Simple lifting scheme wavelets consist of three steps, a split/merge step,
 * predict step and an update step:
 *
 *
 *  *
 *
 *
 * The split step divides the elements in an array so that the even elements are
 * in the first half and the odd elements are in the second half.
 *
 *
 *  *
 *
 *
 * The merge step is the inverse of the split step. It takes two regions of an
 * array, an odd region and an even region and merges them into a new region
 * where an even element alternates with an odd element.
 *
 *
 *  *
 *
 *
 * The predict step calculates the difference between an odd element and its
 * predicted value based on the even elements. The difference between the
 * predicted value and the actual value replaces the odd element.
 *
 *
 *  *
 *
 *
 * The predict step operates on the odd elements. The update step operates on
 * the even element, replacing them with a difference between the predict value
 * and the actual odd element. The update step replaces each even element with
 * an average. The result of the update step becomes the input to the next
 * recursive step in the wavelet calculation.
 *
 *
 *
 *
 *
 *
 *
 * The split and merge methods are shared by all Lifting Scheme wavelet
 * algorithms. This base class provides the transform and inverse transform
 * methods (forwardTrans and inverseTrans). The predict and update methods are
 * abstract and are defined for a particular Lifting Scheme wavelet sub-class.
 *
 *
 *
 *
 * **References:**
 *
 *
 *
 *  *
 * [
 * *The Wavelet Lifting Scheme*](http://www.bearcave.com/misl/misl_tech/wavelets/lifting/index.html) by Ian Kaplan, www.bearcave.com. This
 * is the parent web page for this Java source code.
 *  *
 * *Ripples in Mathematics: the Discrete Wavelet Transform* by Arne Jense
 * and Anders la Cour-Harbo, Springer, 2001
 *  *
 * *Building Your Own Wavelets at Home* in [
 * Wavelets in Computer Graphics](http://www.multires.caltech.edu/teaching/courses/waveletcourse/)
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
abstract class LiftingSchemeBaseWavelet {
    /**
     * Split the *vec* into even and odd elements, where the even elements
     * are in the first half of the vector and the odd elements are in the
     * second half.
     */
    protected fun split(vec: FloatArray, N: Int) {
        var start = 1
        var end = N - 1
        while (start < end) {
            var i = start
            while (i < end) {
                val tmp = vec[i]
                vec[i] = vec[i + 1]
                vec[i + 1] = tmp
                i += 2
            }
            start += 1
            end -= 1
        }
    }

    /**
     * Merge the odd elements from the second half of the N element region in
     * the array with the even elements in the first half of the N element
     * region. The result will be the combination of the odd and even elements
     * in a region of length N.
     */
    protected fun merge(vec: FloatArray, N: Int) {
        val half = N shr 1
        var start = half - 1
        var end = half
        while (start > 0) {
            var i = start
            while (i < end) {
                val tmp = vec[i]
                vec[i] = vec[i + 1]
                vec[i + 1] = tmp
                i += 2
            }
            start -= 1
            end += 1
        }
    }

    /**
     * Predict step, to be defined by the subclass
     *
     * @param vec       input array
     * @param N         size of region to act on (from 0..N-1)
     * @param direction forward or inverse transform
     */
    protected abstract fun predict(vec: FloatArray, N: Int, direction: TransformDirection)

    /**
     * Update step, to be defined by the subclass
     *
     * @param vec       input array
     * @param N         size of region to act on (from 0..N-1)
     * @param direction forward or inverse transform
     */
    protected abstract fun update(vec: FloatArray, N: Int, direction: TransformDirection)

    /**
     *
     *
     * Simple wavelet Lifting Scheme forward transform
     *
     *
     *
     *
     * forwardTrans is passed an array of doubles. The array size must be a
     * power of two. Lifting Scheme wavelet transforms are calculated in-place
     * and the result is returned in the argument array.
     *
     *
     *
     *
     * The result of forwardTrans is a set of wavelet coefficients ordered by
     * increasing frequency and an approximate average of the input data set in
     * vec[0]. The coefficient bands follow this element in powers of two (e.g.,
     * 1, 2, 4, 8...).
     *
     *
     * @param vec the vector
     */
    open fun forwardTrans(vec: FloatArray) {
        val N = vec.size
        var n = N
        while (n > 1) {
            split(vec, n)
            predict(vec, n, TransformDirection.FORWARD)
            update(vec, n, TransformDirection.FORWARD)
            n = n shr 1
        }
    } // forwardTrans

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
     *
     * @param vec the vector
     */
    open fun inverseTrans(vec: FloatArray) {
        val N = vec.size
        var n = 2
        while (n <= N) {
            update(vec, n, TransformDirection.INVERSE)
            predict(vec, n, TransformDirection.INVERSE)
            merge(vec, n)
            n = n shl 1
        }
    }
}

