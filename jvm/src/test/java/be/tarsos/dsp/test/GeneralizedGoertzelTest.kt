package be.tarsos.dsp.test

import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromInputStream
import be.tarsos.dsp.pitch.GeneralizedGoertzel
import be.tarsos.dsp.pitch.Goertzel.FrequenciesDetectedHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

class GeneralizedGoertzelTest {
    private var symbolsAccumulator = ""

    //see https://en.wikipedia.org/wiki/Selcall
    @Test
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun testSelCallDetection() {
        val frequencies = doubleArrayOf(
            1981.0,
            1124.0,
            1197.0,
            1275.0,
            1358.0,
            1446.0,
            1540.0,
            1640.0,
            1747.0,
            1860.0,
            2400.0,
            930.0,
            2247.0,
            991.0,
            2110.0,
            1055.0
        )
        val symbols = arrayOf(
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "A",
            "B",
            "C",
            "D",
            "E",
            "F"
        )
        val handler: FrequenciesDetectedHandler = object : FrequenciesDetectedHandler {
            var prevSymbol = ""
            override fun handleDetectedFrequencies(
                time: Double,
                frequencies: DoubleArray,
                powers: DoubleArray,
                allFrequencies: DoubleArray,
                allPowers: DoubleArray
            ) {
                var maxIndex = 0
                var maxPower = 0.0
                for (i in frequencies.indices) {
                    if (powers[i] > maxPower) {
                        maxPower = powers[i]
                        maxIndex = i
                    }
                }
                if (maxPower > 20) {
                    val symbol = symbols[maxIndex]
                    if (!symbol.equals(prevSymbol, ignoreCase = true)) {
                        //System.out.println(frequencies[maxIndex] +"\t" + powers[maxIndex]+ "\t" + symbol);
                        symbolsAccumulator += symbol
                        //System.out.println(symbolsAccumulator);
                    }
                    prevSymbol = symbol
                }
            }
        }
        val blockSize = 205
        val sampleRate = 8000
        val generalized: AudioProcessor =
            GeneralizedGoertzel(sampleRate.toFloat(), blockSize, frequencies, handler)
        //AudioProcessor classic = new Goertzel(44100, 2048,frequenciesToDetect, handler);
        val stream = TestUtilities.ccirFile()
        val ad =
            fromInputStream(stream, blockSize, 0)
        ad.addAudioProcessor(generalized)
        ad.run()
        Assertions.assertEquals(symbolsAccumulator, "042E1", "The selCall decoded it")
    }
}