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
package be.tarsos.dsp.io

import java.io.IOException
import java.io.InputStream

class UniversalAudioInputStream(private val underlyingStream: InputStream, override val format: TarsosDSPAudioFormat) :
    TarsosDSPAudioInputStream {

    @Throws(IOException::class)
    override fun skip(bytesToSkip: Long): Long {
        //the skip probably
        var bytesSkipped = 0
        for (i in 0 until bytesToSkip) {
            val theByte = underlyingStream.read()
            if (theByte != -1) {
                bytesSkipped++
            }
        }
        return bytesSkipped.toLong()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return underlyingStream.read(b, off, len)
    }

    @Throws(IOException::class)
    override fun close() {
        underlyingStream.close()
    }

    override val frameLength: Long = -1

}