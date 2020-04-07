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

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import kotlin.math.roundToInt

/**
 * Is responsible to call a pitch estimation algorithm. It also calculates progress.
 * The underlying pitch detection algorithm must implement the [PitchDetector] interface.
 *
 * @author Joren Six
 */
class PitchProcessor(
    algorithm: PitchEstimationAlgorithm, sampleRate: Float,
    bufferSize: Int,
    private val handler: PitchDetectionHandler
) : AudioProcessor {
    /**
     * The underlying pitch detector;
     */
    private val detector: PitchDetector = algorithm.getDetector(sampleRate, bufferSize)
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        val result = detector.getPitch(audioFloatBuffer)
        handler.handlePitch(result, audioEvent)
        return true
    }

    override fun processingFinished() {}

    /**
     * A list of pitch estimation algorithms.
     *
     * @author Joren Six
     */
    enum class PitchEstimationAlgorithm {
        /**
         * See [Yin] for the implementation. Or see [the YIN article](http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf).
         */
        YIN,

        /**
         * See [McLeodPitchMethod]. It is described in the article "[A Smarter Way to Find Pitch](http://miracle.otago.ac.nz/postgrads/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf)".
         */
        MPM,

        /**
         * A YIN implementation with a faster  [FastYin] for the implementation. Or see [the YIN article](http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf).
         */
        FFT_YIN,

        /**
         * An implementation of a dynamic wavelet pitch detection algorithm (See
         * [DynamicWavelet]), described in a paper by Eric Larson and Ross
         * Maddox [](http://online.physics.uiuc)/courses/phys498pom/NSF_REU_Reports/2005_reu/Real
         * -Time_Time-Domain_Pitch_Tracking_Using_Wavelets.pdf">"Real-Time
         * Time-Domain Pitch Tracking Using Wavelets
         */
        DYNAMIC_WAVELET,

        /**
         * Returns the frequency of the FFT-bin with most energy.
         */
        FFT_PITCH,

        /**
         * A pitch extractor that extracts the Average Magnitude Difference
         * (AMDF) from an audio buffer. This is a good measure of the Pitch (f0)
         * of a signal.
         */
        AMDF;

        /**
         * Returns a new instance of a pitch detector object based on the provided values.
         *
         * @param sampleRate The sample rate of the audio buffer.
         * @param bufferSize The size (in samples) of the audio buffer.
         * @return A new pitch detector object.
         */
        fun getDetector(sampleRate: Float, bufferSize: Int): PitchDetector {
            return when {
                this == MPM -> {
                    McLeodPitchMethod(sampleRate, bufferSize)
                }
                this == DYNAMIC_WAVELET -> {
                    DynamicWavelet(sampleRate, bufferSize)
                }
                this == FFT_YIN -> {
                    FastYin(sampleRate, bufferSize)
                }
                this == AMDF -> {
                    AMDF(sampleRate, bufferSize)
                }
                this == FFT_PITCH -> {
                    FFTPitch(sampleRate.roundToInt(), bufferSize)
                }
                else -> {
                    Yin(sampleRate, bufferSize)
                }
            }
        }
    }
}