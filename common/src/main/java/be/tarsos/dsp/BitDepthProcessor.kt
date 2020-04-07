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
package be.tarsos.dsp

import kotlin.math.pow

/**
 * Can be used to show the effect of bit depth modification in real-time.
 * It simply transforms every sample to the requested bit depth.
 *
 * @author Joren Six
 */
class BitDepthProcessor : AudioProcessor {
    var bitDepth = 16

    override fun process(audioEvent: AudioEvent): Boolean {
        val buffer = audioEvent.floatBuffer
        //For e.g. a bith depth of 3, the factor is
        // 2^3 - 1 = 7
        val factor = 2.0.pow(bitDepth.toDouble()).toFloat() / 2.0f - 1
        for (i in buffer.indices) {
            //the float is scaled to the bith depth
            // e.g. if the bit depth is 3 and the value is 0.3:
            // ((int)(0.3 * 7)) / 7 = 0.28
            buffer[i] = (buffer[i] * factor).toInt() / factor
        }
        return true
    }

    override fun processingFinished() {}
}