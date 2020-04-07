package be.tarsos.dsp.io.android

import android.media.AudioRecord
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.io.IOException

class AndroidAudioInputStream(
    private val underlyingStream: AudioRecord,
    override val format: TarsosDSPAudioFormat
) : TarsosDSPAudioInputStream, AutoCloseable {
    @Throws(IOException::class)
    override fun skip(bytesToSkip: Long): Long {
        throw IOException("Can not skip in audio stream")
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return underlyingStream.read(b, off, len)
    }

    @Throws(IOException::class)
    override fun close() {
        underlyingStream.stop()
        underlyingStream.release()
    }

    override val frameLength: Long
        get() = -1

}