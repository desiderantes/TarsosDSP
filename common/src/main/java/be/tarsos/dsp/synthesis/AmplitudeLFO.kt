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
package be.tarsos.dsp.synthesis

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

class AmplitudeLFO @JvmOverloads constructor(
    private val frequency: Double = 1.5,
    private val scaleParameter: Double = 0.75
) :
    AudioProcessor {
    private var phase = 0.0
    override fun process(audioEvent: AudioEvent): Boolean {
        val buffer = audioEvent.floatBuffer
        val sampleRate = audioEvent.sampleRate.toDouble()
        val twoPiF = 2 * Math.PI * frequency
        var time = 0.0
        for (i in buffer.indices) {
            time = i / sampleRate
            val gain = (scaleParameter * Math.sin(twoPiF * time + phase)).toFloat()
            buffer[i] = gain * buffer[i]
        }
        phase += twoPiF * buffer.size / sampleRate
        return true
    }

    override fun processingFinished() {}

}