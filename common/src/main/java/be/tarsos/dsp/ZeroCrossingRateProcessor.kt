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

/**
 * Calculates the zero crossing rate for a frame.
 *
 * @author Joren Six
 */
class ZeroCrossingRateProcessor : AudioProcessor {
    var zeroCrossingRate = 0f
        private set

    override fun process(audioEvent: AudioEvent): Boolean {
        val buffer = audioEvent.floatBuffer
        var numberOfZeroCrossings = 0
        for (i in 1 until buffer.size) {
            if (buffer[i] * buffer[i - 1] < 0) {
                numberOfZeroCrossings++
            }
        }
        zeroCrossingRate = numberOfZeroCrossings / (buffer.size - 1).toFloat()
        return true
    }

    override fun processingFinished() {}
}