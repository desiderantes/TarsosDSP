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

import kotlin.math.max
import kotlin.math.pow

/**
 *
 *
 * An overlap-add technique based on waveform similarity (WSOLA) for high
 * quality time-scale modification of speech
 *
 *
 *
 * A concept of waveform similarity for tackling the problem of time-scale
 * modification of speech is proposed. It is worked out in the context of
 * short-time Fourier transform representations. The resulting WSOLA
 * (waveform-similarity-based synchronized overlap-add) algorithm produces
 * high-quality speech output, is algorithmically and computationally efficient
 * and robust, and allows for online processing with arbitrary time-scaling
 * factors that may be specified in a time-varying fashion and can be chosen
 * over a wide continuous range of values.
 *
 *
 *
 * Inspired by the work soundtouch by Olli Parviainen,
 * http://www.surina.net/soundtouch, especially the TDStrech.cpp file.
 *
 *
 * @author Joren Six
 * @author Olli Parviainen
 */
class WaveformSimilarityBasedOverlapAdd(params: Parameters) :
    AudioProcessor {
    private var seekWindowLength = 0
    private var seekLength = 0
    private var overlapLength = 0
    private var pMidBuffer: FloatArray? = null
    private var pRefMidBuffer: FloatArray? = null
    private var outputFloatBuffer: FloatArray
    private var intskip = 0
    var inputBufferSize = 0
        private set
    private var tempo = 0.0
    private var dispatcher: AudioDispatcher? = null
    private var lastParams = params

    fun setDispatcher(newDispatcher: AudioDispatcher?) {
        dispatcher = newDispatcher
    }

    fun applyNewParameters(params: Parameters) {
        val oldOverlapLength = overlapLength
        overlapLength = (params.sampleRate * params.overlapMs / 1000).toInt()
        seekWindowLength = (params.sampleRate * params.sequenceMs / 1000).toInt()
        seekLength = (params.sampleRate * params.seekWindowMs / 1000).toInt()
        tempo = params.tempo

        //pMidBuffer and pRefBuffer are initialized with 8 times the needed length to prevent a reset
        //of the arrays when overlapLength changes.
        if (overlapLength > oldOverlapLength * 8 && pMidBuffer == null) {
            pMidBuffer = FloatArray(overlapLength * 8) //overlapLengthx2?
            pRefMidBuffer = FloatArray(overlapLength * 8) //overlapLengthx2?
            println("New overlapLength$overlapLength")
        }
        val nominalSkip = tempo * (seekWindowLength - overlapLength)
        intskip = (nominalSkip + 0.5).toInt()
        inputBufferSize = max(intskip + overlapLength, seekWindowLength) + seekLength
        outputFloatBuffer = outputFloatBuffer.sliceArray(0..outputBufferSize)
        lastParams = params
    }

    private val outputBufferSize: Int
        get() = seekWindowLength - overlapLength

    val overlap: Int
        get() = inputBufferSize - intskip

    /**
     * Overlaps the sample in output with the samples in input.
     *
     * @param output The output buffer.
     * @param input  The input buffer.
     */
    private fun overlap(
        output: FloatArray,
        outputOffset: Int,
        input: FloatArray,
        inputOffset: Int
    ) {
        for (i in 0 until overlapLength) {
            val itemp = overlapLength - i
            output[i + outputOffset] = (input[i + inputOffset] * i + pMidBuffer!![i] * itemp) / overlapLength
        }
    }

    /**
     * Seeks for the optimal overlap-mixing position.
     *
     *
     * The best position is determined as the position where the two overlapped
     * sample sequences are 'most alike', in terms of the highest
     * cross-correlation value over the overlapping period
     *
     * @param inputBuffer The input buffer
     * @param postion     The position where to start the seek operation, in the input buffer.
     * @return The best position.
     */
    private fun seekBestOverlapPosition(inputBuffer: FloatArray, postion: Int): Int {
        var bestOffset: Int
        var bestCorrelation: Double
        var currentCorrelation: Double
        var tempOffset: Int
        var comparePosition: Int

        // Slopes the amplitude of the 'midBuffer' samples
        precalcCorrReferenceMono()
        bestCorrelation = -10.0
        bestOffset = 0

        // Scans for the best correlation value by testing each possible
        // position
        // over the permitted range.
        tempOffset = 0
        while (tempOffset < seekLength) {
            comparePosition = postion + tempOffset

            // Calculates correlation value for the mixing position
            // corresponding
            // to 'tempOffset'
            currentCorrelation = calcCrossCorr(pRefMidBuffer!!, inputBuffer, comparePosition)
            // heuristic rule to slightly favor values close to mid of the
            // range
            val tmp = (2 * tempOffset - seekLength).toDouble() / seekLength
            currentCorrelation = (currentCorrelation + 0.1) * (1.0 - 0.25 * tmp * tmp)

            // Checks for the highest correlation value
            if (currentCorrelation > bestCorrelation) {
                bestCorrelation = currentCorrelation
                bestOffset = tempOffset
            }
            tempOffset++
        }
        return bestOffset
    }

    /**
     * Slopes the amplitude of the 'midBuffer' samples so that cross correlation
     * is faster to calculate. Why is this faster?
     */
    fun precalcCorrReferenceMono() {
        for (i in 0 until overlapLength) {
            val temp = i * (overlapLength - i).toFloat()
            pRefMidBuffer!![i] = pMidBuffer!![i] * temp
        }
    }

    fun calcCrossCorr(mixingPos: FloatArray, compare: FloatArray, offset: Int): Double {
        var corr = 0.0
        var norm = 0.0
        for (i in 1 until overlapLength) {
            corr += mixingPos[i] * compare[i + offset].toDouble()
            norm += mixingPos[i] * mixingPos[i].toDouble()
        }
        // To avoid division by zero.
        if (norm < 1e-8) {
            norm = 1.0
        }
        return corr / norm.pow(0.5)
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.floatBuffer
        assert(audioFloatBuffer.size == inputBufferSize)

        //Search for the best overlapping position.
        val offset = seekBestOverlapPosition(audioFloatBuffer, 0)

        // Mix the samples in the 'inputBuffer' at position of 'offset' with the
        // samples in 'midBuffer' using sliding overlapping
        // ... first partially overlap with the end of the previous sequence
        // (that's in 'midBuffer')
        overlap(outputFloatBuffer, 0, audioFloatBuffer, offset)

        //copy sequence samples from input to output
        val sequenceLength = seekWindowLength - 2 * overlapLength
        System.arraycopy(
            audioFloatBuffer,
            offset + overlapLength,
            outputFloatBuffer,
            overlapLength,
            sequenceLength
        )

        // Copies the end of the current sequence from 'inputBuffer' to
        // 'midBuffer' for being mixed with the beginning of the next
        // processing sequence and so on
        System.arraycopy(
            audioFloatBuffer,
            offset + sequenceLength + overlapLength,
            pMidBuffer,
            0,
            overlapLength
        )
        assert(outputFloatBuffer.size == outputBufferSize)
        audioEvent.floatBuffer = outputFloatBuffer
        audioEvent.overlap = 0
        lastParams.also {
            applyNewParameters(it)
            dispatcher!!.setStepSizeAndOverlap(inputBufferSize, overlap)
        }
        return true
    }

    override fun processingFinished() {
        // NOOP
    }

    /**
     * An object to encapsulate some of the parameters for
     * WSOLA, together with a couple of practical helper functions.
     *
     * @author Joren Six
     */
    data class Parameters
    /**
     * @param tempo           The tempo change 1.0 means unchanged, 2.0 is + 100% , 0.5
     * is half of the speed.
     * @param sampleRate      The sample rate of the audio 44.1kHz is common.
     * @param newSequenceMs   Length of a single processing sequence, in milliseconds.
     * This determines to how long sequences the original sound
     * is chopped in the time-stretch algorithm.
     *
     *
     * The larger this value is, the lesser sequences are used in
     * processing. In principle a bigger value sounds better when
     * slowing down tempo, but worse when increasing tempo and
     * vice versa.
     *
     *
     * Increasing this value reduces computational burden & vice
     * versa.
     * @param newSeekWindowMs Seeking window length in milliseconds for algorithm that
     * finds the best possible overlapping location. This
     * determines from how wide window the algorithm may look for
     * an optimal joining location when mixing the sound
     * sequences back together.
     *
     *
     * The bigger this window setting is, the higher the
     * possibility to find a better mixing position will become,
     * but at the same time large values may cause a "drifting"
     * artifact because consequent sequences will be taken at
     * more uneven intervals.
     *
     *
     * If there's a disturbing artifact that sounds as if a
     * constant frequency was drifting around, try reducing this
     * setting.
     *
     *
     * Increasing this value increases computational burden &
     * vice versa.
     * @param newOverlapMs    Overlap length in milliseconds. When the chopped sound
     * sequences are mixed back together, to form a continuous
     * sound stream, this parameter defines over how long period
     * the two consecutive sequences are let to overlap each
     * other.
     *
     *
     * This shouldn't be that critical parameter. If you reduce
     * the DEFAULT_SEQUENCE_MS setting by a large amount, you
     * might wish to try a smaller value on this.
     *
     *
     * Increasing this value increases computational burden &
     * vice versa.
     */(
        val tempo: Double,
        val sampleRate: Double,
        val sequenceMs: Double,
        val seekWindowMs: Double,
        val overlapMs: Double
    ) {

        companion object {
            @JvmStatic
            fun speechDefaults(
                tempo: Double,
                sampleRate: Double
            ): Parameters {
                val sequenceMs = 40.0
                val seekWindowMs = 15.0
                val overlapMs = 12.0
                return Parameters(
                    tempo,
                    sampleRate,
                    sequenceMs,
                    seekWindowMs,
                    overlapMs
                )
            }

            @JvmStatic
            fun musicDefaults(
                tempo: Double,
                sampleRate: Double
            ): Parameters {
                val sequenceMs = 82.0
                val seekWindowMs = 28.0
                val overlapMs = 12.0
                return Parameters(
                    tempo,
                    sampleRate,
                    sequenceMs,
                    seekWindowMs,
                    overlapMs
                )
            }

            @JvmStatic
            fun slowdownDefaults(
                tempo: Double,
                sampleRate: Double
            ): Parameters {
                val sequenceMs = 100.0
                val seekWindowMs = 35.0
                val overlapMs = 20.0
                return Parameters(
                    tempo,
                    sampleRate,
                    sequenceMs,
                    seekWindowMs,
                    overlapMs
                )
            }

            @JvmStatic
            fun automaticDefaults(
                tempo: Double,
                sampleRate: Double
            ): Parameters {
                val tempoLow = 0.5 // -50% speed
                val tempoHigh = 2.0 // +100% speed
                val sequenceMsLow = 125.0 //ms
                val sequenceMsHigh = 50.0 //ms
                val sequenceK = (sequenceMsHigh - sequenceMsLow) / (tempoHigh - tempoLow)
                val sequenceC = sequenceMsLow - sequenceK * tempoLow
                val seekLow = 25.0 // ms
                val seekHigh = 15.0 // ms
                val seekK = (seekHigh - seekLow) / (tempoHigh - tempoLow)
                val seekC = seekLow - seekK * seekLow
                val sequenceMs = (sequenceC + sequenceK * tempo + 0.5)
                val seekWindowMs = (seekC + seekK * tempo + 0.5)
                val overlapMs = 12.0
                return Parameters(
                    tempo,
                    sampleRate,
                    sequenceMs,
                    seekWindowMs,
                    overlapMs
                )
            }
        }

    }

    /**
     * Create a new instance based on algorithm parameters for a certain audio format.
     *
     * @param params The parameters for the algorithm.
     */
    init {
        outputFloatBuffer = FloatArray(outputBufferSize)
        applyNewParameters(params)
    }
}