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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class HaarWaveletFileWriter(fileName: String, private val compression: Int) : AudioProcessor {
    private var rawOutputStream: FileOutputStream
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioBuffer = audioEvent.floatBuffer
        var placesWithZero = 0
        var zeroCounter = 0
        for (i in audioBuffer.indices) {
            if (audioBuffer[i] == 0F && zeroCounter < compression) {
                zeroCounter++
                placesWithZero = placesWithZero or (1 shl i)
            }
        }
        assert(zeroCounter == compression)


        //16 bits little endian
        val byteBuffer = ByteArray((audioBuffer.size - compression) * 2)
        zeroCounter = 0
        var bufferIndex = 0
        var i = 0
        while (i < byteBuffer.size) {
            val value = audioBuffer[bufferIndex++]
            if (value == 0f && zeroCounter < compression) {
                zeroCounter++
                i--
            } else {
                val x = (value * 32767.0).toInt()
                byteBuffer[i] = x.toByte()
                i++
                byteBuffer[i] = (x ushr 8).toByte()
            }
            i++
        }
        try {
            rawOutputStream.write(byteBuffer)
            rawOutputStream.write(placesWithZero)
            rawOutputStream.write((placesWithZero ushr 8))
            rawOutputStream.write((placesWithZero ushr 16))
            rawOutputStream.write((placesWithZero ushr 24))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    override fun processingFinished() {
        try {
            rawOutputStream.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    init {
        rawOutputStream = try {
            FileOutputStream(fileName)
        } catch (e: FileNotFoundException) {
            throw e
        }
    }
}