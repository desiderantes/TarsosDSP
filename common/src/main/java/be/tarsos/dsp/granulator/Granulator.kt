package be.tarsos.dsp.granulator

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.fft.CosineWindow
import java.util.*
import kotlin.math.floor

/**
 * Granulator plays back samples using granular synthesis.
 * Methods can be used to control playback rate, pitch, grain size,
 * grain interval and grain randomness and position (this last case assumes that the playback rate is zero).
 *
 * @author ollie
 * @author Joren
 */
class Granulator(sampleRate: Float, bufferSize: Int) : AudioProcessor {
    /**
     * The window used by grains.
     */
    private val window: FloatArray = CosineWindow().generateCurve(bufferSize)
    private val audioBuffer: FloatArray = FloatArray((12 * 60 * sampleRate).toInt())
    private val outputBuffer: FloatArray = FloatArray(bufferSize)

    /**
     * The position in milliseconds.
     */
    protected var position = 0.0


    private var grainInterval: Float  = 40.0f
    private var grainSize: Float  = 100.0f
    private var grainRandomness: Float = 0.1f

    /**
     * The time in milliseconds since the last grain was activated.
     */
    private var timeSinceLastGrain = 0f

    /**
     * The length of one sample in milliseconds.
     */
    private val msPerSample: Double = 1000.0f / sampleRate.toDouble()

    /**
     * The millisecond position increment per sample. Calculated from the ratio
     * of the [AudioContext]'s sample rate and the [Sample]'s sample
     * rate.
     */
    private val positionIncrement: Double = msPerSample

    /**
     * The pitch, bound to the pitch envelope.
     */
    private var pitchFactor: Float  = 1.0f

    /**
     * The pitch, bound to the pitch envelope.
     */
    private var timeStretchFactor = 0f
    /** The interpolation type.  */ //protected InterpolationType interpolationType;
    /**
     * The list of current grains.
     */
    private val grains: ArrayList<Grain?> = ArrayList()

    /**
     * A list of free grains.
     */
    private val freeGrains: ArrayList<Grain?> = ArrayList()

    /**
     * A list of dead grains.
     */
    private val deadGrains: ArrayList<Grain?> = ArrayList()
    private var audioBufferWatermark: Int = 0

    /**
     * Flag to indicate special case for the first grain.
     */
    private var firstGrain = true
    fun start() {
        timeSinceLastGrain = 0f
    }

    /**
     * Special case method for playing first grain.
     */
    private fun firstGrain() {
        if (firstGrain) {
            val g = Grain()
            g.position = position
            g.age = (grainSize / 4f).toDouble()
            g.grainSize = grainSize.toDouble()
            grains.add(g)
            firstGrain = false
            timeSinceLastGrain = grainInterval / 2f
        }
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        System.arraycopy(
            audioEvent.floatBuffer, 0, audioBuffer,
            audioBufferWatermark, audioEvent.bufferSize
        )
        audioBufferWatermark += audioEvent.bufferSize

        // grains.clear();
        // position = audioEvent.getTimeStamp()*1000 - 5000;

        // reset output
        Arrays.fill(outputBuffer, 0f)
        firstGrain()
        val bufferSize = audioEvent.bufferSize

        // now loop through the buffer
        for (i in 0 until bufferSize) {
            // determine if we need a new grain
            if (timeSinceLastGrain > grainInterval) {
                val g: Grain = if (freeGrains.size > 0) {
                    freeGrains.removeAt(0)!!
                } else {
                    Grain()
                }
                g.reset(
                    grainSize.toDouble(),
                    grainRandomness.toDouble(),
                    position,
                    timeStretchFactor.toDouble(),
                    pitchFactor.toDouble()
                )
                grains.add(g)
                timeSinceLastGrain = 0f
                //System.out.println(grains.size());
            }

            // gather the output from each grain
            for (g in grains) {
                // calculate value of grain window
                val windowScale = getValueFraction((g!!.age / g.grainSize).toFloat())
                // get position in sample for this grain
                // get the frame for this grain
                getFrameLinear(g.position)
                var sampleValue: Double = when {
                    pitchFactor > ADAPTIVE_INTERP_HIGH_THRESH -> {
                        getFrameNoInterp(g.position).toDouble()
                    }
                    pitchFactor > ADAPTIVE_INTERP_LOW_THRESH -> {
                        getFrameLinear(g.position)
                    }
                    else -> {
                        getFrameCubic(g.position).toDouble()
                    }
                }
                sampleValue *= windowScale
                outputBuffer[i] += sampleValue.toFloat()
            }
            // increment time
            position += positionIncrement * timeStretchFactor
            for (g in grains) {
                calculateNextGrainPosition(g)
            }
            // increment timeSinceLastGrain
            timeSinceLastGrain += msPerSample.toFloat()
            // finally, see if any grains are dead
            for (g in grains) {
                if (g!!.age > g.grainSize) {
                    freeGrains.add(g)
                    deadGrains.add(g)
                }
            }
            for (g in deadGrains) {
                grains.remove(g)
            }
            deadGrains.clear()
        }
        audioEvent.floatBuffer = outputBuffer
        return true
    }

    /**
     * Retrieves a frame of audio using linear interpolation. If the frame is
     * not in the sample range then zeros are returned.
     *
     * @param posInMS The frame to read -- can be fractional (e.g., 4.4).
     * @param result  The framedata to fill.
     */
    fun getFrameLinear(posInMS: Double): Double {
        var result = 0.0
        val sampleNumber = msToSamples(posInMS)
        val sampleNumberFloor = floor(sampleNumber).toInt()
        if (sampleNumberFloor in 1 until audioBufferWatermark) {
            val sampleNumberFraction = sampleNumber - sampleNumberFloor
            result = if (sampleNumberFloor == audioBufferWatermark - 1) {
                audioBuffer[sampleNumberFloor].toDouble()
            } else {
                // linear interpolation
                val current = audioBuffer[sampleNumberFloor].toDouble()
                val next = audioBuffer[sampleNumberFloor].toDouble()
                ((1 - sampleNumberFraction) * current + sampleNumberFraction * next)
            }
        }
        return result
    }

    /**
     * Retrieves a frame of audio using no interpolation. If the frame is not in
     * the sample range then zeros are returned.
     *
     * @param posInMS The frame to read -- will take the last frame before this one.
     */
    fun getFrameNoInterp(posInMS: Double): Float {
        val frame = msToSamples(posInMS)
        val frame_floor = floor(frame).toInt()
        return audioBuffer[frame_floor]
    }

    /**
     * Retrieves a frame of audio using cubic interpolation. If the frame is not
     * in the sample range then zeros are returned.
     *
     * @param posInMS The frame to read -- can be fractional (e.g., 4.4).
     */
    fun getFrameCubic(posInMS: Double): Float {
        val frame = msToSamples(posInMS).toFloat()
        var result = 0.0f
        val a0: Float
        val a1: Float
        val a2: Float
        val a3: Float
        val mu2: Float
        val ym1: Float
        val y0: Float
        val y1: Float
        val y2: Float
        var realCurrentSample = floor(frame.toDouble()).toInt()
        val fractionOffset = frame - realCurrentSample
        if (realCurrentSample >= 0 && realCurrentSample < audioBufferWatermark - 1) {
            realCurrentSample--
            if (realCurrentSample < 0) {
                ym1 = audioBuffer[0]
                realCurrentSample = 0
            } else {
                ym1 = audioBuffer[realCurrentSample++]
            }
            y0 = audioBuffer[realCurrentSample++]
            y1 = if (realCurrentSample >= audioBufferWatermark) {
                audioBuffer[audioBufferWatermark - 1] // ??
            } else {
                audioBuffer[realCurrentSample++]
            }
            y2 = if (realCurrentSample >= audioBufferWatermark) {
                audioBuffer[audioBufferWatermark - 1]
            } else {
                audioBuffer[realCurrentSample++]
            }
            mu2 = fractionOffset * fractionOffset
            a0 = y2 - y1 - ym1 + y0
            a1 = ym1 - y0 - a0
            a2 = y1 - ym1
            a3 = y0
            result = a0 * fractionOffset * mu2 + a1 * mu2 + a2 * fractionOffset + a3
        }
        return result
    }

    private fun msToSamples(posInMs: Double): Double {
        return posInMs / msPerSample
    }

    override fun processingFinished() {}

    /**
     * Returns the value of the buffer at the given fraction along its length (0 = start, 1 = end). Uses linear interpolation.
     *
     * @param fraction the point along the buffer to inspect.
     * @return the value at that point.
     */
    fun getValueFraction(fraction: Float): Float {
        val posInBuf = fraction * window.size
        val lowerIndex = posInBuf.toInt()
        val offset = posInBuf - lowerIndex
        val upperIndex = (lowerIndex + 1) % window.size
        return (1 - offset) * window[lowerIndex] + offset * window[upperIndex]
    }

    /**
     * Calculate next position for the given Grain.
     *
     * @param g the Grain.
     */
    private fun calculateNextGrainPosition(g: Grain?) {
        val direction =
            if (timeStretchFactor >= 0) 1 else -1 //this is a bit odd in the case when controlling grain from positionEnvelope
        g!!.age += msPerSample
        g!!.position += direction * positionIncrement * pitchFactor
    }

    fun setTimestretchFactor(currentFactor: Float) {
        timeStretchFactor = currentFactor
    }

    fun setPitchShiftFactor(currentFactor: Float) {
        pitchFactor = currentFactor
    }

    fun setGrainInterval(grainInterval: Int) {
        this.grainInterval = grainInterval.toFloat()
    }

    fun setGrainSize(grainSize: Int) {
        this.grainSize = grainSize.toFloat()
    }

    fun setGrainRandomness(grainRandomness: Float) {
        this.grainRandomness = grainRandomness
    }

    /**
     * @param position in seconds
     */
    fun setPosition(position: Float) {
        this.position = position * 1000.toDouble()
    }

    companion object {
        const val ADAPTIVE_INTERP_LOW_THRESH = 0.5f
        const val ADAPTIVE_INTERP_HIGH_THRESH = 2.5f
    }
}