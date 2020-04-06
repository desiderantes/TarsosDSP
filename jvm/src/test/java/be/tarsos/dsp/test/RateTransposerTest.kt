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

import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFloatArray
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromInputStream
import be.tarsos.dsp.io.jvm.AudioPlayer
import be.tarsos.dsp.resample.RateTransposer
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

class RateTransposerTest {
    @Test
    @Throws(
        UnsupportedAudioFileException::class,
        LineUnavailableException::class,
        IOException::class
    )
    fun testTransposeSine() {
        val audioBuffer = TestUtilities.audioBufferSine()
        val factor = 1.2
        val sampleRate = 44100
        val w = WaveformSimilarityBasedOverlapAdd(
            WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(
                1.2,
                sampleRate.toDouble()
            )
        )
        val d = fromFloatArray(
            audioBuffer,
            sampleRate,
            w.inputBufferSize,
            w.overlap
        )
        val f =
            AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        w.setDispatcher(d)
        d.addAudioProcessor(w)
        d.addAudioProcessor(RateTransposer(factor))
        d.addAudioProcessor(AudioPlayer(f))
        d.run()
    }

    @Test
    @Throws(
        LineUnavailableException::class,
        UnsupportedAudioFileException::class,
        IOException::class
    )
    fun testTransposeFlute() {
        val factor = 1.2
        val sampleRate = 44100
        val w = WaveformSimilarityBasedOverlapAdd(
            WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(
                1.2,
                sampleRate.toDouble()
            )
        )
        val d = fromInputStream(
            TestUtilities.fluteFile(),
            w.inputBufferSize,
            w.overlap
        )
        val f =
            AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        w.setDispatcher(d)
        d.addAudioProcessor(w)
        d.addAudioProcessor(RateTransposer(factor))
        d.addAudioProcessor(AudioPlayer(f))
        d.run()
    }
}