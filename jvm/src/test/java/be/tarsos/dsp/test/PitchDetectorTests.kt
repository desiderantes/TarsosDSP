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

import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import be.tarsos.dsp.test.JVMTestUtilities.audioBufferFlute
import be.tarsos.dsp.test.JVMTestUtilities.audioBufferHighFlute
import be.tarsos.dsp.test.JVMTestUtilities.audioBufferLowPiano
import be.tarsos.dsp.test.JVMTestUtilities.audioBufferPiano
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PitchDetectorTests {
    @Test
    fun testSine() {
        val audioBuffer = TestUtilities.audioBufferSine()
        for (algorithm in PitchEstimationAlgorithm.values()) {
            val detector = algorithm.getDetector(44100f, 1024)
            var pitch = 0f
            val shortAudioBuffer = FloatArray(1024)
            System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.size)
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            Assertions.assertEquals(
                440.0,
                pitch.toDouble(),
                1.5,
                "Expected about 440Hz for $algorithm"
            )
            System.arraycopy(
                audioBuffer,
                1024,
                shortAudioBuffer,
                0,
                shortAudioBuffer.size
            )
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            Assertions.assertEquals(
                440.0,
                pitch.toDouble(),
                1.5,
                "Expected about 440Hz for $algorithm"
            )
            System.arraycopy(
                audioBuffer,
                2048,
                shortAudioBuffer,
                0,
                shortAudioBuffer.size
            )
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            Assertions.assertEquals(
                440.0,
                pitch.toDouble(),
                1.5,
                "Expected about 440Hz for $algorithm"
            )
        }
        println()
    }

    @Test
    fun testFlute() {
        val audioBuffer = audioBufferFlute()
        for (algorithm in PitchEstimationAlgorithm.values()) {
            val detector = algorithm.getDetector(44100f, 1024)
            var pitch = 0f
            val shortAudioBuffer = FloatArray(1024)
            System.arraycopy(
                audioBuffer,
                2048,
                shortAudioBuffer,
                0,
                shortAudioBuffer.size
            )
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            Assertions.assertEquals(442f, pitch, 2f, "Expected about 440Hz for $algorithm")
        }
        println()
    }

    @Test
    fun testPiano() {
        val audioBuffer = audioBufferPiano()
        for (algorithm in PitchEstimationAlgorithm.values()) {
            val detector = algorithm.getDetector(44100f, 1024)
            var pitch = 0f
            val shortAudioBuffer = FloatArray(1024)
            System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.size)
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            Assertions.assertEquals(443f, pitch, 3f, "Expected about 440Hz for $algorithm")
        }
        println()
    }

    @Test
    fun testLowPiano() {
        val audioBuffer = audioBufferLowPiano()
        for (algorithm in PitchEstimationAlgorithm.values()) {
            val detector = algorithm.getDetector(44100f, 1024)
            var pitch = 0f
            val shortAudioBuffer = FloatArray(1024)
            System.arraycopy(audioBuffer, 0, shortAudioBuffer, 0, shortAudioBuffer.size)
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            Assertions.assertEquals(
                130.81,
                pitch.toDouble(),
                2.0,
                "Expected about 130.81Hz for $algorithm"
            )
        }
        println()
    }

    @Test
    fun testHighFlute() {
        val audioBuffer = audioBufferHighFlute()
        for (algorithm in PitchEstimationAlgorithm.values()) {
            val detector = algorithm.getDetector(44100f, 1024)
            var pitch = 0f
            val shortAudioBuffer = FloatArray(1024)
            System.arraycopy(
                audioBuffer,
                3000,
                shortAudioBuffer,
                0,
                shortAudioBuffer.size
            )
            pitch = detector.getPitch(shortAudioBuffer).pitch
            println(String.format("%15s %8.3f Hz", algorithm, pitch))
            //this fails with dynamic wavelet and amdf
            //assertEquals("Expected about 1975.53Hz for " + algorithm,1975.53,pitch,30);
        }
        println()
    }
}