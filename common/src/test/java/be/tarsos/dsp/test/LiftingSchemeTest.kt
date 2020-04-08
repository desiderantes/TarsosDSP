package be.tarsos.dsp.test

import be.tarsos.dsp.wavelet.lift.*

internal object LiftingSchemeTest {
    private fun print(values: FloatArray) {
        print("[")
        for (`val` in values) {
            print(`val`)
            print(",")
        }
        println("]")
    }

    @JvmStatic
    fun main(args: Array<String>) {

        /*
         * double vals[] = { 32.0, 10.0, 20.0, 38.0, 37.0, 28.0, 38.0, 34.0,
         * 18.0, 24.0, 18.0, 9.0, 23.0, 24.0, 28.0, 34.0 };
         */
        val vals = floatArrayOf(25f, 40f, 8f, 24f, 48f, 48f, 40f, 16f)
        /*
         * double vals[] = { 77.6875, 78.1875, 82.0625, 85.5625, 86.7500,
         * 82.4375, 82.2500, 82.7500, 81.2500, 79.5625, 80.2813, 79.8750,
         * 77.7500, 74.7500, 78.5000, 79.1875, 78.8125, 80.3125, 80.1250,
         * 79.3125, 83.7500, 89.8125, 87.7500, 91.1250, 94.4375, 92.7500,
         * 98.0000, 97.1875, 99.4375, 101.7500, 108.5000, 109.0000, 105.2500,
         * 105.5000, 110.0000, 107.0000, 107.2500, 103.3125, 102.8750, 102.4375,
         * 102.0000, 101.3125, 97.4375, 100.5000, 107.7500, 110.2500, 114.3125,
         * 111.2500, 114.8125, 112.6875, 109.4375, 108.0625, 104.5625, 103.2500,
         * 110.5625, 110.7500, 116.3125, 123.6250, 120.9375, 121.6250, 127.6875,
         * 126.0625, 126.3750, 124.3750 };
         */
        val hr = HaarWavelet()
        val ln = LineWavelet()
        val d = Daubechies4Wavelet()
        val hrpy =
            HaarWithPolynomialInterpolationWavelet()
        val py = PolynomialWavelets()
        println("Data:")
        print(vals)
        println()
        println("HaarWavelet:")
        hr.forwardTrans(vals)
        print(vals)
        println()
        hr.inverseTrans(vals)
        print(vals)
        println()
        println("Daubechies4Wavelet:")
        d.forwardTrans(vals)
        print(vals)
        println()
        d.inverseTrans(vals)
        print(vals)
        println()
        println("Line:")
        ln.forwardTrans(vals)
        print(vals)
        println()
        ln.inverseTrans(vals)
        print(vals)
        println()
        println("HaarWavelet, extended with polynomial interpolation:")
        hrpy.forwardTrans(vals)
        print(vals)
        println()
        hrpy.inverseTrans(vals)
        print(vals)
        println()
        println("Poly:")
        py.forwardTrans(vals)
        print(vals)
        println()
        py.inverseTrans(vals)
        print(vals)
        println()
        val t = floatArrayOf(56f, 40f, 8f, 24f, 48f, 48f, 40f, 16f)
        hr.forwardTransOne(t)
        val signal = floatArrayOf(56f, 40f, 8f, 24f, 48f, 48f, 40f, 16f)
        dwtHaar(signal)
    }

    private fun dwtHaar(signal: FloatArray) {
        val s = FloatArray(signal.size)
        val d = FloatArray(signal.size)
        for (i in 0 until signal.size / 2) {
            s[i] = (signal[2 * i] + signal[2 * i + 1]) / 2.0f
            d[i] = signal[2 * i] - s[i]
        }
        print(s)
        print(d)
    } /*
     * private static void decompose(float[] signal) { int length =
     * signal.length; int steps = (int) Math.round(Math.log(length) /
     * Math.log(2)); }
     */
}