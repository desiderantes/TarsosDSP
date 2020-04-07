package be.tarsos.dsp.writer

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * this source code is copied from : https://android.googlesource.com/platform/frameworks/base.git/+/android-4.3_r2/core/java/android/speech/srec/WaveHeader.java
 */
/**
 * This class represents the header of a WAVE format audio file, which usually
 * have a .wav suffix.  The following integer valued fields are contained:
 *
 *  *  format - usually PCM, ALAW or ULAW.
 *  *  numChannels - 1 for mono, 2 for stereo.
 *  *  sampleRate - usually 8000, 11025, 16000, 22050, or 44100 hz.
 *  *  bitsPerSample - usually 16 for PCM, 8 for ALAW, or 8 for ULAW.
 *  *  numBytes - size of audio data after this header, in bytes.
 *
 *
 *
 * Not yet ready to be supported, so
 *
 * @hide
 */
class WaveHeader @JvmOverloads constructor(
    /**
     * Get the format field.
     *
     * @return format field,
     * one of [.FORMAT_PCM], [.FORMAT_ULAW], or [.FORMAT_ALAW].
     */
    var format: Short = 0,
    /**
     * Get the number of channels.
     *
     * @return number of channels, 1 for mono, 2 for stereo.
     */
    var numChannels: Short = 0,
    /**
     * Get the sample rate.
     *
     * @return sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     */
    var sampleRate: Int = 0,
    /**
     * Get the number of bits per sample.
     *
     * @return number of bits per sample,
     * usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     */
    var bitsPerSample: Short = 0,
    /**
     * Get the size of audio data after this header, in bytes.
     *
     * @return size of audio data after this header, in bytes.
     */
    var numBytes: Int = 0
) {

    /**
     * Read and initialize a WaveHeader.
     *
     * @param in [java.io.InputStream] to read from.
     * @return number of bytes consumed.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun read(`in`: InputStream): Int {
        /* RIFF header */
        readId(`in`, "RIFF")
        readId(`in`, "WAVE")

        /* fmt chunk */readId(`in`, "fmt ")
        if (16 != readInt(`in`)) throw IOException("fmt chunk length not 16")
        format = readShort(`in`)
        numChannels = readShort(`in`)
        sampleRate = readInt(`in`)
        val byteRate = readInt(`in`)
        val blockAlign = readShort(`in`)
        bitsPerSample = readShort(`in`)
        if (byteRate != numChannels * sampleRate * bitsPerSample / 8) {
            throw IOException("fmt.ByteRate field inconsistent")
        }
        if (blockAlign.toInt() != numChannels * bitsPerSample / 8) {
            throw IOException("fmt.BlockAlign field inconsistent")
        }

        /* data chunk */readId(`in`, "data")
        numBytes = readInt(`in`)
        return HEADER_LENGTH
    }

    /**
     * Write a WAVE file header.
     *
     * @param out [java.io.OutputStream] to receive the header.
     * @return number of bytes written.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(out: OutputStream): Int {
        /* RIFF header */
        writeId(out, "RIFF")
        writeInt(out, 36 + numBytes)
        writeId(out, "WAVE")

        /* fmt chunk */writeId(out, "fmt ")
        writeInt(out, 16)
        writeShort(out, format)
        writeShort(out, numChannels)
        writeInt(out, sampleRate)
        writeInt(out, numChannels * sampleRate * bitsPerSample / 8)
        writeShort(out, (numChannels * bitsPerSample / 8).toShort())
        writeShort(out, bitsPerSample)

        /* data chunk */writeId(out, "data")
        writeInt(out, numBytes)
        return HEADER_LENGTH
    }

    override fun toString(): String {
        return String.format(
            "WaveHeader format=%d numChannels=%d sampleRate=%d bitsPerSample=%d numBytes=%d",
            format, numChannels, sampleRate, bitsPerSample, numBytes
        )
    }

    companion object {
        // follows WAVE format in http://ccrma.stanford.edu/courses/422/projects/WaveFormat
        /**
         * Indicates PCM format.
         */
        const val FORMAT_PCM: Short = 1

        /**
         * Indicates ALAW format.
         */
        const val FORMAT_ALAW: Short = 6

        /**
         * Indicates ULAW format.
         */
        const val FORMAT_ULAW: Short = 7
        private const val HEADER_LENGTH = 44

        @Throws(IOException::class)
        private fun readId(`in`: InputStream, id: String) {
            for (element in id) {
                if (element.toInt() != `in`.read()) throw IOException("$id tag not present")
            }
        }

        @Throws(IOException::class)
        private fun readInt(`in`: InputStream): Int {
            return `in`.read() or (`in`.read() shl 8) or (`in`.read() shl 16) or (`in`.read() shl 24)
        }

        @Throws(IOException::class)
        private fun readShort(`in`: InputStream): Short {
            return (`in`.read() or (`in`.read() shl 8)).toShort()
        }

        @Throws(IOException::class)
        private fun writeId(out: OutputStream, id: String) {
            for (element in id) out.write(element.toInt())
        }

        @Throws(IOException::class)
        private fun writeInt(out: OutputStream, `val`: Int) {
            out.write(`val` shr 0)
            out.write(`val` shr 8)
            out.write(`val` shr 16)
            out.write(`val` shr 24)
        }

        @Throws(IOException::class)
        private fun writeShort(out: OutputStream, `val`: Short) {
            out.write(`val`.toInt() shr 0)
            out.write(`val`.toInt() shr 8)
        }
    }
}