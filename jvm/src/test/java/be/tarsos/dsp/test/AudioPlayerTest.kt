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

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.PipedAudioStream
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFloatArray
import be.tarsos.dsp.io.jvm.AudioPlayer
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toAudioFormat
import org.junit.jupiter.api.Test
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

class AudioPlayerTest {
    @Test
    @Throws(UnsupportedAudioFileException::class, LineUnavailableException::class)
    fun testAudioPlayer() {
        val sine = TestUtilities.audioBufferSine()
        val dispatcher =
            fromFloatArray(sine, 44100, 10000, 128)
        dispatcher.addAudioProcessor(
            AudioPlayer(
                toAudioFormat(
                    dispatcher.format
                )
            )
        )
        dispatcher.run()
    }

    @Throws(UnsupportedAudioFileException::class, LineUnavailableException::class)
    fun testStreamAudioPlayer() {
        val file = PipedAudioStream("http://mp3.streampower.be/stubru-high.mp3")
        val stream = file.getMonoStream(44100, 0.0)
        val d: AudioDispatcher
        d = AudioDispatcher(stream, 1024, 128)
        d.addAudioProcessor(AudioPlayer(toAudioFormat(d.format)))
        d.run()
    }
}