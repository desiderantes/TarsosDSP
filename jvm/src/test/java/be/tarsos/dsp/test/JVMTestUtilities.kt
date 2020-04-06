package be.tarsos.dsp.test

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toTarsosDSPFormat
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

object JVMTestUtilities {
    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a flute played double forte at A6 (theoretically 440Hz) without vibrato
     */
    @JvmStatic
    fun audioBufferFlute(): FloatArray {
        val lengthInSamples = 4096
        val file = "flute.novib.ff.A4.wav"
        return audioBufferFile(file, lengthInSamples)
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a flute played double forte at B6 (theoretically 1975.53Hz) without vibrato
     */
    @JvmStatic
    fun audioBufferHighFlute(): FloatArray {
        val lengthInSamples = 4096
        val file = "flute.novib.ff.B6.wav"
        return audioBufferFile(file, lengthInSamples)
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a piano played double forte at A4 (theoretically 440Hz)
     */
    @JvmStatic
    fun audioBufferPiano(): FloatArray {
        val lengthInSamples = 4096
        val file = "piano.ff.A4.wav"
        return audioBufferFile(file, lengthInSamples)
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a piano played double forte at C3 (theoretically 130.81Hz)
     */
    @JvmStatic
    fun audioBufferLowPiano(): FloatArray {
        val lengthInSamples = 4096
        val file = "piano.ff.C3.wav"
        return audioBufferFile(file, lengthInSamples)
    }

    private fun audioBufferFile(file: String, lengthInSamples: Int): FloatArray {
        val buffer = FloatArray(lengthInSamples)
        try {
            val audioStream = AudioSystem.getAudioInputStream(TestUtilities.streamFromFilename(file))
            val format = audioStream.format
            val converter = TarsosDSPAudioFloatConverter.getConverter(toTarsosDSPFormat(format))
            val bytes = ByteArray(lengthInSamples * format.sampleSizeInBits)
            audioStream.read(bytes)
            converter.toFloatArray(bytes, buffer)
        } catch (e: IOException) {
            throw Error("Test audio file should be present.")
        } catch (e: UnsupportedAudioFileException) {
            throw Error("Test audio file format should be supported.")
        }
        return buffer
    }
}