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

import java.util.*

/**
 * Implements a moving mean adaptive threshold peak picker.
 *
 *
 * The implementation is a translation of peakpicker.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem></piem>@aubio.org>
 *
 * @author Joren Six
 * @author Paul Brossiers
 */
class PeakPicker(
    /**
     * thresh: offset threshold [0.033 or 0.01]
     * The threshold defines when a peak is selected. It should be
     * between zero and one, 0.3 is a reasonable value. If too many
     * peaks are detected go to 0.5 - 0.8.
     */
    var threshold: Double
) {

    /**
     * win_post: median filter window length (causal part) [8]
     */
    private val win_post: Int

    /**
     * pre: median filter window (anti-causal part) [post-1]
     */
    private val win_pre: Int

    /**
     * biquad low pass filter
     */
    private val biquad: BiQuadFilter = BiQuadFilter(0.1600, 0.3200, 0.1600, -0.5949, 0.2348)

    /**
     * original onsets
     */
    private val onset_keep: FloatArray

    /**
     * modified onsets
     */
    private val onset_proc: FloatArray

    /**
     * peak picked window [3]
     */
    private val onset_peek: FloatArray

    /**
     * scratch pad for biquad and median
     */
    private val scratch: FloatArray

    /**
     * @return The value of the last detected peak, or zero.
     */
    var lastPeekValue = 0f
        private set

    /**
     * Modified version for real time, moving mean adaptive threshold this
     * method is slightly more permissive than the off-LineWavelet one, and yields to
     * an increase of false positives.
     *
     * @param onset The new onset value.
     * @return True if a peak is detected, false otherwise.
     */
    fun pickPeak(onset: Float): Boolean {
        var mean = 0f
        var median = 0f
        val length = win_post + win_pre + 1


        /* store onset in onset_keep */
        /* shift all elements but last, then write last */
        /* for (i=0;i<channels;i++) { */for (j in 0 until length - 1) {
            onset_keep[j] = onset_keep[j + 1]
            onset_proc[j] = onset_keep[j]
        }
        onset_keep[length - 1] = onset
        onset_proc[length - 1] = onset

        /* filter onset_proc */
        /** \bug filtfilt calculated post+pre times, should be only once !?  */
        biquad.doFiltering(onset_proc, scratch)

        /* calculate mean and median for onset_proc */

        /* copy to scratch */
        var sum = 0.0f
        for (j in 0 until length) {
            scratch[j] = onset_proc[j]
            sum += scratch[j]
        }
        Arrays.sort(scratch)
        median = scratch[scratch.size / 2]
        mean = sum / length.toFloat()

        /* shift peek array */
        System.arraycopy(onset_peek, 1, onset_peek, 0, 3 - 1)
        /* calculate new peek value */
        onset_peek[2] = (onset_proc[win_post] - median - mean * threshold).toFloat()
        val isPeak = isPeak(1)
        lastPeekValue = onset
        return isPeak
    }

    /**
     * Returns true if the onset is a peak.
     *
     * @param index the index in onset_peak to check.
     * @return True if the onset is a peak, false otherwise.
     */
    private fun isPeak(index: Int): Boolean {
        return onset_peek[index] > onset_peek[index - 1] && onset_peek[index] > onset_peek[index + 1] && onset_peek[index] > 0.0
    }
    init {
        /* Low-pass filter cutoff [0.34, 1] */
        win_post = 5
        win_pre = 1
        onset_keep = FloatArray(win_post + win_pre + 1)
        onset_proc = FloatArray(win_post + win_pre + 1)
        scratch = FloatArray(win_post + win_pre + 1)
        onset_peek = FloatArray(3)
    }
}