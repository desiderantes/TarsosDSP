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

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * An audio event flows through the processing pipeline. The object is reused for performance reasons.
 * The arrays with audio information are also reused, so watch out when using the buffer getter and setters.
 *
 * @author Joren Six
 */
class AudioEvent @JvmOverloads constructor(
    /**
     * The format specifies a particular arrangement of data in a sound stream.
     */
    private val format: TarsosDSPAudioFormat,
    /**
     * The audio data encoded in floats from -1.0 to 1.0.
     */
    var floatBuffer: FloatArray,
    /**
     * The overlap in samples.
     */
    var overlap: Int = 0
) {
    private val converter: TarsosDSPAudioFloatConverter = TarsosDSPAudioFloatConverter.getConverter(format)!!

    /**
     * The audio data encoded in bytes according to format.
     */
    private var byteBuffer: ByteArray? = null

    /**
     * Return a byte array with the audio data in bytes.
     * A conversion is done from float, cache accordingly on the other side...
     *
     * @return a byte array with the audio data in bytes.
     */
    fun getByteBuffer(): ByteArray {
        val length = floatBuffer.size * format.frameSize
        return (if (byteBuffer == null || byteBuffer!!.size != length) {
            ByteArray(length)
        } else byteBuffer!!).also { converter.toByteArray(floatBuffer, it) }

    }

    /**
     * @return The length of the stream, expressed in sample frames rather than bytes
     */
    /**
     * The length of the stream, expressed in sample frames rather than bytes
     */
    val frameLength: Long = 0

    /**
     * The number of bytes processed before this event. It can be used to calculate the time stamp for when this event started.
     */
    private var bytesProcessed: Long = 0
    private var bytesProcessing = 0
    val sampleRate: Float
        get() = format.sampleRate

    val bufferSize: Int
        get() = floatBuffer.size

    fun setBytesProcessed(bytesProcessed: Long) {
        this.bytesProcessed = bytesProcessed
    }

    /**
     * Calculates and returns the time stamp at the beginning of this audio event.
     *
     * @return The time stamp at the beginning of the event in seconds.
     */
    val timeStamp: Double
        get() = (bytesProcessed / format.frameSize / format.sampleRate).toDouble()

    val endTimeStamp: Double
        get() = ((bytesProcessed + bytesProcessing) / format.frameSize / format.sampleRate).toDouble()

    val samplesProcessed: Long
        get() = bytesProcessed / format.frameSize

    /**
     * Calculate the progress in percentage of the total number of frames.
     *
     * @return a percentage of processed frames or a negative number if the
     * number of frames is not known beforehand.
     */
    val progress: Double
        get() = bytesProcessed / format.frameSize / frameLength.toDouble()

    /**
     * Calculates and returns the root mean square of the signal. Please
     * cache the result since it is calculated every time.
     *
     * @return The [RMS](http://en.wikipedia.org/wiki/Root_mean_square) of
     * the signal present in the current buffer.
     */
    val rMS: Double
        get() = calculateRMS(floatBuffer)

    /**
     * Returns the dBSPL for a buffer.
     *
     * @return The dBSPL level for the buffer.
     */
    val dBSPL: Double
        get() {
            return soundPressureLevel(floatBuffer)
        }

    fun clearFloatBuffer() {
        Arrays.fill(floatBuffer, 0f)
    }

    fun isSilence(silenceThreshold: Double): Boolean {
        return soundPressureLevel(floatBuffer) < silenceThreshold
    }

    fun setBytesProcessing(bytesProcessing: Int) {
        this.bytesProcessing = bytesProcessing
    }

    companion object {
        /**
         * Calculates and returns the root mean square of the signal. Please
         * cache the result since it is calculated every time.
         *
         * @param floatBuffer The audio buffer to calculate the RMS for.
         * @return The [RMS](http://en.wikipedia.org/wiki/Root_mean_square) of
         * the signal present in the current buffer.
         */
        fun calculateRMS(floatBuffer: FloatArray): Double {
            var rms = 0.0
            for (i in floatBuffer.indices) {
                rms += floatBuffer[i] * floatBuffer[i].toDouble()
            }
            rms /= floatBuffer.size.toDouble()
            rms = sqrt(rms)
            return rms
        }

        /**
         * Returns the dBSPL for a buffer.
         *
         * @param buffer The buffer with audio information.
         * @return The dBSPL level for the buffer.
         */
        private fun soundPressureLevel(buffer: FloatArray): Double {
            val rms = calculateRMS(buffer)
            return linearToDecibel(rms)
        }

        /**
         * Converts a linear to a dB value.
         *
         * @param value The value to convert.
         * @return The converted value.
         */
        private fun linearToDecibel(value: Double): Double {
            return 20.0 * log10(value)
        }
    }
}