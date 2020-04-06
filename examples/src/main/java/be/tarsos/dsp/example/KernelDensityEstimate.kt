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
/*
 *              _______
 *             |__   __|
 *                | | __ _ _ __ ___  ___  ___
 *                | |/ _` | '__/ __|/ _ \/ __|
 *                | | (_| | |  \__ \ (_) \__ \
 *                |_|\__,_|_|  |___/\___/|___/
 *
 * -----------------------------------------------------------
 *
 *  Tarsos is developed by Joren Six at
 *  The School of Arts,
 *  University College Ghent,
 *  Hoogpoort 64, 9000 Ghent - Belgium
 *
 * -----------------------------------------------------------
 *
 *  Info: http://tarsos.0110.be
 *  Github: https://github.com/JorenSix/Tarsos
 *  Releases: http://tarsos.0110.be/releases/Tarsos/
 *
 *  Tarsos includes some source code by various authors,
 *  for credits and info, see README.
 *
 */
package be.tarsos.dsp.example

import kotlin.math.pow

class KernelDensityEstimate {
    /**
     * Returns the current estimate.
     *
     * @return The current estimate. To prevent unauthorized modification a
     * clone of the array is returned. Please cache appropriately.
     */
    val estimate: DoubleArray
    protected val kernel: Kernel

    /**
     * Returns the sum of all estimates in the accumulator.
     *
     * @return The total sum of all estimates.
     */
    var sumFreq = 0.0
        private set

    constructor(kernel: Kernel, size: Int) {
        estimate = DoubleArray(size)
        sumFreq = 0.0
        this.kernel = kernel
        require(kernel.size() <= estimate.size) { "The kernel size should be smaller than the acummulator size." }
    }

    constructor(
        kernel: Kernel,
        accumulator: DoubleArray
    ) {
        estimate = accumulator
        this.kernel = kernel
        require(kernel.size() <= accumulator.size) { "The kernel size should be smaller than the acummulator size." }
        calculateSumFreq()
    }

    /**
     * Add the kernel to an accumulator for each value.
     *
     *
     * When a kernel with a width of 7 is added at 1 cents it has influence on
     * the bins from 1200 - 7 * 10 + 1 to 1 + 7 * 10 so from 1131 to 71. To make
     * the modulo calculation easy 1200 is added to each value: -69 % 1200 is
     * -69, (-69 + 1200) % 1200 is the expected 1131. If you know what I mean.
     * This algorithm computes O(width * n) sums with n the number of
     * annotations and width the number of bins affected, rather efficient.
     *
     * @param value The value to add.
     */
    fun add(value: Double) {
        val accumulatorSize = estimate.size
        val calculationAria = kernel.size() / 2
        val start = (value + accumulatorSize - calculationAria).toInt()
        var stop = (value + accumulatorSize + calculationAria).toInt()
        if (kernel.size() % 2 != 0) stop++
        for (i in start until stop) {
            val kernelValue = kernel.value(i - start)
            estimate[i % accumulatorSize] += kernelValue
            sumFreq += kernelValue
        }
    }

    /**
     * Remove a value from the kde, removes a kernel at the specified position.
     *
     * @param value The value to remove.
     */
    fun remove(value: Double) {
        val accumulatorSize = estimate.size
        val calculationAria = kernel.size() / 2
        val start = (value + accumulatorSize - calculationAria).toInt()
        var stop = (value + accumulatorSize + calculationAria).toInt()
        if (kernel.size() % 2 != 0) stop++
        for (i in start until stop) {
            val kernelValue = kernel.value(i - start)
            estimate[i % accumulatorSize] -= kernelValue
            sumFreq -= kernelValue
        }
    }

    /**
     * Shift the accumulator x positions.
     *
     * @param shift The number of positions the accumulator should be shifted.
     */
    fun shift(shift: Int) {
        val newValues = DoubleArray(size())
        for (index in 0 until size()) {
            newValues[index] = estimate[(index + shift) % size()]
        }
        for (index in 0 until size()) {
            estimate[index] = newValues[index]
        }
    }

    /**
     * Map the kernel density estimate to another size. E.g. a KDE with 4 values
     * mapped to two is done by iterating the 4 elements and adding them on
     * modulo 2 places. Here 1 + 4 = 5, 2 + 9 = 11
     *
     * <pre>
     * (1 2 4 9).map(2) = (5 11)
    </pre> *
     *
     * @param size The new size for the KDE.
     * @return A new KDE with the contents of the original mapped to the new size.
     */
    fun map(size: Int): KernelDensityEstimate {
        val newKDE =
            KernelDensityEstimate(kernel, size)
        for (index in 0 until size()) {
            newKDE.estimate[index % size] += estimate[index]
        }
        newKDE.calculateSumFreq()
        return newKDE
    }

    /**
     * Return the value for the accumulator at a certain index.
     *
     * @param index The index.
     * @return The value for the accumulator at a certain index.
     */
    fun getValue(index: Int): Double {
        return estimate[index]
    }

    /**
     * @return The size of the accumulator.
     */
    fun size(): Int {
        return estimate.size
    }

    /**
     * Calculates the sum of all estimates in the accummulator. Should be called after each update.
     */
    private fun calculateSumFreq() {
        sumFreq = 0.0
        for (i in estimate.indices) {
            sumFreq += estimate[i]
        }
    }
    /**
     * Sets a new maximum bin value.
     *
     * @param newMaxvalue The new maximum bin value.
     */
    /**
     * Sets the maximum value in accumulator to 1.0
     */
    @JvmOverloads
    fun normalize(newMaxvalue: Double = 1.0) {
        val maxElement = maxElement
        val scaleFactor = newMaxvalue / maxElement
        if (maxElement > 0) {
            for (i in 0 until size()) {
                estimate[i] = estimate[i] * scaleFactor
            }
        }
        calculateSumFreq()
    }

    /**
     * @return the maximum element in the accumulator;
     */
    val maxElement: Double
        get() {
            var maxElement = 0.0
            for (i in 0 until size()) {
                maxElement = estimate[i].coerceAtLeast(maxElement)
            }
            return maxElement
        }

    /**
     * Sets the area under the curve to 1.0.
     * In essence every value is divided by getSumFreq().
     * As per definition of a probability density function.
     */
    fun pdfify() {
        val sumFreq = sumFreq
        if (sumFreq != 0.0) {
            for (i in estimate.indices) {
                estimate[i] = estimate[i] / sumFreq
            }
        }
        //reset sum freq
        calculateSumFreq()
        assert(sumFreq == 1.0)
    }

    /**
     * Clears the data in the accumulator.
     */
    fun clear() {
        for (i in estimate.indices) {
            estimate[i] = 0.0
        }
        //reset sum freq
        calculateSumFreq()
        assert(sumFreq == 0.0)
    }

    /**
     * Takes the maximum of the value in the accumulator for two kde's.
     *
     * @param other The other kde of the same size.
     */
    fun max(other: KernelDensityEstimate) {
        assert(other.size() == size()) { "The kde size should be the same!" }
        for (i in estimate.indices) {
            estimate[i] = estimate[i].coerceAtLeast(other.estimate[i])
        }
        calculateSumFreq()
    }

    /**
     * Adds a KDE to this accumulator
     *
     * @param other The other KDE of the same size.
     */
    fun add(other: KernelDensityEstimate) {
        assert(other.size() == size()) { "The kde size should be the same!" }
        for (i in estimate.indices) {
            estimate[i] += other.estimate[i]
        }
        calculateSumFreq()
    }

    /**
     *
     *
     * Calculate a correlation with another KernelDensityEstimate. The index of
     * the other estimates are shifted by a number which can be zero (or
     * positive or negative). Beware: the index wraps around the edges.
     *
     *
     *
     * This and the other KernelDensityEstimate should have the same size.
     *
     *
     * @param other                 The other estimate.
     * @param positionsToShiftOther The number of positions to shift the estimate.
     * @return A value between 0 and 1 representing how similar both estimates
     * are. 1 means total correlation, 0 no correlation.
     */
    fun correlation(
        other: KernelDensityEstimate,
        positionsToShiftOther: Int
    ): Double {
        assert(other.size() == size()) { "The kde size should be the same!" }
        val correlation: Double
        var matchingArea = 0.0
        val biggestKDEArea = sumFreq.coerceAtLeast(other.sumFreq)
        //an if, else to prevent modulo calculation
        if (positionsToShiftOther == 0) {
            for (i in estimate.indices) {
                matchingArea += estimate[i].coerceAtMost(other.estimate[i])
            }
        } else {
            for (i in estimate.indices) {
                val otherIndex = (i + positionsToShiftOther) % other.size()
                matchingArea += estimate[i].coerceAtMost(other.estimate[otherIndex])
            }
        }
        correlation = if (matchingArea == 0.0) {
            0.0
        } else {
            matchingArea / biggestKDEArea
        }
        return correlation
    }

    /**
     * Calculates how much the other KernelDensityEstimate needs to be shifted
     * for optimal correlation.
     *
     * @param other The other KernelDensityEstimate.
     * @return A number between 0 (inclusive) and the size of the
     * KernelDensityEstimate (exclusive) which represents how much the
     * other KernelDensityEstimate needs to be shifted for optimal
     * correlation.
     */
    fun shiftForOptimalCorrelation(other: KernelDensityEstimate): Int {
        var optimalShift = 0 // displacement with best correlation
        var maximumCorrelation = -1.0 // best found correlation
        for (shift in 0 until size()) {
            val currentCorrelation = correlation(other, shift)
            if (maximumCorrelation < currentCorrelation) {
                maximumCorrelation = currentCorrelation
                optimalShift = shift
            }
        }
        return optimalShift
    }

    /**
     * Calculates the optimal correlation between two Kernel Density Estimates
     * by shifting and searching for optimal correlation.
     *
     * @param other The other KernelDensityEstimate.
     * @return A value between 0 and 1 representing how similar both estimates
     * are. 1 means total correlation, 0 no correlation.
     */
    fun optimalCorrelation(other: KernelDensityEstimate): Double {
        val shift = shiftForOptimalCorrelation(other)
        return correlation(other, shift)
    }

    /**
     * Calculates the optimal correlation between two Kernel Density Estimates
     * by shifting and searching for optimal correlation.
     *
     * @param correlationMeasure
     * @param other              The other KernelDensityEstimate.
     * @return A value between 0 and 1 representing how similar both estimates
     * are. 1 means total correlation, 0 no correlation.
     */
    fun optimalCorrelation(
        correlationMeasure: KDECorrelation,
        other: KernelDensityEstimate
    ): Double {
        val shift = shiftForOptimalCorrelation(correlationMeasure, other)
        return correlationMeasure.correlation(this, other, shift)
    }

    /**
     * Calculates how much the other KernelDensityEstimate needs to be shifted
     * for optimal correlation.
     *
     * @param correlationMeasure
     * @param other              The other KernelDensityEstimate.
     * @return A number between 0 (inclusive) and the size of the
     * KernelDensityEstimate (exclusive) which represents how much the
     * other KernelDensityEstimate needs to be shifted for optimal
     * correlation.
     */
    fun shiftForOptimalCorrelation(
        correlationMeasure: KDECorrelation,
        other: KernelDensityEstimate
    ): Int {
        var optimalShift = 0 // displacement with best correlation
        var maximumCorrelation = -1.0 // best found correlation
        for (shift in 0 until size()) {
            val currentCorrelation =
                correlationMeasure.correlation(this, other, shift)
            if (maximumCorrelation < currentCorrelation) {
                maximumCorrelation = currentCorrelation
                optimalShift = shift
            }
        }
        return optimalShift
    }

    /**
     * Defines a kernel. It has a size and cached values for each index.
     *
     * @author Joren Six
     */
    interface Kernel {
        /**
         * Fetch the value for the kernel at a certain index.
         *
         * @param kernelIndex The index of the previously computed value.
         * @return The cached value for a certain index.
         */
        fun value(kernelIndex: Int): Double

        /**
         * The size of the kernel.
         *
         * @return The size of the kernel.
         */
        fun size(): Int
    }

    interface KDECorrelation {
        fun correlation(
            first: KernelDensityEstimate,
            other: KernelDensityEstimate,
            shift: Int
        ): Double
    }

    /**
     * A Gaussian kernel function.
     *
     * @author Joren Six
     */
    class GaussianKernel(kernelWidth: Double) :
        Kernel {
        private val kernel: DoubleArray
        override fun value(kernelIndex: Int): Double {
            return kernel[kernelIndex]
        }

        override fun size(): Int {
            return kernel.size
        }

        /**
         * Construct a kernel with a defined width.
         *
         * @param kernelWidth The width of the kernel.
         */
        init {
            val calculationAria = 5 * kernelWidth // Aria, not area
            val halfWidth = kernelWidth / 2.0

            // Compute a kernel: a lookup table with e.g. a Gaussian curve
            kernel = DoubleArray(calculationAria.toInt() * 2 + 1)
            var difference = -calculationAria
            for (i in kernel.indices) {
                val power = (difference / halfWidth).pow(2.0)
                kernel[i] = Math.E.pow(-0.5 * power)
                difference++
            }
        }
    }

    /**
     * A rectangular kernel function.
     */
    class RectangularKernel(kernelWidth: Double) :
        Kernel {
        private val kernel: DoubleArray = DoubleArray(kernelWidth.toInt()) {
            1.0
        }
        override fun value(kernelIndex: Int): Double {
            return kernel[kernelIndex]
        }

        override fun size(): Int {
            return kernel.size
        }
    }

    class Overlap : KDECorrelation {
        override fun correlation(
            first: KernelDensityEstimate,
            other: KernelDensityEstimate,
            shift: Int
        ): Double {
            val correlation: Double
            var matchingArea = 0
            for (i in 0 until first.size()) {
                val otherIndex = (other.size() + i + shift) % other.size()
                matchingArea += first.getValue(i).coerceAtMost(other.getValue(otherIndex))
                    .toInt()
            }
            val biggestKDEArea =
                first.sumFreq.coerceAtLeast(other.sumFreq)
            correlation = matchingArea / biggestKDEArea
            return correlation
        }
    }

    class Cosine : KDECorrelation {
        override fun correlation(
            first: KernelDensityEstimate,
            other: KernelDensityEstimate,
            shift: Int
        ): Double {
            val correlation: Double
            var innerProduct = 0.0
            var firstSquaredSum = 0.0
            var otherSquaredSum = 0.0
            for (i in 0 until first.size()) {
                val otherIndex = (other.size() + i + shift) % other.size()
                val firstValue = first.getValue(i)
                val otherValue = other.getValue(otherIndex)
                innerProduct += firstValue * otherValue
                firstSquaredSum += firstValue * firstValue
                otherSquaredSum += otherValue * otherValue
            }
            correlation =
                innerProduct / (firstSquaredSum.pow(0.5) * otherSquaredSum.pow(0.5))
            return correlation
        }
    }
}