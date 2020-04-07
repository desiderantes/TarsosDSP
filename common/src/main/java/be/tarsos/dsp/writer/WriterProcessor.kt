package be.tarsos.dsp.writer

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * This class writes the ongoing sound to an output specified by the programmer
 */
class WriterProcessor(var audioFormat: TarsosDSPAudioFormat, var output: RandomAccessFile) :
    AudioProcessor {
    private var audioLen = 0
    override fun process(audioEvent: AudioEvent): Boolean {
        try {
            audioLen += audioEvent.getByteBuffer().size
            //write audio to the output
            output.write(audioEvent.getByteBuffer())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    override fun processingFinished() {
        //write header and data to the result output
        val waveHeader = WaveHeader(
            WaveHeader.FORMAT_PCM,
            audioFormat.channels.toShort(),
            audioFormat.sampleRate.toInt(), 16.toShort(), audioLen
        ) //16 is for pcm, Read WaveHeader class for more details
        val header = ByteArrayOutputStream()
        try {
            waveHeader.write(header)
            output.seek(0)
            output.write(header.toByteArray())
            output.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val HEADER_LENGTH = 44 //byte
    }

    /**
     * @param audioFormat which this processor is attached to
     * @param output      randomaccessfile of the output file
     */
    init {
        try {
            output.write(ByteArray(HEADER_LENGTH))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}