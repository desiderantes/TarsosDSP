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
 * With the gain processor it is possible to adapt the volume of the sound. With
 * a gain of 1, nothing happens. A gain greater than one is a volume increase a
 * gain between zero and one, exclusive, is a decrease. If you need to flip the
 * sign of the audio samples, you can by providing a gain of -1.0. but I have no
 * idea what you could gain by doing that (pathetic pun, I know).
 *
 * @author Joren Six
 */
class GainProcessor(var gain: Double = 0.0) : AudioProcessor {


    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        for (i in audioEvent.overlap until audioFloatBuffer.size) {
            var newValue = (audioFloatBuffer[i] * gain).toFloat()
            if (newValue > 1.0f) {
                newValue = 1.0f
            } else if (newValue < -1.0f) {
                newValue = -1.0f
            }
            audioFloatBuffer[i] = newValue
        }
        return true
    }

    override fun processingFinished() {
        // NOOP
    }
}