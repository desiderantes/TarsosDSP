package be.tarsos.dsp.wavelet.lift

/**
 * @author Ian Kaplan
 */
internal class PolynomialInterpolation {
    /**
     * Table for 4-point interpolation coefficients
     */
    private val fourPointTable: Array<FloatArray>

    /**
     * Table for 2-point interpolation coefficients
     */
    private val twoPointTable: Array<FloatArray>

    /**
     *
     *
     * The polynomial interpolation algorithm assumes that the known points are
     * located at x-coordinates 0, 1,.. N-1. An interpolated point is calculated
     * at ***x***, using N coefficients. The polynomial coefficients for
     * the point ***x*** can be calculated staticly, using the Lagrange
     * method.
     *
     *
     * @param x the x-coordinate of the interpolated point
     * @param N the number of polynomial points.
     * @param c an array for returning the coefficients
     */
    private fun lagrange(x: Float, N: Int, c: FloatArray) {
        var num: Float
        var denom: Float
        for (i in 0 until N) {
            num = 1f
            denom = 1f
            for (k in 0 until N) {
                if (i != k) {
                    num *= (x - k)
                    denom *= (i - k)
                }
            }
            c[i] = num / denom
        }
    }

    /**
     *
     *
     * For a given N-point polynomial interpolation, fill the coefficient table,
     * for points 0.5 ... (N-0.5).
     *
     */
    private fun fillTable(N: Int, table: Array<FloatArray>) {
        var x: Float
        val n = N.toFloat()
        var i = 0
        x = 0.5f
        while (x < n) {
            lagrange(x, N, table[i])
            i++
            x += 1.0f
        }
    }

    /**
     * Print an N x N table polynomial coefficient table
     */
    private fun printTable(table: Array<FloatArray>, N: Int) {
        println("$N-point interpolation table:")
        var x = 0.5
        for (i in 0 until N) {
            print("$x: ")
            for (j in 0 until N) {
                print(table[i][j])
                if (j < N - 1) print(", ")
            }
            println()
            x += 1.0
        }
    }

    /**
     * Print the 4-point and 2-point polynomial coefficient tables.
     */
    fun printTables() {
        printTable(fourPointTable, numPts)
        printTable(twoPointTable, 2)
    }

    /**
     *
     *
     * For the polynomial interpolation point x-coordinate ***x***,
     * return the associated polynomial interpolation coefficients.
     *
     *
     * @param x the x-coordinate for the interpolated pont
     * @param n the number of polynomial interpolation points
     * @param c an array to return the polynomial coefficients
     */
    private fun getCoef(x: Float, n: Int, c: FloatArray) {
        var table: Array<FloatArray>? = null
        val j = x.toInt()
        if (j < 0 || j >= n) {
            println(
                "PolynomialWavelets::getCoef: n = " + n
                        + ", bad x value"
            )
        }
        when (n) {
            numPts -> {
                table = fourPointTable
            }
            2 -> {
                table = twoPointTable
                c[2] = 0.0f
                c[3] = 0.0f
            }
            else -> {
                println("PolynomialWavelets::getCoef: bad value for N")
            }
        }
        if (table != null) {
            System.arraycopy(table[j], 0, c, 0, n)
        }
    }

    /**
     *
     *
     * Given four points at the x,y coordinates {0,d<sub>0</sub>},
     * {1,d<sub>1</sub>}, {2,d<sub>2</sub>}, {3,d<sub>3</sub>} return the
     * y-coordinate value for the polynomial interpolated point at
     * ***x***.
     *
     *
     * @param x the x-coordinate for the point to be interpolated
     * @param N the number of interpolation points
     * @param d an array containing the y-coordinate values for the known
     * points (which are located at x-coordinates 0..N-1).
     * @return the y-coordinate value for the polynomial interpolated point at
     * ***x***.
     */
    fun interpPoint(x: Float, N: Int, d: FloatArray): Float {
        val c = FloatArray(numPts)
        var point = 0f
        var n = numPts
        if (N < numPts) n = N
        getCoef(x, n, c)
        if (n == numPts) {
            point =
                c[0] * d[0] + c[1] * d[1] + c[2] * d[2] + c[3] * d[3]
        } else if (n == 2) {
            point = c[0] * d[0] + c[1] * d[1]
        }
        return point
    }

    companion object {
        /**
         * number of polynomial interpolation ponts
         */
        private const val numPts = 4
    }

    /**
     *
     *
     * PolynomialWavelets constructor
     *
     *
     *
     * Build the 4-point and 2-point polynomial coefficient tables.
     *
     */
    init {

        // Fill in the 4-point polynomial interplation table
        // for the points 0.5, 1.5, 2.5, 3.5
        fourPointTable = Array(
            numPts
        ) { FloatArray(numPts) }
        fillTable(numPts, fourPointTable)

        // Fill in the 2-point polynomial interpolation table
        // for 0.5 and 1.5
        twoPointTable = Array(2) { FloatArray(2) }
        fillTable(2, twoPointTable)
    }
}
