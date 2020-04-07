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

import kotlin.math.abs
import kotlin.math.exp

/**
 * An envelope follower follows the envelope of a signal. Sometimes the name
 * envelope detector is used. From wikipedia:
 * <blockquote> An envelope detector
 * is an electronic circuit that takes a high-frequency signal as input and
 * provides an output which is the envelope of the original signal. The
 * capacitor in the circuit stores up charge on the rising edge, and releases it
 * slowly through the resistor when the signal falls. The diode in series
 * rectifies the incoming signal, allowing current flow only when the positive
 * input terminal is at a higher potential than the negative input terminal.
</blockquote> *
 *
 *
 * The resulting envelope is stored in the buffer in the processed AudioEvent. The class can be used thusly:
 *
 * <pre>
 * EnvelopeFollower follower = new EnvelopeFollower(44100);
 *
 * AudioDispatcher dispatcher = AudioDispatcher.fromFloatArray(sine, 44100, 1024, 0);
 *
 *
 * dispatcher.addAudioProcessor(follower);
 * dispatcher.addAudioProcessor(new AudioProcessor() {
 *
 * public boolean process(AudioEvent audioEvent) {
 * //envelope
 * float buffer[] = audioEvent.getFloatBuffer();
 * for(int i = 0 ; i < buffer.length ; i++){
 * System.out.println(buffer[i]);
 * }
 * return true;
 * }
 *
 * public void processingFinished() {
 * }
 * });
 * dispatcher.run();
</pre> *
 *
 * @author Joren Six
 */
class EnvelopeFollower @JvmOverloads constructor(
    sampleRate: Double,
    attackTime: Double = DEFAULT_ATTACK_TIME,
    releaseTime: Double = DEFAULT_RELEASE_TIME
) : AudioProcessor {
    var gainAttack: Float = exp(-1.0 / (sampleRate * attackTime)).toFloat()
    var gainRelease: Float = exp(-1.0 / (sampleRate * releaseTime)).toFloat()
    var envelopeOut = 0.0f
    override fun process(audioEvent: AudioEvent): Boolean {
        val buffer = audioEvent.floatBuffer
        calculateEnvelope(buffer)
        return true
    }

    fun calculateEnvelope(buffer: FloatArray) {
        for (i in buffer.indices) {
            val envelopeIn = abs(buffer[i])
            envelopeOut = if (envelopeOut < envelopeIn) {
                envelopeIn + gainAttack * (envelopeOut - envelopeIn)
            } else {
                envelopeIn + gainRelease * (envelopeOut - envelopeIn)
            }
            buffer[i] = envelopeOut
        }
    }

    override fun processingFinished() {}

    companion object {
        /**
         * Defines how fast the envelope raises, defined in seconds.
         */
        private const val DEFAULT_ATTACK_TIME = 0.0002 //in seconds

        /**
         * Defines how fast the envelope goes down, defined in seconds.
         */
        private const val DEFAULT_RELEASE_TIME = 0.0004 //in seconds
    }
}