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
package be.tarsos.dsp.io.jvm

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.jvm.JVMAudioInputStream.Companion.toAudioFormat
import be.tarsos.dsp.io.jvm.WaveformWriter
import java.io.*
import java.util.*
import java.util.logging.Logger
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 *
 *
 * Writes a WAV-file to disk. It stores the bytes to a raw file and when the
 * processingFinished method is called it prepends the raw file with a header to
 * make it a legal WAV-file.
 *
 *
 *
 *
 * Writing a RAW file first and then a header is needed because the header
 * contains fields for the size of the file, which is unknown beforehand. See
 * Subchunk2Size and ChunkSize on this [wav file
 * reference](https://ccrma.stanford.edu/courses/422/projects/WaveFormat/).
 *
 *
 * @author Joren Six
 */
class WaveformWriter(
    private val format: AudioFormat,
    private val fileName: String
) : AudioProcessor {
    private val rawOutputFile: File = File(
        System.getProperty("java.io.tmpdir"),
        Random().nextInt().toString() + "out.raw"
    )
    private var rawOutputStream: FileOutputStream? = null

    /**
     * The overlap and step size defined not in samples but in bytes. So it
     * depends on the bit depth. Since the integer data type is used only
     * 8,16,24,... bits or 1,2,3,... bytes are supported.
     */
    private var byteOverlap = 0
    private var byteStepSize = 0

    constructor(
        format: TarsosDSPAudioFormat,
        fileName: String
    ) : this(toAudioFormat(format), fileName) {
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        byteOverlap = audioEvent.overlap * format.frameSize
        byteStepSize = audioEvent.bufferSize * format.frameSize - byteOverlap
        try {
            rawOutputStream!!.write(audioEvent.byteBuffer, byteOverlap, byteStepSize)
        } catch (e: IOException) {
            LOG.severe("Failure while writing temporary file:${rawOutputFile.absolutePath} : ${e.message}")
        }
        return true
    }

    override fun processingFinished() {
        val out = File(fileName)
        try {
            //stream the raw file
            val inputStream = FileInputStream(rawOutputFile)
            val lengthInSamples = rawOutputFile.length() / format.frameSize
            val audioInputStream: AudioInputStream
            //create an audio stream form the raw file in the specified format
            audioInputStream = AudioInputStream(inputStream, format, lengthInSamples)
            //stream this to the out file
            val fos = FileOutputStream(out)
            //stream all the bytes to the output stream
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, fos)
            //cleanup
            fos.close()
            audioInputStream.close()
            inputStream.close()
            rawOutputStream!!.close()
            rawOutputFile.delete()
        } catch (e: IOException) {
            LOG.severe("Error writing the WAV file ${out.absolutePath}: ${e.message}")
        }
    }

    companion object {
        /**
         * Log messages.
         */
        private val LOG = Logger.getLogger(
            WaveformWriter::class.java.name
        )
    }

    /**
     * Initialize the writer.
     *
     * @param format   The format of the received bytes.
     * @param fileName The name of the wav file to store.
     */
    init {
        //a temporary raw file with a random prefix
        try {
            rawOutputStream = FileOutputStream(rawOutputFile)
        } catch (e: FileNotFoundException) {
            //It should always be possible to write to a temporary file.
            LOG.severe("Could not write to the temporary RAW file ${rawOutputFile.absolutePath}: ${e.message}")
        }
    }
}