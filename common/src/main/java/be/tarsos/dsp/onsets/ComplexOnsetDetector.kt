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
package be.tarsos.dsp.onsets

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.PeakPicker
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HannWindow

/**
 * A complex Domain Method onset detection function
 *
 *
 * Christopher Duxbury, Mike E. Davies, and Mark B. Sandler. Complex domain
 * onset detection for musical signals. In Proceedings of the Digital Audio
 * Effects Conference, DAFx-03, pages 90-93, London, UK, 2003
 *
 *
 * The implementation is a translation of onset.c from Aubio, Copyright (C)
 * 2003-2009 Paul Brossier <piem></piem>@aubio.org>
 *
 * @author Joren Six
 * @author Paul Brossiers
 */
class ComplexOnsetDetector @JvmOverloads constructor(
    fftSize: Int,
    peakThreshold: Double = 0.3,
    /**
     * The minimum IOI (inter onset interval), in seconds.
     */
    private val minimumInterOnsetInterval: Double = 0.03,
    /**
     * The threshold to define silence, in dbSPL.
     */
    private val silenceThreshold: Double = -70.0
) : AudioProcessor, OnsetDetector {

    private val peakPicker: PeakPicker = PeakPicker(peakThreshold)

    /**
     * To calculate the FFT.
     */
    private val fft: FFT = FFT(fftSize, HannWindow())

    /**
     * Previous phase vector, one frame behind
     */
    private val theta1: FloatArray

    /**
     * Previous phase vector, two frames behind
     */
    private val theta2: FloatArray

    /**
     * Previous norm (power, magnitude) vector
     */
    private val oldmag: FloatArray

    /**
     * Current onset detection measure vector
     */
    private val dev1: FloatArray

    /**
     * The last detected onset, in seconds.
     */
    private var lastOnset = 0.0

    /**
     * The last detected onset value.
     */
    private var lastOnsetValue = 0.0
    private var handler: OnsetHandler = PrintOnsetHandler()
    override fun process(audioEvent: AudioEvent): Boolean {
        onsetDetection(audioEvent)
        return true
    }

    private fun onsetDetection(audioEvent: AudioEvent) {
        //calculate the complex fft (the magnitude and phase)
        val data = audioEvent.floatBuffer.clone()
        val power = FloatArray(data.size / 2)
        val phase = FloatArray(data.size / 2)
        fft.powerPhaseFFT(data, power, phase)
        var onsetValue = 0f
        for (j in power.indices) {
            //int imgIndex = (power.length - 1) * 2 - j;

            // compute the predicted phase
            dev1[j] = 2f * theta1[j] - theta2[j]

            // compute the euclidean distance in the complex domain
            // sqrt ( r_1^2 + r_2^2 - 2 * r_1 * r_2 * \cos ( \phi_1 - \phi_2 ) )
            onsetValue += Math.sqrt(
                Math.abs(
                    Math.pow(
                        oldmag[j].toDouble(),
                        2.0
                    ) + Math.pow(
                        power[j].toDouble(),
                        2.0
                    ) - 2.0 * oldmag[j] * power[j] * Math.cos(dev1[j] - phase[j].toDouble())
                )
            ).toFloat()

            /* swap old phase data (need to remember 2 frames behind)*/theta2[j] = theta1[j]
            theta1[j] = phase[j]

            /* swap old magnitude data (1 frame is enough) */oldmag[j] = power[j]
        }
        lastOnsetValue = onsetValue.toDouble()
        var isOnset = peakPicker.pickPeak(onsetValue)
        if (isOnset) {
            if (audioEvent.isSilence(silenceThreshold)) {
                isOnset = false
            } else {
                val delay = audioEvent.overlap * 4.3 / audioEvent.sampleRate
                val onsetTime = audioEvent.timeStamp - delay
                if (onsetTime - lastOnset > minimumInterOnsetInterval) {
                    handler.handleOnset(onsetTime, peakPicker.lastPeekValue.toDouble())
                    lastOnset = onsetTime
                }
            }
        }
    }

    override fun setHandler(handler: OnsetHandler) {
        this.handler = handler
    }

    fun setThreshold(threshold: Double) {
        peakPicker.threshold = threshold
    }

    override fun processingFinished() {}

    /**
     * @param fftSize                   The size of the fft to take (e.g. 512)
     * @param peakThreshold             A threshold used for peak picking. Values between 0.1 and 0.8. Default is 0.3, if too many onsets are detected adjust to 0.4 or 0.5.
     * @param silenceThreshold          The threshold that defines when a buffer is silent. Default is -70dBSPL. -90 is also used.
     * @param minimumInterOnsetInterval The minimum inter-onset-interval in seconds. When two onsets are detected within this interval the last one does not count. Default is 0.004 seconds.
     */
    init {
        val rsize = fftSize / 2 + 1
        oldmag = FloatArray(rsize)
        dev1 = FloatArray(rsize)
        theta1 = FloatArray(rsize)
        theta2 = FloatArray(rsize)
    }
}