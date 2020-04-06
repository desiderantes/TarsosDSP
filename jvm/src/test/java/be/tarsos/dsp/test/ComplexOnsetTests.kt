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
package be.tarsos.dsp.test

import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromInputStream
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

class ComplexOnsetTests {
    @Test
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun testOnsets() {
        val inputStream = TestUtilities.onsetsAudioFile()
        val contents =
            TestUtilities.readFileFromJar("NR45_expected_onsets_complex.txt")
        val onsetStrings = contents.split("\n").toTypedArray()
        val expectedOnsets = DoubleArray(onsetStrings.size)
        var i = 0
        for (onset in onsetStrings) {
            expectedOnsets[i] = onset.toDouble()
            i++
        }
        val d =
            fromInputStream(inputStream, 512, 256)
        //use the same default params as aubio:
        val cod = ComplexOnsetDetector(512, 0.3, 256.0 / 44100.0 * 4.0, -70.0)
        d.addAudioProcessor(cod)
        cod.setHandler(object : OnsetHandler {
            var i = 1
            override fun handleOnset(
                actualTime: Double,
                salience: Double
            ) {
                val expectedTime = expectedOnsets[i]
                println(actualTime)
                Assertions.assertEquals(
                    expectedTime,
                    actualTime,
                    0.017417,
                    "Onset time should be the expected value!"
                )
                i++
            }
        })
        d.run()
    }
}