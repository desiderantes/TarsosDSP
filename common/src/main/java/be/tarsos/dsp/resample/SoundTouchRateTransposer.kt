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
package be.tarsos.dsp.resample

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import kotlin.math.roundToInt

/**
 * Currently not working sample rate transposer, works only for integer factors.
 * Changes sample rate by using linear interpolation.
 *
 *
 * Together with the time stretcher this can be used for pitch shifting.
 *
 * @author Joren Six
 * @author Olli Parviainen
 */
class SoundTouchRateTransposer(private val rate: Double) : AudioProcessor {
    var slopeCount = 0
    var prevSample = 0.0
    private var dispatcher: AudioDispatcher? = null
    fun setDispatcher(newDispatcher: AudioDispatcher?) {
        dispatcher = newDispatcher
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val src = audioEvent.floatBuffer
        val dest = FloatArray((audioEvent.bufferSize / rate).roundToInt())
        var used: Int = 0
        var i: Int = 0

        // Process the last sample saved from the previous call first...
        while (slopeCount <= 1.0f) {
            dest[i] = ((1.0f - slopeCount) * prevSample + slopeCount * src[0]).toFloat()
            i++
            slopeCount += rate.toInt()
        }
        slopeCount -= 1
        end@ while (true) {
            while (slopeCount > 1.0f) {
                slopeCount -= 1
                used++
                if (used >= src.size - 1) break@end
            }
            if (i < dest.size) {
                dest[i] = ((1.0f - slopeCount) * src[used] + slopeCount * src[used + 1])
            }
            i++
            slopeCount += rate.toInt()
        }

        //Store the last sample for the next round
        prevSample = src[src.size - 1].toDouble()
        dispatcher!!.setStepSizeAndOverlap(dest.size, 0)
        audioEvent.floatBuffer = dest
        return true
    }

    override fun processingFinished() {}

}