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

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

/**
 * Sample rate transposer. Changes sample rate by using  interpolation
 *
 *
 * Together with the time stretcher this can be used for pitch shifting.
 *
 * @author Joren Six
 */
class RateTransposer(private var factor: Double) : AudioProcessor {
    private val r: Resampler = Resampler(false, 0.1, 4.0)
    fun setFactor(tempo: Double) {
        factor = tempo
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val src = audioEvent.floatBuffer
        //Creation of float array in loop could be prevented if src.length is known beforehand...
        //Possible optimization is to instantiate it outside the loop and get a pointer to the
        //array here, in the process method method.
        val out = FloatArray((src.size * factor).toInt())
        r.process(factor, src, 0, src.size, false, out, 0, out.size)
        //The size of the output buffer changes (according to factor).
        audioEvent.floatBuffer = out
        //Update overlap offset to match new buffer size
        audioEvent.overlap = (audioEvent.overlap * factor).toInt()
        return true
    }

    override fun processingFinished() {}

}