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
import be.tarsos.dsp.filters.HighPass
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.jvm.AudioPlayer
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toAudioFormat
import be.tarsos.dsp.io.jvm.WaveformWriter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.PI
import kotlin.math.sin

class TestFilters {
    @Throws(UnsupportedAudioFileException::class, LineUnavailableException::class)
    fun testFilters() {
        val floatBuffer = testAudioBufferSine()
        val format = TarsosDSPAudioFormat(44100F, 16, 1, true, false)
        val converter = TarsosDSPAudioFloatConverter.getConverter(format)
        val byteBuffer = ByteArray(floatBuffer.size * format.frameSize)
        Assertions.assertEquals(2, format.frameSize, "Specified 16 bits so framesize should be 2.")
        converter.toByteArray(floatBuffer, byteBuffer)
        val bais = ByteArrayInputStream(byteBuffer)
        val inputStream = AudioInputStream(bais, toAudioFormat(format), floatBuffer.size.toLong())
        val jvmAudioInputStream = JVMAudioInputStream(inputStream)
        val dispatcher = AudioDispatcher(jvmAudioInputStream, 1024, 0)
        dispatcher.addAudioProcessor(LowPassFS(1000F, 44100F))
        dispatcher.addAudioProcessor(HighPass(100F, 44100F))
        dispatcher.addAudioProcessor(AudioPlayer(toAudioFormat(format)))
        dispatcher.run()
    }

    @Test
    @Throws(
        UnsupportedAudioFileException::class,
        LineUnavailableException::class,
        IOException::class
    )
    fun testFilterOnFile() {
        val stream = TestUtilities.fluteFile()
        val format = AudioSystem.getAudioFileFormat(stream).format
        val stepSize = 2048 //samples
        val overlap = 0 //samples;
        val sampleRate = format.sampleRate
        val startFrequency = 200f
        val stopFrequency = 800f
        val inputStream = AudioSystem.getAudioInputStream(stream)
        val jvmAudioInputStream = JVMAudioInputStream(inputStream)
        val dispatcher = AudioDispatcher(jvmAudioInputStream, stepSize, overlap)
        dispatcher.addAudioProcessor(HighPass(startFrequency, sampleRate))
        dispatcher.addAudioProcessor(LowPassFS(stopFrequency, sampleRate))
        dispatcher.addAudioProcessor(WaveformWriter(format, "filtered.wav"))
        dispatcher.run()
    }

    companion object {
        fun testAudioBufferSine(): FloatArray {
            val sampleRate = 44100.0
            val f0 = 440.0
            val amplitudeF0 = 0.5
            val seconds = 2.0
            val buffer = FloatArray((seconds * sampleRate).toInt())
            for (sample in buffer.indices) {
                val time = sample / sampleRate
                buffer[sample] = (amplitudeF0 * sin(2 * PI * f0 * time)).toFloat()
            }
            return buffer
        }
    }
}