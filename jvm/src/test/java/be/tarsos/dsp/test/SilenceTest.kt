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
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.jvm.AudioPlayer
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toTarsosDSPFormat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

class SilenceTest {
    @Test
    @Throws(UnsupportedAudioFileException::class, LineUnavailableException::class)
    fun testSilenceDetector() {
        val floatSinBuffer = TestUtilities.audioBufferSine()
        val floatSilenceBuffer = TestUtilities.audioBufferSilence()
        val floatBuffer =
            FloatArray(floatSinBuffer.size + 2 * floatSilenceBuffer.size)
        var i = floatSilenceBuffer.size
        while (i < floatSilenceBuffer.size + floatSinBuffer.size) {
            floatBuffer[i] = floatSinBuffer[i - floatSilenceBuffer.size]
            i++
        }
        val format =
            AudioFormat(44100F, 16, 1, true, false)
        val converter = TarsosDSPAudioFloatConverter
            .getConverter(toTarsosDSPFormat(format))
        val byteBuffer = ByteArray(
            floatBuffer.size
                    * format.frameSize
        )
        Assertions.assertEquals(
            2,
            format.frameSize, "Specified 16 bits so framesize should be 2."
        )
        converter.toByteArray(floatBuffer, byteBuffer)
        val bais = ByteArrayInputStream(byteBuffer)
        val inputStream = AudioInputStream(
            bais, format,
            floatBuffer.size.toLong()
        )
        val stream = JVMAudioInputStream(inputStream)
        val dispatcher = AudioDispatcher(
            stream,
            1024, 0
        )
        dispatcher.addAudioProcessor(SilenceDetector())
        dispatcher.addAudioProcessor(AudioPlayer(format))
        dispatcher.run()
    }
}