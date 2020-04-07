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
package be.tarsos.dsp.wavelet

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.shl
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.experimental.and
import kotlin.experimental.or

class HaarWaveletFileReader(fileName: String, private val compression: Int) : AudioProcessor {
    private var rawInputStream: FileInputStream
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioBuffer = FloatArray(32)
        val byteBuffer = ByteArray((32 - compression) * 2)
        var placesWithZero = 0
        try {
            rawInputStream.read(byteBuffer)
            placesWithZero += rawInputStream.read()
            placesWithZero += rawInputStream.read() shl 8
            placesWithZero += rawInputStream.read() shl 16
            placesWithZero += rawInputStream.read() shl 24
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var byteBufferIndex = 0
        for (i in audioBuffer.indices) {
            if (placesWithZero and (1 shl i) != 1 shl i) {
                var x: Byte = byteBuffer[byteBufferIndex] and 0xFF.toByte()
                byteBufferIndex++
                val y: Byte = byteBuffer[byteBufferIndex] shl 8.toByte()
                byteBufferIndex++
                x = x or y
                val value = x / 32767.0f
                audioBuffer[i] = value
            }
        }
        audioEvent.floatBuffer = audioBuffer
        var more = true
        try {
            more = rawInputStream.available() > 0
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return more
    }

    override fun processingFinished() {
        try {
            rawInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    init {
        rawInputStream = try {
            FileInputStream(fileName)
        } catch (e: FileNotFoundException) {
            throw e
        }
    }
}