package be.tarsos.dsp.io.android

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import java.io.InputStream

class WrapperAndroidAudioInputStream (
    private val underlyingStream: InputStream,
    private val format: TarsosDSPAudioFormat
) : AndroidAudioInputStream {
    override fun getFrameLength(): Long {
        return -1
    }

    override fun skip(bytesToSkip: Long): Long {
        return underlyingStream.skip(bytesToSkip)
    }

    override fun close() {
        underlyingStream.close()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return underlyingStream.read(b,off,len)
    }

    override fun getFormat(): TarsosDSPAudioFormat {
        return format
    }
}