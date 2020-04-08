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
/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -----------------------------------------------------------
 *
 *  TarsosDSP is developed by Joren Six at
 *  The School of Arts,
 *  University College Ghent,
 *  Hoogpoort 64, 9000 Ghent - Belgium
 *
 * -----------------------------------------------------------
 *
 *  Info: http://tarsos.0110.be/tag/TarsosDSP
 *  Github: https://github.com/JorenSix/TarsosDSP
 *  Releases: http://tarsos.0110.be/releases/TarsosDSP/
 *
 *  TarsosDSP includes modified source code by various authors,
 *  for credits and info, see README.
 *
 */
package be.tarsos.dsp.effects

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.TWO_PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 *
 *
 * Adds a flanger effect to a signal. The implementation is done with a delay
 * buffer and an LFO in the form of a sine wave. It is probably the most
 * straightforward flanger implementation possible.
 *
 *
 * @author Joren Six
 */
class FlangerEffect(
    maxFlangerLength: Double, wet: Double,
    /**
     * The sample rate is neede to calculate the length of the delay buffer.
     */
    private val sampleRate: Double,
    /**
     * The frequency for the LFO (sine).
     */
    var lfoFrequency: Double
) : AudioProcessor {
    /**
     * A simple delay buffer, it holds a number of samples determined by the
     * maxFlangerLength and the sample rate.
     */
    private var flangerBuffer: FloatArray = FloatArray((sampleRate * maxFlangerLength).toInt())

    /**
     * The position in the delay buffer to store the current sample.
     */
    private var writePosition = 0

    /**
     * Determines the factor of original signal that remains in the final mix.
     * Dry should always equal 1-wet).
     */
    private var dry: Float = 0.0f

    /**
     * Determines the factor of flanged signal that is mixed in the final mix.
     * Wet should always equal 1-dry.
     */
    private var wet: Float = 0.0f

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        val overlap = audioEvent.overlap

        // Divide f by two, to counter rectifier below, which effectively
        // doubles the frequency
        val twoPIf = TWO_PI * lfoFrequency / 2.0
        var time = audioEvent.timeStamp
        val timeStep = 1.0 / sampleRate
        for (i in overlap until audioFloatBuffer.size) {

            // Calculate the LFO delay value with a sine wave:
            //fix by hans bickel
            val lfoValue = (flangerBuffer.size - 1) * sin(twoPIf * time)
            // add a time step, each iteration
            time += timeStep

            // Make the delay a positive integer
            val delay = abs(lfoValue).roundToInt()

            // store the current sample in the delay buffer;
            if (writePosition >= flangerBuffer.size) {
                writePosition = 0
            }
            flangerBuffer[writePosition] = audioFloatBuffer[i]

            // find out the position to read the delayed sample:
            var readPosition = writePosition - delay
            if (readPosition < 0) {
                readPosition += flangerBuffer.size
            }

            //increment the write position
            writePosition++

            // Output is the input summed with the value at the delayed flanger
            // buffer
            audioFloatBuffer[i] = dry * audioFloatBuffer[i] + wet * flangerBuffer[readPosition]
        }
        return true
    }

    override fun processingFinished() {}

    /**
     * Set the new length of the delay LineWavelet.
     *
     * @param flangerLength The new length of the delay LineWavelet, in seconds.
     */
    fun setFlangerLength(flangerLength: Double) {
        flangerBuffer = FloatArray((sampleRate * flangerLength).toInt())
    }

    /**
     * Sets the wetness and dryness of the effect. Should be a value between
     * zero and one (inclusive), the dryness is determined by 1-wet.
     *
     * @param wet A value between zero and one (inclusive) that determines the
     * wet and dryness of the resulting mix.
     */
    fun setWet(wet: Double) {
        this.wet = wet.toFloat()
        dry = (1 - wet).toFloat()
    }

    /**
     * Sets the wetness and wetness of the effect. Should be a value between
     * zero and one (inclusive), the wetness is determined by 1-dry.
     *
     * @param dry A value between zero and one (inclusive) that determines the
     * wet and dryness of the resulting mix.
     */
    fun setDry(dry: Double) {
        this.dry = dry.toFloat()
        wet = (1 - dry).toFloat()
    }

    /**
     * @param maxFlangerLength in seconds
     * @param wet              The 'wetness' of the flanging effect. A value between 0 and 1.
     * Zero meaning no flanging effect in the resulting signal, one
     * means total flanging effect and no original signal left. The
     * dryness of the signal is determined by dry = "1-wet".
     * @param sampleRate       the sample rate in Hz.
     * @param lfoFrequency     in Hertz
     */
    init {
        flangerBuffer
        setWet(wet)
    }
}