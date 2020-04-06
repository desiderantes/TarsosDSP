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

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.PipedAudioStream
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.sound.sampled.*

/**
 * The Factory creates [AudioDispatcher] objects from various sources: the
 * configured default microphone, PCM wav files or PCM samples piped from a
 * sub-process. It depends on the javax.sound.* packages and does not work on Android.
 *
 * @author Joren Six
 * @see AudioDispatcher
 */
object AudioDispatcherFactory {
    /**
     * Create a new AudioDispatcher connected to the default microphone. The default is defined by the
     * Java runtime by calling <pre>AudioSystem.getTargetDataLine(format)</pre>.
     * The microphone must support the format: 44100Hz sample rate, 16bits mono, signed big endian.
     *
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     * in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     * AudioBufferSize is common.
     * @return An audio dispatcher connected to the default microphone.
     * @throws LineUnavailableException
     */
    @Throws(LineUnavailableException::class)
    @JvmStatic
    fun fromDefaultMicrophone(audioBufferSize: Int, bufferOverlap: Int): AudioDispatcher {
        return fromDefaultMicrophone(
            44100,
            audioBufferSize,
            bufferOverlap
        )
    }

    /**
     * Create a new AudioDispatcher connected to the default microphone. The default is defined by the
     * Java runtime by calling <pre>AudioSystem.getTargetDataLine(format)</pre>.
     * The microphone must support the format of the requested sample rate, 16bits mono, signed big endian.
     *
     * @param sampleRate      The **requested** sample rate must be supported by the capture device. Nonstandard sample
     * rates can be problematic!
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     * in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     * AudioBufferSize is common.
     * @return An audio dispatcher connected to the default microphone.
     * @throws LineUnavailableException
     */
    @Throws(LineUnavailableException::class)
    @JvmStatic
    fun fromDefaultMicrophone(
        sampleRate: Int,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        val format =
            AudioFormat(sampleRate.toFloat(), 16, 1, true, true)
        val line = AudioSystem.getTargetDataLine(format)
        line.open(format, audioBufferSize)
        line.start()
        val stream = AudioInputStream(line)
        val audioStream: TarsosDSPAudioInputStream = JVMAudioInputStream(stream)
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    }

    /**
     * Create a stream from an array of bytes and use that to create a new
     * AudioDispatcher.
     *
     * @param byteArray       An array of bytes, containing audio information.
     * @param audioFormat     The format of the audio represented using the bytes.
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     * in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     * AudioBufferSize is common.
     * @return A new AudioDispatcher.
     * @throws UnsupportedAudioFileException If the audio format is not supported.
     */
    @Throws(UnsupportedAudioFileException::class)
    @JvmStatic
    fun fromByteArray(
        byteArray: ByteArray, audioFormat: AudioFormat,
        audioBufferSize: Int, bufferOverlap: Int
    ): AudioDispatcher {
        val bais = ByteArrayInputStream(byteArray)
        val length = byteArray.size / audioFormat.frameSize.toLong()
        val stream = AudioInputStream(bais, audioFormat, length)
        val audioStream: TarsosDSPAudioInputStream = JVMAudioInputStream(stream)
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    }

    /**
     * Create a stream from an URL and use that to create a new AudioDispatcher
     *
     * @param audioURL        The URL describing the stream..
     * @param audioBufferSize The number of samples used in the buffer.
     * @param bufferOverlap
     * @return A new audio processor.
     * @throws UnsupportedAudioFileException If the audio file is not supported.
     * @throws IOException                   When an error occurs reading the file.
     */
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    @JvmStatic
    fun fromURL(
        audioURL: URL?,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        val stream = AudioSystem.getAudioInputStream(audioURL)
        val audioStream: TarsosDSPAudioInputStream = JVMAudioInputStream(stream)
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    }

    /**
     * Create a stream from a piped sub process and use that to create a new
     * [AudioDispatcher] The sub-process writes a WAV-header and
     * PCM-samples to standard out. The header is ignored and the PCM samples
     * are are captured and interpreted. Examples of executables that can
     * convert audio in any format and write to stdout are ffmpeg and avconv.
     *
     * @param source           The file or stream to capture.
     * @param targetSampleRate The target sample rate.
     * @param audioBufferSize  The number of samples used in the buffer.
     * @param bufferOverlap
     * @param startTimeOffset  Number of seconds to skip
     * @return A new audioprocessor.
     */
    @JvmOverloads
    @JvmStatic
    fun fromPipe(
        source: String,
        targetSampleRate: Int,
        audioBufferSize: Int,
        bufferOverlap: Int,
        startTimeOffset: Double = 0.0
    ): AudioDispatcher {
        return if (File(source).exists() && File(source).isFile && File(source)
                .canRead()
        ) {
            val f = PipedAudioStream(source)
            val audioStream =
                f.getMonoStream(targetSampleRate, startTimeOffset)
            AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
        } else {
            throw IllegalArgumentException("The file $source is not a readable file. Does it exist?")
        }
    }

    /**
     * Create a stream from a piped sub process and use that to create a new
     * [AudioDispatcher] The sub-process writes a WAV-header and
     * PCM-samples to standard out. The header is ignored and the PCM samples
     * are are captured and interpreted. Examples of executables that can
     * convert audio in any format and write to stdout are ffmpeg and avconv.
     *
     * @param source           The file or stream to capture.
     * @param targetSampleRate The target sample rate.
     * @param audioBufferSize  The number of samples used in the buffer.
     * @param bufferOverlap
     * @param startTimeOffset  Number of seconds to skip
     * @param numberOfSeconds  Number of seconds to pipe
     * @return A new audioprocessor.
     */
    @JvmStatic
    fun fromPipe(
        source: String,
        targetSampleRate: Int,
        audioBufferSize: Int,
        bufferOverlap: Int,
        startTimeOffset: Double,
        numberOfSeconds: Double
    ): AudioDispatcher {
        return if (File(source).exists() && File(source).isFile && File(source)
                .canRead()
        ) {
            val f = PipedAudioStream(source)
            val audioStream =
                f.getMonoStream(targetSampleRate, startTimeOffset, numberOfSeconds)
            AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
        } else {
            throw IllegalArgumentException("The file $source is not a readable file. Does it exist?")
        }
    }

    /**
     * Create a stream from a file and use that to create a new AudioDispatcher
     *
     * @param audioFile       The file.
     * @param audioBufferSize The number of samples used in the buffer.
     * @param bufferOverlap
     * @return A new audioprocessor.
     * @throws UnsupportedAudioFileException If the audio file is not supported.
     * @throws IOException                   When an error occurs reading the file.
     */
    @JvmStatic
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun fromFile(
        audioFile: File?,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        val stream = AudioSystem.getAudioInputStream(audioFile)
        val audioStream: TarsosDSPAudioInputStream = JVMAudioInputStream(stream)
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    }

    /**
     * Create a stream from a input Stream and use that to create a new AudioDispatcher
     *
     * @param inputStream       The input Stream.
     * @param audioBufferSize The number of samples used in the buffer.
     * @param bufferOverlap
     * @return A new audioprocessor.
     * @throws UnsupportedAudioFileException If the audio file is not supported.
     * @throws IOException                   When an error occurs reading the file.
     */
    @JvmStatic
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun fromInputStream(
        inputStream: InputStream?,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        val stream = AudioSystem.getAudioInputStream(inputStream)
        val audioStream: TarsosDSPAudioInputStream = JVMAudioInputStream(stream)
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    }

    /**
     * Create a stream from an array of floats and use that to create a new
     * AudioDispatcher.
     *
     * @param floatArray      An array of floats, containing audio information.
     * @param sampleRate      The sample rate of the audio information contained in the buffer.
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     * in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     * AudioBufferSize is common.
     * @return A new AudioDispatcher.
     * @throws UnsupportedAudioFileException If the audio format is not supported.
     */
    @JvmStatic
    @Throws(UnsupportedAudioFileException::class)
    fun fromFloatArray(
        floatArray: FloatArray,
        sampleRate: Int,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        val audioFormat =
            AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val converter = TarsosDSPAudioFloatConverter.getConverter(
            JVMAudioInputStream.toTarsosDSPFormat(audioFormat)
        )
        val byteArray =
            ByteArray(floatArray.size * audioFormat.frameSize)
        converter.toByteArray(floatArray, byteArray)
        return fromByteArray(
            byteArray,
            audioFormat,
            audioBufferSize,
            bufferOverlap
        )
    }
}