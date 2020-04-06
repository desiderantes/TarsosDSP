package be.tarsos.dsp.test

import be.tarsos.dsp.PitchShifter
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFloatArray
import be.tarsos.dsp.io.jvm.AudioPlayer
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

class PitchShifterTest {
    @Test
    @Throws(
        UnsupportedAudioFileException::class,
        LineUnavailableException::class,
        IOException::class
    )
    fun testPitchShiftSine() {
        val audioBuffer = TestUtilities.audioBufferSine()
        val factor = 1.35
        val sampleRate = 44100
        val d = fromFloatArray(audioBuffer, sampleRate, 1024, 1024 - 32)
        d.setZeroPadLastBuffer(true)
        val w = PitchShifter(factor, sampleRate.toDouble(), 1024, 1024 - 32)
        val f = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        d.addAudioProcessor(w)
        d.addAudioProcessor(AudioPlayer(f))
        d.run()
    }
}