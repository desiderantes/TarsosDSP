package be.tarsos.dsp.test

import be.tarsos.dsp.wavelet.HaarWaveletTransform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HaarWaveletTransformTest {
    @Test
    fun testTransform() {
        val ht = HaarWaveletTransform()
        val data = floatArrayOf(5f, 1f, 2f, 8f)
        ht.transform(data)
        val expected = floatArrayOf(4f, 2f, -1f, -3f)
        assertArrayEquals(expected, data)
        val otherData = floatArrayOf(3f, 1f, 0f, 4f, 8f, 6f, 9f, 9f)
        ht.transform(otherData)
        val expectedResult = floatArrayOf(5f, 1f, 0f, -2f, -3f, 1f, -1f, 0f)
        assertArrayEquals(expectedResult, otherData)
    }

    @Test
    fun testInverseTransform() {
        val ht = HaarWaveletTransform()
        val data = floatArrayOf(4f, 2f, -1f, -3f)
        ht.inverseTransform(data)
        val expected = floatArrayOf(5f, 1f, 2f, 8f)
        assertArrayEquals(expected, data)
        val otherData = floatArrayOf(5f, 1f, 0f, -2f, -3f, 1f, -1f, 0f)
        ht.inverseTransform(otherData)
        val expectedResult = floatArrayOf(3f, 1f, 0f, 4f, 8f, 6f, 9f, 9f)
        assertArrayEquals(expectedResult, otherData)
    }

    private fun assertArrayEquals(expecteds: FloatArray, actuals: FloatArray) {
        for (i in expecteds.indices) {
            Assertions.assertEquals(expecteds[i], actuals[i], 0.0001f)
        }
    }
}