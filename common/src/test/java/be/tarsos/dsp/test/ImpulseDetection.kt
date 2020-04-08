package be.tarsos.dsp.test

import be.tarsos.dsp.resample.Resampler
import be.tarsos.dsp.util.TWO_PI
import be.tarsos.dsp.wavelet.lift.Daubechies4Wavelet
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

object ImpulseDetection {
    @JvmStatic
    fun main(args: Array<String>) {
        val sampleRate = 2000.0
        val frequency = 8.0
        val amplitude = 0.8
        val twoPiF = TWO_PI * frequency
        val data = FloatArray(512)
        for (sample in data.indices) {
            val time = sample / sampleRate
            data[sample] = (amplitude * sin(twoPiF * time)).toFloat()
        }
        for (sample in data.indices) {
            //System.out.println(data[sample]);
        }

        /*
		float amount = 10;
		for(int sample = 205 ; sample < 215 ; sample++){
			data[sample] = (sample-205)/amount;
		}
		*/data[62] = 1.0f
        val dwt = Daubechies4Wavelet()
        dwt.forwardTrans(data)
        val levelFive = mra(data, 5) //31.25 - 62.5
        val levelFour = mra(data, 4) //62.5-125
        val levelThree = mra(data, 3) //125-250
        val levelTwo = mra(data, 2) //250-500Hz
        val levelOne = mra(data, 1) //500-100Hz
        dwt.inverseTrans(data)
        normalize(data)
        for (i in levelFive.indices) {
            println(
                i.toString() + ";" + data[i] + ";" + levelFive[i] + ";" + levelFour[i] + ";" + levelThree[i] + ";" + levelTwo[i] + ";" + levelOne[i]
            )
        }
        var maxValue = 0.0
        var maxIndex = 0
        for (i in levelOne.indices) {
            if (abs(levelOne[i]) > maxValue) {
                maxIndex = i
                maxValue = abs(levelOne[i]).toDouble()
            }
        }
        println("Anomaly at $maxIndex")
    }

    private fun mra(data: FloatArray, level: Int): FloatArray {
        val length = 2.0.pow(level.toDouble()).toInt()
        val startIndex = (data.size / 2.0.pow(level.toDouble())).toInt()
        val stopIndex = (data.size / 2.0.pow(level - 1.toDouble())).toInt()
        val part = FloatArray(stopIndex - startIndex)
        var j = 0
        for (i in startIndex until stopIndex) {
            part[j] = -1 * data[i]
            j++
        }
        normalize(part)
        val factor = data.size / part.size.toFloat()
        val r = Resampler(false, factor.toDouble(), factor.toDouble())
        val out = FloatArray((part.size * factor).toInt())
        r.process(factor.toDouble(), part, 0, part.size, false, out, 0, out.size)
        val mra = FloatArray(data.size)
        j = 0
        for (i in out.indices) {
            if ((i + length / 2) % length == 0) {
                mra[i] = part[j]
                j++
            }
        }
        return mra
    }

    private fun normalize(data: FloatArray) {
        var maxValue = 0f
        for (i in data.indices) {
            maxValue = max(abs(data[i]), maxValue)
        }
        for (i in data.indices) {
            data[i] = data[i] / maxValue
        }
    }
}