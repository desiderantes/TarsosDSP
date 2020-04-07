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

/**
 * A class with information about the result of a pitch detection on a block of
 * audio.
 *
 *
 * It contains:
 *
 *
 *  * The pitch in Hertz.
 *  * A probability (noisiness, (a)periodicity, salience, voicedness or clarity
 * measure) for the detected pitch. This is somewhat similar to the term voiced
 * which is used in speech recognition. This probability should be calculated
 * together with the pitch. The exact meaning of the value depends on the detector used.
 *  * A way to calculate the RMS of the signal.
 *  * A boolean that indicates if the algorithm thinks the signal is pitched or
 * not.
 *
 *
 *
 * The separate pitched or unpitched boolean can coexist with a defined pitch.
 * E.g. if the algorithm detects 220Hz in a noisy signal it may respond with
 * 220Hz "unpitched".
 *
 *
 *
 * For performance reasons the object is reused. Please create a copy of the object
 * if you want to use it on an other thread.
 *
 * @author Joren Six
 */
data class PitchDetectionResult @JvmOverloads constructor(
    /**
     * @return The pitch in Hertz.
     */
    /**
     * The pitch in Hertz.
     */
    var pitch: Float = -1f,

    /**
     * @return A probability (noisiness, (a)periodicity, salience, voicedness or
     * clarity measure) for the detected pitch. This is somewhat similar
     * to the term voiced which is used in speech recognition. This
     * probability should be calculated together with the pitch. The
     * exact meaning of the value depends on the detector used.
     */
    var probability: Float = -1f,

    /**
     * @return Whether the algorithm thinks the block of audio is pitched. Keep
     * in mind that an algorithm can come up with a best guess for a
     * pitch even when isPitched() is false.
     */
    var isPitched: Boolean = false
)