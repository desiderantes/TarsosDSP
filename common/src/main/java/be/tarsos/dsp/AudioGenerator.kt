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

import be.tarsos.dsp.AudioGenerator
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors. This class can be used to feed FFT's, pitch detectors, audio players, ...
 * Using a (blocking) audio player it is even possible to synchronize execution of
 * AudioProcessors and sound. This behavior can be used for visualization.
 *
 * @author Joren Six
 */
class AudioGenerator @JvmOverloads constructor(audioBufferSize: Int, bufferOverlap: Int, samplerate: Int = 44100) :
    Runnable {
    /**
     * A list of registered audio processors. The audio processors are
     * responsible for actually doing the digital signal processing
     */
    private val audioProcessors: MutableList<AudioProcessor> = CopyOnWriteArrayList()
    val format: TarsosDSPAudioFormat

    /**
     * This buffer is reused again and again to store audio data using the float
     * data type.
     */
    private lateinit var audioFloatBuffer: FloatArray

    /**
     * The floatOverlap: the number of elements that are copied in the buffer
     * from the previous buffer. Overlap should be smaller (strict) than the
     * buffer size and can be zero. Defined in number of samples.
     */
    private var floatOverlap = 0
    private var floatStepSize = 0
    private var samplesProcessed: Int

    /**
     * The audio event that is send through the processing chain.
     */
    private val audioEvent: AudioEvent

    /**
     * If true the dispatcher stops dispatching audio.
     */
    private var stopped: Boolean

    /**
     * Constructs the target audio format. The audio format is one channel
     * signed PCM of a given sample rate.
     *
     * @param targetSampleRate The sample rate to convert to.
     * @return The audio format after conversion.
     */
    private fun getTargetAudioFormat(targetSampleRate: Int): TarsosDSPAudioFormat {
        return TarsosDSPAudioFormat(
            TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
            targetSampleRate.toFloat(),
            2 * 8,
            1,
            2 * 1,
            targetSampleRate.toFloat(), ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
    }

    /**
     * Set a new step size and overlap size. Both in number of samples. Watch
     * out with this method: it should be called after a batch of samples is
     * processed, not during.
     *
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     * in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     * AudioBufferSize is common (512, 1024) for an FFT.
     */
    fun setStepSizeAndOverlap(audioBufferSize: Int, bufferOverlap: Int) {
        audioFloatBuffer = FloatArray(audioBufferSize)
        floatOverlap = bufferOverlap
        floatStepSize = audioFloatBuffer.size - floatOverlap
    }

    /**
     * Adds an AudioProcessor to the chain of processors.
     *
     * @param audioProcessor The AudioProcessor to add.
     */
    fun addAudioProcessor(audioProcessor: AudioProcessor) {
        audioProcessors.add(audioProcessor)
        LOG.fine("Added an audioprocessor to the list of processors: $audioProcessor")
    }

    /**
     * Removes an AudioProcessor to the chain of processors and calls processingFinished.
     *
     * @param audioProcessor The AudioProcessor to remove.
     */
    fun removeAudioProcessor(audioProcessor: AudioProcessor) {
        audioProcessors.remove(audioProcessor)
        audioProcessor.processingFinished()
        LOG.fine("Remove an audioprocessor to the list of processors: $audioProcessor")
    }

    override fun run() {


        //Read the first (and in some cases last) audio block.
        generateNextAudioBlock()


        // As long as the stream has not ended
        while (!stopped) {

            //Makes sure the right buffers are processed, they can be changed by audio processors.
            for (processor in audioProcessors) {
                if (!processor.process(audioEvent)) {
                    //skip to the next audio processors if false is returned.
                    break
                }
            }
            if (!stopped) {
                audioEvent.setBytesProcessed(samplesProcessed * format.frameSize.toLong())

                // Read, convert and process consecutive overlapping buffers.
                // Slide the buffer.
                generateNextAudioBlock()
            }
        }

        // Notify all processors that no more data is available.
        // when stop() is called processingFinished is called explicitly, no need to do this again.
        // The explicit call is to prevent timing issues.
        if (!stopped) {
            stop()
        }
    }

    /**
     * Stops dispatching audio data.
     */
    fun stop() {
        stopped = true
        for (processor in audioProcessors) {
            processor.processingFinished()
        }
    }

    /**
     * Reads the next audio block. It tries to read the number of bytes defined
     * by the audio buffer size minus the overlap. If the expected number of
     * bytes could not be read either the end of the stream is reached or
     * something went wrong.
     *
     *
     * The behavior for the first and last buffer is defined by their corresponding the zero pad settings. The method also handles the case if
     * the first buffer is also the last.
     */
    private fun generateNextAudioBlock() {
        assert(floatOverlap < audioFloatBuffer.size)

        //Shift the audio information using array copy since it is probably faster than manually shifting it.
        // No need to do this on the first buffer
        if (audioFloatBuffer.size == floatOverlap + floatStepSize) {
            System.arraycopy(audioFloatBuffer, floatStepSize, audioFloatBuffer, 0, floatOverlap)
        }
        samplesProcessed += floatStepSize
    }

    fun resetTime() {
        samplesProcessed = 0
    }

    /**
     * @return The currently processed number of seconds.
     */
    fun secondsProcessed(): Float {
        return samplesProcessed / format.sampleRate / format.channels
    }

    companion object {
        /**
         * Log messages.
         */
        private val LOG =
            Logger.getLogger(AudioGenerator::class.java.name)
    }

    /**
     * Create a new generator.
     *
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     * in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     * AudioBufferSize is common (512, 1024) for an FFT.
     */
    init {

        format = getTargetAudioFormat(samplerate)
        setStepSizeAndOverlap(audioBufferSize, bufferOverlap)
        audioEvent = AudioEvent(format, audioFloatBuffer)
        stopped = false
        samplesProcessed = 0
    }
}