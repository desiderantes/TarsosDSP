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

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.beatroot.Peaks
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.ScaledHammingWindow
import java.util.*
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 *
 *
 * A non real-time spectral flux onset detection method, as implemented in the
 * BeatRoot system of Centre for Digital Music, Queen Mary, University of
 * London.
 *
 *
 *
 *
 * This onset detection function does not, NOT work in real-time. It analyzes an
 * audio-stream and detects onsets during a post processing step.
 *
 *
 * @author Joren Six
 * @author Simon Dixon
 */
class BeatRootSpectralFluxOnsetDetector(
    d: AudioDispatcher,
    /**
     * The size of an FFT frame in samples (see `fftTime`)
     */
    protected var fftSize: Int,
    /**
     * Spacing of audio frames in samples (see `hopTime`)
     */
    protected var hopSize: Int
) : AudioProcessor,
    OnsetDetector {
    private val fft: FFT = FFT(fftSize, ScaledHammingWindow())

    /**
     * RMS amplitude of the current frame.
     */
    private var frameRMS = 0.0

    /**
     * The number of overlapping frames of audio data which have been read.
     */
    private var frameCount = 0

    /**
     * Long term average frame energy (in frequency domain representation).
     */
    private var ltAverage = 0.0

    /**
     * The real part of the data for the in-place FFT computation.
     * Since input data is real, this initially contains the input data.
     */
    private var reBuffer: FloatArray

    /**
     * The imaginary part of the data for the in-place FFT computation.
     * Since input data is real, this initially contains zeros.
     */
    private val imBuffer: FloatArray

    /**
     * Spectral flux onset detection function, indexed by frame.
     */
    private val spectralFlux: DoubleArray

    /**
     * A mapping function for mapping FFT bins to final frequency bins.
     * The mapping is linear (1-1) until the resolution reaches 2 points per
     * semitone, then logarithmic with a semitone resolution.  e.g. for
     * 44.1kHz sampling rate and fftSize of 2048 (46ms), bin spacing is
     * 21.5Hz, which is mapped linearly for bins 0-34 (0 to 732Hz), and
     * logarithmically for the remaining bins (midi notes 79 to 127, bins 35 to
     * 83), where all energy above note 127 is mapped into the final bin.
     */
    private lateinit var freqMap: IntArray

    /**
     * The number of entries in `freqMap`. Note that the length of
     * the array is greater, because its size is not known at creation time.
     */
    private var freqMapSize = 0

    /**
     * The magnitude spectrum of the most recent frame.
     * Used for calculating the spectral flux.
     */
    private var prevFrame: FloatArray

    /**
     * The magnitude spectrum of the current frame.
     */
    private val newFrame: DoubleArray

    /**
     * The magnitude spectra of all frames, used for plotting the spectrogram.
     */
    private val frames: Array<DoubleArray>

    /**
     * The RMS energy of all frames.
     */
    private val energy: DoubleArray

    /**
     * Total number of audio frames if known, or -1 for live or compressed input.
     */
    private val totalFrames: Int
    private var handler: OnsetHandler = PrintOnsetHandler()
    private val hopTime: Double = hopSize / d.format.sampleRate.toDouble()
    override fun process(audioEvent: AudioEvent): Boolean {
        frameRMS = audioEvent.rMS / 2.0
        val audioBuffer = audioEvent.floatBuffer.clone()
        Arrays.fill(imBuffer, 0f)
        fft.powerPhaseFFTBeatRootOnset(audioBuffer, reBuffer, imBuffer)
        Arrays.fill(newFrame, 0.0)
        var flux = 0.0
        for (i in 0 until fftSize / 2) {
            if (reBuffer[i] > prevFrame[i]) flux += reBuffer[i] - prevFrame[i].toDouble()
            newFrame[freqMap[i]] = newFrame[freqMap[i]].plus(reBuffer[i])
        }
        spectralFlux[frameCount] = flux
        for (i in 0 until freqMapSize) frames[frameCount][i] = newFrame[i]
        val sz = (fftSize - hopSize) / energyOversampleFactor
        var index = hopSize
        for (j in 0 until energyOversampleFactor) {
            var newEnergy = 0.0
            for (i in 0 until sz) {
                newEnergy += audioBuffer[index] * audioBuffer[index].toDouble()
                if (++index == fftSize) index = 0
            }
            energy[frameCount * energyOversampleFactor + j] =
                if (newEnergy / sz <= 1e-6) 0.0 else ln(newEnergy / sz) + 13.816
        }
        val decay: Double =
            if (frameCount >= 200) 0.99 else if (frameCount < 100) 0.0 else (frameCount - 100) / 100.0
        ltAverage = if (ltAverage == 0.0) frameRMS else ltAverage * decay + frameRMS * (1.0 - decay)
        if (frameRMS <= silenceThreshold) for (i in 0 until freqMapSize) frames[frameCount][i] = 0.0 else {
            if (normaliseMode == 1) for (i in 0 until freqMapSize) frames[frameCount][i] /= frameRMS else if (normaliseMode == 2) for (i in 0 until freqMapSize) frames[frameCount][i] /= ltAverage
            for (i in 0 until freqMapSize) {
                frames[frameCount][i] = ln(
                    frames[frameCount][i]
                ) + rangeThreshold
                if (frames[frameCount][i] < 0) frames[frameCount][i] = 0.0
            }
        }
        val tmp = prevFrame
        prevFrame = reBuffer
        reBuffer = tmp
        frameCount++
        return true
    }

    /**
     * Creates a map of FFT frequency bins to comparison bins.
     * Where the spacing of FFT bins is less than 0.5 semitones, the mapping is
     * one to one. Where the spacing is greater than 0.5 semitones, the FFT
     * energy is mapped into semitone-wide bins. No scaling is performed; that
     * is the energy is summed into the comparison bins. See also
     * processFrame()
     */
    protected fun makeFreqMap(fftSize: Int, sampleRate: Float) {
        freqMap = IntArray(fftSize / 2 + 1)
        val binWidth = sampleRate / fftSize.toDouble()
        val crossoverBin = (2 / (2.0.pow(1 / 12.0) - 1)).toInt()
        val crossoverMidi = (ln(crossoverBin * binWidth / 440) /
                ln(2.0) * 12 + 69).roundToLong().toInt()
        // freq = 440 * Math.pow(2, (midi-69)/12.0) / binWidth;
        var i = 0
        while (i <= crossoverBin) freqMap[i++] = i
        while (i <= fftSize / 2) {
            var midi = ln(i * binWidth / 440) / ln(2.0) * 12 + 69
            if (midi > 127) midi = 127.0
            freqMap[i++] = crossoverBin + midi.roundToLong().toInt() - crossoverMidi
        }
        freqMapSize = freqMap[i - 1] + 1
    }

    private fun findOnsets(p1: Double, p2: Double) {
        val peaks =
            Peaks.findPeaks(spectralFlux, (0.06 / hopTime).roundToLong().toInt(), p1, p2, true)
        val it: Iterator<Int> = peaks.iterator()
        val minSalience = Peaks.min(spectralFlux)
        for (i in peaks.indices) {
            val index = it.next()
            val time = index * hopTime
            val salience = spectralFlux[index] - minSalience
            handler.handleOnset(time, salience)
        }
    }

    override fun setHandler(handler: OnsetHandler) {
        this.handler = handler
    }

    override fun processingFinished() {
        val p1 = 0.35
        val p2 = 0.84
        Peaks.normalise(spectralFlux)
        findOnsets(p1, p2)
    }

    companion object {
        /**
         * RMS frame energy below this value results in the frame being set to zero,
         * so that normalization does not have undesired side-effects.
         */
        var silenceThreshold = 0.0004

        /**
         * For dynamic range compression, this value is added to the log magnitude
         * in each frequency bin and any remaining negative values are then set to zero.
         */
        var rangeThreshold = 10.0

        /**
         * Determines method of normalization. Values can be:
         *  * 0: no normalization
         *  * 1: normalization by current frame energy
         *  * 2: normalization by exponential average of frame energy
         *
         */
        var normaliseMode = 2

        /**
         * Ratio between rate of sampling the signal energy (for the amplitude envelope) and the hop size
         */
        var energyOversampleFactor = 2
    }

    init {
        System.err.println("Please use the ComplexOnset detector: BeatRootSpectralFluxOnsetDetector doesn't currently support streaming")
        //no overlap
        //FIXME:
        val durationInFrames = -1000
        totalFrames = (durationInFrames / hopSize) + 4
        energy = DoubleArray(totalFrames * energyOversampleFactor)
        spectralFlux = DoubleArray(totalFrames)
        reBuffer = FloatArray(fftSize / 2)
        imBuffer = FloatArray(fftSize / 2)
        prevFrame = FloatArray(fftSize / 2)
        makeFreqMap(fftSize, d.format.sampleRate)
        newFrame = DoubleArray(freqMapSize)
        frames = Array(totalFrames) { DoubleArray(freqMapSize) }
        handler = PrintOnsetHandler()

    }
}