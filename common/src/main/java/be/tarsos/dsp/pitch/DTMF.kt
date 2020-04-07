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
package be.tarsos.dsp.pitch

import be.tarsos.dsp.util.PI
import kotlin.math.sin

/**
 * Utility class to generate Dual-tone multi-frequency (DTMF) signaling tones.
 * This class also contains a list of valid DTMF frequencies and characters.
 *
 *
 * See the [WikiPedia article on DTMF](http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling).
 *
 * @author Joren Six
 */
object DTMF {
    /**
     * The list of valid DTMF frequencies. See the [WikiPedia article on DTMF](http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling).
     */
    @JvmField
    val DTMF_FREQUENCIES = doubleArrayOf(697.0, 770.0, 852.0, 941.0, 1209.0, 1336.0, 1477.0, 1633.0)

    /**
     * The list of valid DTMF characters. See the [WikiPedia article on DTMF](http://en.wikipedia.org/wiki/Dual-tone_multi-frequency_signaling) for the relation between the characters
     * and frequencies.
     */
    @JvmField
    val DTMF_CHARACTERS = arrayOf(
        charArrayOf('1', '2', '3', 'A'),
        charArrayOf('4', '5', '6', 'B'),
        charArrayOf('7', '8', '9', 'C'),
        charArrayOf('*', '0', '#', 'D')
    )

    /**
     * Generate a DTMF - tone for a valid DTMF character.
     *
     * @param character a valid DTMF character (present in DTMF_CHARACTERS}
     * @return a float buffer of predefined length (7168 samples) with the correct DTMF tone representing the character.
     */
    @JvmStatic
    fun generateDTMFTone(character: Char): FloatArray {
        var firstFrequency = -1.0
        var secondFrequency = -1.0
        for (row in DTMF_CHARACTERS.indices) {
            for (col in DTMF_CHARACTERS[row].indices) {
                if (DTMF_CHARACTERS[row][col] == character) {
                    firstFrequency = DTMF_FREQUENCIES[row]
                    secondFrequency = DTMF_FREQUENCIES[col + 4]
                }
            }
        }
        return audioBufferDTMF(firstFrequency, secondFrequency, 512 * 2 * 10)
    }

    /**
     * Checks if the given character is present in DTMF_CHARACTERS.
     *
     * @param character the character to check.
     * @return True if the given character is present in
     * DTMF_CHARACTERS, false otherwise.
     */
    @JvmStatic
    fun isDTMFCharacter(character: Char): Boolean {
        var firstFrequency = -1.0
        var secondFrequency = -1.0
        for (row in DTMF_CHARACTERS.indices) {
            for (col in DTMF_CHARACTERS[row].indices) {
                if (DTMF_CHARACTERS[row][col] == character) {
                    firstFrequency = DTMF_FREQUENCIES[row]
                    secondFrequency = DTMF_FREQUENCIES[col + 4]
                }
            }
        }
        return firstFrequency != -1.0 && secondFrequency != -1.0
    }

    /**
     * Creates an audio buffer in a float array of the defined size. The sample
     * rate is 44100Hz by default. It mixes the two given frequencies with an
     * amplitude of 0.5.
     *
     * @param f0   The first fundamental frequency.
     * @param f1   The second fundamental frequency.
     * @param size The size of the float array (sample rate is 44.1kHz).
     * @return An array of the defined size.
     */
    fun audioBufferDTMF(
        f0: Double, f1: Double,
        size: Int
    ): FloatArray {
        val sampleRate = 44100.0
        val amplitudeF0 = 0.4
        val amplitudeF1 = 0.4
        val twoPiF0 = 2 * PI * f0
        val twoPiF1 = 2 * PI * f1
        val buffer = FloatArray(size)
        for (sample in buffer.indices) {
            val time = sample / sampleRate
            val f0Component = amplitudeF0 * sin(twoPiF0 * time)
            val f1Component = amplitudeF1 * sin(twoPiF1 * time)
            buffer[sample] = (f0Component + f1Component).toFloat()
        }
        return buffer
    }
}