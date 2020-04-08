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
package be.tarsos.dsp.wavelet.lift

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import java.util.*
import kotlin.math.abs

class Daubechies4WaveletCoder @JvmOverloads constructor(var compression: Int = 16) : AudioProcessor {
    private val transform: Daubechies4Wavelet = Daubechies4Wavelet()
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioBuffer = audioEvent.floatBuffer
        val sortBuffer = FloatArray(audioBuffer.size)
        transform.forwardTrans(audioBuffer)
        for (i in sortBuffer.indices) {
            sortBuffer[i] = abs(audioBuffer[i])
        }
        Arrays.sort(sortBuffer)
        val threshold = sortBuffer[compression].toDouble()
        for (i in audioBuffer.indices) {
            if (abs(audioBuffer[i]) <= threshold) {
                audioBuffer[i] = 0F
            }
        }
        return true
    }

    override fun processingFinished() {}

}