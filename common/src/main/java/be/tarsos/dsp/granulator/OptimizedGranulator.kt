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
class OptimizedGranulator(sampleRate: Float, bufferSize: Int) : AudioProcessor {
    /**
     * The window used by grains.
     */
    private val window: FloatArray = CosineWindow().generateCurve(512)
    private val audioBuffer: FloatArray = FloatArray(4800 * 2)
    private val outputBuffer: FloatArray = FloatArray(bufferSize)

    /**
     * The position in milliseconds.
     */
    protected var position = 0.0

    /**
     * The millisecond position increment per sample. Calculated from the ratio
     * of the sample rate
     */
    private val audioSampleLength: Double = 1000.0f / sampleRate.toDouble()
    private var grainInterval: Float = 40f
    private var grainSize: Float = 100f
    private var grainRandomness: Float = 0.1f

    /**
     * The time in milliseconds since the last grain was activated.
     */
    private var timeSinceLastGrain = 0f
    /** The interpolation type.  */ //protected InterpolationType interpolationType;
    /**
     * The pitch, bound to the pitch envelope.
     */
    private var pitchFactor: Float = 1F

    /**
     * The pitch, bound to the pitch envelope.
     */
    private var timeStretchFactor = 0f

    /**
     * The list of current grains.
     */
    private val grains: Array<Grain> = Array(50) { Grain() }
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
            val g = grains[0]
            g.position = position
            g.age = (grainSize / 4f).toDouble()
            g.grainSize = grainSize.toDouble()
            firstGrain = false
            timeSinceLastGrain = grainInterval / 2f
        }
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val bufferSize = audioEvent.bufferSize
        for (i in 0 until bufferSize) {
            audioBuffer[audioBufferWatermark] = audioEvent.floatBuffer[i]
            audioBufferWatermark++
            if (audioBufferWatermark == audioBuffer.size) {
                audioBufferWatermark = 0
            }
        }
        println("Buffer water mark:$audioBufferWatermark")

        // grains.clear();
        // position = audioEvent.getTimeStamp()*1000 - 5000;

        // reset output
        Arrays.fill(outputBuffer, 0f)
        firstGrain()
        var activeGrains = 0
        for (j in grains.indices) {
            if (grains[j].active) {
                activeGrains++
            }
        }
        println("Active grains = $activeGrains")

        // now loop through the buffer
        for (i in 0 until bufferSize) {
            // determine if we need a new grain
            if (timeSinceLastGrain > grainInterval) {
                var firstInactiveGrain: Grain? = null
                for (j in grains.indices) {
                    if (!grains[j].active) {
                        firstInactiveGrain = grains[j]
                        firstInactiveGrain.reset(
                            grainSize.toDouble(),
                            grainRandomness.toDouble(),
                            position,
                            timeStretchFactor.toDouble(),
                            pitchFactor.toDouble()
                        )
                        timeSinceLastGrain = 0f
                        break
                    }
                }
                //System.out.println(grains.size());
            }

            // gather the output from each grain
            for (gi in grains.indices) {
                val g = grains[gi]
                if (g.active) {
                    // calculate value of grain window
                    val windowScale = getValueFraction((g.age / g.grainSize).toFloat())
                    // get position in sample for this grain
                    // get the frame for this grain
                    //if (pitchFactor > ADAPTIVE_INTERP_HIGH_THRESH) {
                    var sampleValue: Double = getFrameNoInterp(g.position).toDouble()
                    //} else if (pitchFactor > ADAPTIVE_INTERP_LOW_THRESH) {
                    //	sampleValue = getFrameLinear(g.position);
                    //} else {
                    //	sampleValue = getFrameCubic(g.position);
                    //}
                    sampleValue *= windowScale
                    outputBuffer[i] += sampleValue.toFloat()
                }
            }
            // increment time
            position += audioSampleLength * timeStretchFactor
            for (gi in grains.indices) {
                val g = grains[gi]
                if (g.active) {
                    calculateNextGrainPosition(g)
                    if (g.age > g.grainSize) {
                        g.active = false
                    }
                }
            }
            timeSinceLastGrain += audioSampleLength.toFloat()
        }
        for (i in 0 until bufferSize) {
            outputBuffer[i] = outputBuffer[i] / 5.0f
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

        //int diff = audioBufferWatermark - frame_floor;
        //if( diff < 4800 || diff > )
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
        val fractionOffset = (frame - realCurrentSample)
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
        var positionInSamples = posInMs / audioSampleLength
        positionInSamples = if (positionInSamples < 0) {
            0.0
        } else {
            val bufferNumber = (positionInSamples / audioBuffer.size).toInt()
            positionInSamples - bufferNumber * audioBuffer.size
        }
        return positionInSamples
    }

    override fun processingFinished() {}

    /**
     * Returns the value of the buffer at the given fraction along its length (0 = start, 1 = end). Uses linear interpolation.
     *
     * @param fraction the point along the buffer to inspect.
     * @return the value at that point.
     */
    fun getValueFraction(fraction: Float): Float {
        var posInBuf = fraction * window.size
        if (fraction >= 1.0f) {
            posInBuf -= 1.0f
        }
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
        g!!.age += audioSampleLength
        g!!.position += direction * audioSampleLength * pitchFactor
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