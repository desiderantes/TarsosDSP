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
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toAudioFormat
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toTarsosDSPFormat
import be.tarsos.dsp.pitch.DTMF
import be.tarsos.dsp.pitch.Goertzel
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

class GoertzelTest {
    /**
     * Test detection of a simple sine wave (one frequency).
     *
     * @throws LineUnavailableException
     * @throws UnsupportedAudioFileException
     */
    @Test
    @Throws(LineUnavailableException::class, UnsupportedAudioFileException::class)
    fun testDetection() {
        val floatSinBuffers = arrayOf(
            testAudioBufferSine(6000.0, 10240),
            testAudioBufferSine(2000.0, 10240),
            testAudioBufferSine(4000.0, 10240)
        )
        val floatBuffer = appendBuffers(*floatSinBuffers)
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
        val frequencies = doubleArrayOf(6000.0, 3000.0, 5000.0, 5800.0, 6500.0)
        dispatcher.addAudioProcessor(
            Goertzel(
                44100F,
                1024,
                frequencies) { time, frequencies, powers, allFrequencies, allPowers ->
                    Assertions.assertEquals(
                        frequencies[0].toInt(),
                        6000,
                        "Should only detect 6000 Hz"
                    )
                }
        )
        //dispatcher.addAudioProcessor(new BlockingAudioPlayer(format,1024, 0));
        dispatcher.run()
    }

    /**
     * Test detection of multiple frequencies.
     *
     * @throws LineUnavailableException
     * @throws UnsupportedAudioFileException
     */
    @Test
    @Throws(LineUnavailableException::class, UnsupportedAudioFileException::class)
    fun testDTMF() {
        val floatSinBuffers = arrayOf(
            DTMF.generateDTMFTone('1'),
            DTMF.generateDTMFTone('2'),
            DTMF.generateDTMFTone('3'),
            DTMF.generateDTMFTone('4'),
            DTMF.generateDTMFTone('5'),
            DTMF.generateDTMFTone('6'),
            DTMF.generateDTMFTone('7'),
            DTMF.generateDTMFTone('8'),
            DTMF.generateDTMFTone('9')
        )
        val floatBuffer = appendBuffers(*floatSinBuffers)
        val stepSize = 512
        val format =
            AudioFormat(44100F, 16, 1, true, false)
        val converter =
            TarsosDSPAudioFloatConverter.getConverter(toTarsosDSPFormat(format))
        val byteBuffer =
            ByteArray(floatBuffer.size * format.frameSize)
        Assertions.assertEquals(
            2,
            format.frameSize,
            "Specified 16 bits so framesize should be 2."
        )
        converter.toByteArray(floatBuffer, byteBuffer)
        val bais = ByteArrayInputStream(byteBuffer)
        val inputStream = AudioInputStream(bais, format, floatBuffer.size.toLong())
        val stream = JVMAudioInputStream(inputStream)
        val dispatcher = AudioDispatcher(stream, stepSize, 0)
        val data = StringBuilder()
        dispatcher.addAudioProcessor(
            Goertzel(
                44100F,
                stepSize,
                DTMF.DTMF_FREQUENCIES) { time, frequencies, powers, allFrequencies, allPowers -> // assertEquals("Should detect 2 frequencies.",2,frequencies.length);
                    Assertions.assertEquals(
                        frequencies.size,
                        powers.size,
                        "Number of frequencies should be the same as the number of powers."
                    )
                    if (frequencies.size == 2) {
                        var rowIndex = -1
                        var colIndex = -1
                        for (i in 0..3) {
                            if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i]
                            ) rowIndex = i
                        }
                        for (i in 4 until DTMF.DTMF_FREQUENCIES.size) {
                            if (frequencies[0] == DTMF.DTMF_FREQUENCIES[i] || frequencies[1] == DTMF.DTMF_FREQUENCIES[i]
                            ) colIndex = i - 4
                        }
                        if (rowIndex >= 0 && colIndex >= 0) {
                            val character =
                                DTMF.DTMF_CHARACTERS[rowIndex][colIndex]
                            if (data.isEmpty() || character != data[data.length - 1]) {
                                data.append(character)
                            }
                        }
                    }
                })

        //dispatcher.addAudioProcessor(new BlockingAudioPlayer(format, stepSize, 0));
        dispatcher.run()
        Assertions.assertEquals("123456789", data.toString(), "Decoded string should be 123456789")
        Assertions.assertEquals(9, data.length, "Length should be 9")
    }

    companion object {
        /**
         * Generate a buffer with one sine wave.
         *
         * @param f0   the frequency of the sine wave;
         * @param size the size of the buffer in samples.
         * @return a buffer (float array) with audio information for the sine wave.
         */
        fun testAudioBufferSine(f0: Double, size: Int): FloatArray {
            val sampleRate = 44100.0
            val amplitudeF0 = 1.0
            val buffer = FloatArray(size)
            for (sample in buffer.indices) {
                val time = sample / sampleRate
                buffer[sample] = (amplitudeF0 * Math.sin(
                    2 * Math.PI * f0
                            * time
                )).toFloat()
            }
            return buffer
        }

        /**
         * Append float buffers to form one big float buffer.
         *
         * @param floatBuffers The float buffers to append.
         * @return An appended float buffer with all the information in the array of
         * buffers.
         */
        inline fun appendBuffers(vararg floatBuffers: FloatArray): FloatArray {
            return floatBuffers.reduce { acc, floats -> acc + floats }
        }
    }
}