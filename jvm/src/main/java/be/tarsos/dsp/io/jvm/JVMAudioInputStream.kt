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

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream

/**
 * Encapsulates an [AudioInputStream] to make it work with the core TarsosDSP library.
 *
 * @author Joren Six
 */
class JVMAudioInputStream(private val underlyingStream: AudioInputStream) :
    TarsosDSPAudioInputStream,
    AutoCloseable {
    override val format: TarsosDSPAudioFormat = toTarsosDSPFormat(underlyingStream.format)

    @Throws(IOException::class)
    override fun skip(bytesToSkip: Long): Long {
        return underlyingStream.skip(bytesToSkip)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return underlyingStream.read(b, off, len)
    }

    @Throws(IOException::class)
    override fun close() {
        underlyingStream.close()
    }

    override val frameLength: Long = underlyingStream.frameLength

    companion object {
        /**
         * Converts a [AudioFormat] to a [TarsosDSPAudioFormat].
         *
         * @param format The [AudioFormat]
         * @return A [TarsosDSPAudioFormat]
         */
        @JvmStatic
        fun toTarsosDSPFormat(format: AudioFormat): TarsosDSPAudioFormat {
            val isSigned =
                format.encoding === AudioFormat.Encoding.PCM_SIGNED
            return TarsosDSPAudioFormat(
                format.sampleRate,
                format.sampleSizeInBits,
                format.channels,
                isSigned,
                format.isBigEndian
            )
        }

        /**
         * Converts a [TarsosDSPAudioFormat] to a [AudioFormat].
         *
         * @param format The [TarsosDSPAudioFormat]
         * @return A [AudioFormat]
         */
        @JvmStatic
        fun toAudioFormat(format: TarsosDSPAudioFormat): AudioFormat {
            val isSigned =
                format.encoding === TarsosDSPAudioFormat.Encoding.PCM_SIGNED
            return AudioFormat(
                format.sampleRate,
                format.sampleSizeInBits,
                format.channels,
                isSigned,
                format.isBigEndian
            )
        }
    }
}