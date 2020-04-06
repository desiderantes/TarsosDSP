package be.tarsos.dsp.test

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.PipedAudioStream
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

class AudioDispatcherTest {
    val audioInputStream: TarsosDSPAudioInputStream
        get() {
            val audioFile = TestUtilities.sineOf4000Samples()
            var stream: AudioInputStream? = null
            try {
                stream = AudioSystem.getAudioInputStream(audioFile)
            } catch (e: UnsupportedAudioFileException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return JVMAudioInputStream(stream!!)
        }

    val audioInputStreamPiped: TarsosDSPAudioInputStream
        get() {
            val audioFile = TestUtilities.sineOf4000SamplesFile()
            return PipedAudioStream(audioFile.absolutePath).getMonoStream(44100, 0.0)
        }

    @Test
    fun testZeroPaddingFirstBuffer() {
        testZeroPaddingFirstBufferForStream(audioInputStream)
        testZeroPaddingFirstBufferForStream(audioInputStreamPiped)
    }

    fun testZeroPaddingFirstBufferForStream(audioStream: TarsosDSPAudioInputStream?) {
        val bufferSize = 4096
        val stepSize = 2048
        val totalSamples = 4000
        val adp = AudioDispatcher(audioStream, bufferSize, stepSize)
        adp.setZeroPadFirstBuffer(true)
        adp.setZeroPadLastBuffer(true)
        adp.addAudioProcessor(object : AudioProcessor {
            var bufferCounter = 0
            override fun process(audioEvent: AudioEvent): Boolean {
                //Check if the first samples are zero
                if (audioEvent.samplesProcessed == 0L) {
                    for (i in 0 until bufferSize - stepSize) {
                        Assertions.assertEquals(
                            0.0f,
                            audioEvent.floatBuffer[i],
                            0.00000001f,
                            "First buffer should be zero padded"
                        )
                    }
                    Assertions.assertEquals(
                        bufferSize,
                        audioEvent.bufferSize,
                        "Buffer size should always equal 4096"
                    )
                }
                //Check if the last samples are zero
                //first buffer contains [0-2048] second buffer[2048-4000]
                if (audioEvent.samplesProcessed == stepSize.toLong()) {
                    for (i in totalSamples until bufferSize) {
                        Assertions.assertEquals(
                            0.0f,
                            audioEvent.floatBuffer[i],
                            0.00000001f,
                            "Last buffer should be zero padded"
                        )
                    }
                    Assertions.assertEquals(
                        bufferSize,
                        audioEvent.bufferSize,
                        "Buffer size should always equal 4096"
                    )
                }
                bufferCounter++
                return true
            }

            override fun processingFinished() {
                Assertions.assertEquals(2, bufferCounter, "Should have processed 2 buffers.")
            }
        })
        adp.run()
    }

    /**
     * Tests the case when the first buffer is immediately the last.
     */
    @Test
    fun testFirstAndLastBuffer() {
        testFirstAndLastBufferForStream(audioInputStream)
        testFirstAndLastBufferForStream(audioInputStreamPiped)
    }

    fun testFirstAndLastBufferForStream(audioStream: TarsosDSPAudioInputStream?) {
        val bufferSize = 4096
        val stepSize = 0
        val totalSamples = 4000
        val adp = AudioDispatcher(audioStream, bufferSize, stepSize)
        adp.setZeroPadFirstBuffer(false)
        adp.setZeroPadLastBuffer(false)
        adp.addAudioProcessor(object : AudioProcessor {
            var bufferCounter = 0
            override fun process(audioEvent: AudioEvent): Boolean {
                //Check if the first samples are zero
                if (audioEvent.samplesProcessed == 0L) {
                    Assertions.assertEquals(
                        totalSamples,
                        audioEvent.bufferSize,
                        "Buffer size should always equal 4000"
                    )
                }
                bufferCounter++
                return true
            }

            override fun processingFinished() {
                Assertions.assertEquals(1, bufferCounter, "Should have processed 1 buffer.")
            }
        })
        adp.run()
    }
}