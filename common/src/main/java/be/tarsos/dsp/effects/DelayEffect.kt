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
package be.tarsos.dsp.effects

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

/**
 *
 *
 * Adds an echo effect to the signal.
 *
 *
 * @author Joren Six
 */
class DelayEffect(
    var echoLength: Double = 0.0,
    var decay: Double = 0.0,
    private val sampleRate: Double
) :
    AudioProcessor {
    //in seconds
    private var echoBuffer: FloatArray? = null
    private var position = 0

    private fun applyNewEchoLength() {
        if (echoLength != -1.0) {

            //create a new buffer with the information of the previous buffer
            val newEchoBuffer = FloatArray((sampleRate * echoLength).toInt())
            if (echoBuffer != null) {
                for (i in newEchoBuffer.indices) {
                    if (position >= echoBuffer!!.size) {
                        position = 0
                    }
                    newEchoBuffer[i] = echoBuffer!![position]
                    position++
                }
            }
            echoBuffer = newEchoBuffer
            echoLength = -1.0
        }
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        val overlap = audioEvent.overlap
        for (i in overlap until audioFloatBuffer.size) {
            if (position >= echoBuffer!!.size) {
                position = 0
            }

            //output is the input added with the decayed echo
            audioFloatBuffer[i] = audioFloatBuffer[i] + echoBuffer!![position] * decay.toFloat()
            //store the sample in the buffer;
            echoBuffer!![position] = audioFloatBuffer[i]
            position++
        }
        applyNewEchoLength()
        return true
    }

    override fun processingFinished() {}

    /**
     * @param echoLength in seconds
     * @param sampleRate the sample rate in Hz.
     * @param decay      The decay of the echo, a value between 0 and 1. 1 meaning no decay, 0 means immediate decay (not echo effect).
     */
    init {
        applyNewEchoLength()
    }
}