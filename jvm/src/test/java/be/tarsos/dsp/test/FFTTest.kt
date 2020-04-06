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
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.jvm.AudioPlayer
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toAudioFormat
import be.tarsos.dsp.util.fft.FFT
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.PI
import kotlin.math.sin

class FFTTest {
    @Test
    @Throws(UnsupportedAudioFileException::class, LineUnavailableException::class)
    fun testForwardAndBackwardsFFT() {
        val floatBuffer = testAudioBufferSine()
        val format = TarsosDSPAudioFormat(44100F, 16, 1, true, false)
        val converter =
            TarsosDSPAudioFloatConverter.getConverter(format)
        val byteBuffer =
            ByteArray(floatBuffer.size * format.frameSize)
        Assertions.assertEquals(
            2,
            format.frameSize,
            "Specified 16 bits so framesize should be 2."
        )
        converter.toByteArray(floatBuffer, byteBuffer)
        val bais = ByteArrayInputStream(byteBuffer)
        val inputStream =
            AudioInputStream(bais, toAudioFormat(format), floatBuffer.size.toLong())
        val stream = JVMAudioInputStream(inputStream)
        val dispatcher = AudioDispatcher(stream, 1024, 0)
        dispatcher.addAudioProcessor(object : AudioProcessor {
            private val fft = FFT(512)
            override fun processingFinished() {}
            override fun process(audioEvent: AudioEvent): Boolean {
                val audioFloatBuffer = audioEvent.floatBuffer
                fft.forwardTransform(audioFloatBuffer)
                fft.backwardsTransform(audioFloatBuffer)
                return true
            }
        })
        dispatcher.addAudioProcessor(
            AudioPlayer(
                toAudioFormat(
                    format
                )
            )
        )
        dispatcher.run()
    }

    companion object {
        /**
         * Constructs and returns a buffer of a two seconds long pure sine of 440Hz
         * sampled at 44.1kHz.
         *
         * @return A buffer of a two seconds long pure sine (440Hz) sampled at
         * 44.1kHz.
         */
        fun testAudioBufferSine(): FloatArray {
            val sampleRate = 44100.0
            val f0 = 440.0
            val amplitudeF0 = 0.5
            val seconds = 2.0
            return FloatArray((seconds * sampleRate).toInt()){ sample ->
                val time = sample / sampleRate
                return@FloatArray (amplitudeF0 * sin(2 * PI * f0 * time)).toFloat()
            }
        }
    }
}