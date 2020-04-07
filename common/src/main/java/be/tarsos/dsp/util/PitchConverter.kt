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
package be.tarsos.dsp.util

import kotlin.math.*

/**
 * Converts pitch from one unit to another (and back (and back (and back ...))).
 *
 * @author Joren Six
 */
object PitchConverter {
    /**
     * C-1 = 16.35 / 2 Hz.
     */
    private const val REF_FREQ = 8.17579892

    /**
     * Cache LOG 2 calculation.
     */

    /**
     * A MIDI key is an integer between 0 and 127, inclusive. Within a certain
     * range every pitch is mapped to a MIDI key. If a value outside the range
     * is given an IllegalArugmentException is thrown.
     *
     * @param hertzValue The pitch in Hertz.
     * @return An integer representing the closest midi key.
     * @throws IllegalArgumentException if the hertzValue does not fall within the range of valid
     * MIDI key frequencies.
     */
    fun hertzToMidiKey(hertzValue: Double): Int {
        val midiKey = hertzToMidiCent(hertzValue).roundToLong().toInt()
        if (midiKey < 0 || midiKey > 127) {
            // TODO
            // LOG.warning("MIDI is only defined between [" + midiKeyToHertz(0)
            // + ","
            // + midiKeyToHertz(127) + "] " + hertzValue +
            // "does not map to a MIDI key.");
        }
        return midiKey
    }

    /**
     * Calculates the frequency (Hz) for a MIDI key.
     *
     * @param midiKey The MIDI key. A MIDI key is an integer between 0 and 127,
     * inclusive.
     * @return A frequency in Hz corresponding to the MIDI key.
     * @throws IllegalArgumentException If midiKey is not in the valid range between 0 and 127,
     * inclusive.
     */
    fun midiKeyToHertz(midiKey: Int): Double {
        require(!(midiKey < 0 || midiKey > 127)) {
            ("MIDI keys are values from 0 to 127, inclusive " + midiKey
                    + " is invalid.")
        }
        return midiCentToHertz(midiKey.toDouble())
    }

    /**
     * Converts a Hertz value to relative cents. E.g. 440Hz is converted to 900
     * if the reference is a C.
     *
     * @param hertzValue A value in hertz.
     * @return A value in relative cents.
     */
    @JvmStatic
    fun hertzToRelativeCent(hertzValue: Double): Double {
        var absoluteCentValue = hertzToAbsoluteCent(hertzValue)
        // make absoluteCentValue positive. E.g. -2410 => 1210
        if (absoluteCentValue < 0) {
            absoluteCentValue = abs(1200 + absoluteCentValue)
        }
        // so it can be folded to one octave. E.g. 1210 => 10
        return absoluteCentValue % 1200.0
    }
    /**
     * This method is not really practical. Maybe I will need it someday.
     *
     * @param relativeCent
     * @return public static double relativeCentToHertz(double relativeCent){ if
     * (relativeCent < 0 || relativeCent >= 1200) throw new
     * IllegalArgumentException
     * ("Relative cent values are values from 0 to 1199, inclusive " +
     * relativeCent + " is invalid."); int defaultOctave = 5; int offset
     * = defaultOctave * 1200; return absoluteCentToHertz(relativeCent +
     * offset); }
     */
    /**
     * The reference frequency is configured. The default reference frequency is
     * 16.35Hz. This is C0 on a piano keyboard with A4 tuned to 440 Hz. This
     * means that 0 cents is C0; 1200 is C1; 2400 is C2; ... also -1200 cents is
     * C-1
     *
     * @param hertzValue The pitch in Hertz.
     * @return The value in absolute cents using the configured reference
     * frequency
     */
    @JvmStatic
    fun hertzToAbsoluteCent(hertzValue: Double): Double {
        require(hertzValue > 0) {
            "Pitch in Hz should be greater than zero, is $hertzValue"
        }
        return 1200 * ln(hertzValue / REF_FREQ) / LOG_TWO
    }

    /**
     * Returns the frequency (Hz) of an absolute cent value. This calculation
     * uses a configured reference frequency.
     *
     * @param absoluteCent The pitch in absolute cent.
     * @return A pitch in Hz.
     */
    @JvmStatic
    fun absoluteCentToHertz(absoluteCent: Double): Double {
        return REF_FREQ * 2.0.pow(absoluteCent / 1200.0)
    }

    /**
     * Converts a frequency in Hz to a MIDI CENT value using
     * `(12 * log2 (f / 440)) + 69` <br></br>
     * E.g.<br></br>
     * `69.168 MIDI CENTS = MIDI NOTE 69  + 16,8 cents`<br></br>
     * `69.168 MIDI CENTS = 440Hz + x Hz`
     *
     * @param hertzValue The pitch in Hertz.
     * @return The pitch in MIDI cent.
     */
    fun hertzToMidiCent(hertzValue: Double): Double {
        var pitchInMidiCent = 0.0
        if (hertzValue != 0.0) {
            pitchInMidiCent = 12 * ln(hertzValue / 440) / LOG_TWO + 69
        }
        return pitchInMidiCent
    }

    /**
     * Converts a MIDI CENT frequency to a frequency in Hz.
     *
     * @param midiCent The pitch in MIDI CENT.
     * @return The pitch in Hertz.
     */
    fun midiCentToHertz(midiCent: Double): Double {
        return 440 * 2.0.pow((midiCent - 69) / 12.0)
    }

    /**
     * Converts cent values to ratios. See
     * "Ratios Make Cents: Conversions from ratios to cents and back again" in
     * the book "Tuning Timbre Spectrum Scale" William A. Sethares.
     *
     * @param cent A cent value
     * @return A ratio containing the same information.
     */
    fun centToRatio(cent: Double): Double {
        return 10.0.pow(log10(2.0) * cent / 1200.0)
    }

    /**
     * Converts a ratio to cents.
     * "Ratios Make Cents: Conversions from ratios to cents and back again" in
     * the book "Tuning Timbre Spectrum Scale" William A. Sethares
     *
     * @param ratio A cent value
     * @return A ratio containing the same information.
     */
    @JvmStatic
    fun ratioToCent(ratio: Double): Double {
        return 1200 / log10(2.0) * log10(ratio)
    }
}