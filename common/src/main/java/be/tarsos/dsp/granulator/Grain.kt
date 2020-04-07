package be.tarsos.dsp.granulator

/**
 * The nested class Grain. Stores information about the start time, current position, age, and grain size of the grain.
 */
internal data class Grain (
    /**
     * The position in millseconds.
     */

    var position: Double = 0.0,

    /**
     * The age of the grain in milliseconds.
     */

    var age: Double = 0.0,

    /**
     * The grain size of the grain. Fixed at instantiation.
     */

    var grainSize: Double = 0.0,
    var active: Boolean = false)
{
    fun reset(
        grainSize: Double,
        randomness: Double,
        position: Double,
        timeStretchFactor: Double,
        pitchShiftFactor: Double
    ) {
        val randomTimeDiff = (if (Math.random() > 0.5) +1 else -1) * grainSize * randomness
        val actualGrainSize = (grainSize + randomTimeDiff) * 1.0 / timeStretchFactor + 1
        this.position = position - actualGrainSize
        age = 0.0
        this.grainSize = actualGrainSize
        active = true
    }
}