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
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter.Companion.getConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.roundToLong

/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors. This class can be used to feed FFT's, pitch detectors, audio players, ...
 * Using a (blocking) audio player it is even possible to synchronize execution of
 * AudioProcessors and sound. This behavior can be used for visualization.
 *
 * @author Joren Six
 */
class AudioDispatcher
/**
 * Create a new dispatcher from a stream.
 *
 * @param stream          The stream to read data from.
 * @param audioBufferSize The size of the buffer defines how much samples are processed
 * in one step. Common values are 1024,2048.
 * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
 * AudioBufferSize is common (512, 1024) for an FFT.
 */
constructor(
    /**
     * The audio stream (in bytes), conversion to float happens at the last
     * moment.
     */
    private val audioInputStream: TarsosDSPAudioInputStream,
    audioBufferSize: Int,
    bufferOverlap: Int
) :
    Runnable {

    /**
     * A list of registered audio processors. The audio processors are
     * responsible for actually doing the digital signal processing
     */

    // The copy on write list allows concurrent modification of the list while
    // it is iterated. A nice feature to have when adding AudioProcessors while
    // the AudioDispatcher is running.
    private val audioProcessors: MutableList<AudioProcessor> = CopyOnWriteArrayList()

    /**
     * Converter converts an array of floats to an array of bytes (and vice
     * versa).
     */
    private val converter: TarsosDSPAudioFloatConverter?
    val format: TarsosDSPAudioFormat = audioInputStream.format

    /**
     * This buffer is reused again and again to store audio data using the float
     * data type.
     */
    private lateinit var audioFloatBuffer: FloatArray

    /**
     * This buffer is reused again and again to store audio data using the byte
     * data type.
     */
    private lateinit var audioByteBuffer: ByteArray

    /**
     * The floatOverlap: the number of elements that are copied in the buffer
     * from the previous buffer. Overlap should be smaller (strict) than the
     * buffer size and can be zero. Defined in number of samples.
     */
    private var floatOverlap = 0
    private var floatStepSize = 0

    /**
     * The overlap and stepsize defined not in samples but in bytes. So it
     * depends on the bit depth. Since the int datatype is used only 8,16,24,...
     * bits or 1,2,3,... bytes are supported.
     */
    private var byteOverlap = 0
    private var byteStepSize = 0

    /**
     * The number of bytes to skip before processing starts.
     */
    private var bytesToSkip: Long = 0

    /**
     * Position in the stream in bytes. e.g. if 44100 bytes are processed and 16
     * bits per frame are used then you are 0.5 seconds into the stream.
     */
    private var bytesProcessed: Long = 0

    /**
     * The audio event that is send through the processing chain.
     */
    private val audioEvent: AudioEvent

    /**
     * @return True if the dispatcher is stopped or the end of stream has been reached.
     */
    /**
     * If true the dispatcher stops dispatching audio.
     */
    var isStopped: Boolean = false
        private set

    /**
     * If true then the first buffer is only filled up to buffer size - hop size
     * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000 times
     * zero 0 and 48 actual audio samples. During the next iteration you get
     * mostly zeros and 96 samples.
     */
    private var zeroPadFirstBuffer = false

    /**
     * If true then the last buffer is zero padded. Otherwise the buffer is
     * shortened to the remaining number of samples. If false then the audio
     * processors must be prepared to handle shorter audio buffers.
     */
    private var zeroPadLastBuffer: Boolean = true

    /**
     * Skip a number of seconds before processing the stream.
     *
     * @param seconds
     */
    fun skip(seconds: Double) {
        bytesToSkip = (seconds * format.sampleRate).roundToLong() * format.frameSize
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
        audioByteBuffer = ByteArray(audioFloatBuffer.size * format.frameSize)
        byteOverlap = floatOverlap * format.frameSize
        byteStepSize = floatStepSize * format.frameSize
    }

    /**
     * if zero pad is true then the first buffer is only filled up to  buffer size - hop size
     * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000x0 and 48 filled audio samples
     *
     * @param zeroPadFirstBuffer true if the buffer should be zeroPadFirstBuffer, false otherwise.
     */
    fun setZeroPadFirstBuffer(zeroPadFirstBuffer: Boolean) {
        this.zeroPadFirstBuffer = zeroPadFirstBuffer
    }

    /**
     * If zero pad last buffer is true then the last buffer is filled with zeros until the normal amount
     * of elements are present in the buffer. Otherwise the buffer only contains the last elements and no zeros.
     * By default it is set to true.
     *
     * @param zeroPadLastBuffer
     */
    fun setZeroPadLastBuffer(zeroPadLastBuffer: Boolean) {
        this.zeroPadLastBuffer = zeroPadLastBuffer
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
     * Removes an AudioProcessor to the chain of processors and calls its `processingFinished` method.
     *
     * @param audioProcessor The AudioProcessor to remove.
     */
    fun removeAudioProcessor(audioProcessor: AudioProcessor) {
        audioProcessors.remove(audioProcessor)
        audioProcessor.processingFinished()
        LOG.fine("Remove an audioprocessor to the list of processors: $audioProcessor")
    }

    override fun run() {
        var bytesRead = 0
        if (bytesToSkip != 0L) {
            skipToStart()
        }

        //Read the first (and in some cases last) audio block.
        bytesRead = try {
            //needed to get correct time info when skipping first x seconds
            audioEvent.setBytesProcessed(bytesProcessed)
            readNextAudioBlock()
        } catch (e: IOException) {
            val message = "Error while reading audio input stream: " + e.message
            LOG.warning(message)
            throw Error(message)
        }

        // As long as the stream has not ended
        while (bytesRead != 0 && !isStopped) {

            //Makes sure the right buffers are processed, they can be changed by audio processors.
            for (processor in audioProcessors) {
                if (!processor.process(audioEvent)) {
                    //skip to the next audio processors if false is returned.
                    break
                }
            }
            if (!isStopped) {
                //Update the number of bytes processed;
                bytesProcessed += bytesRead.toLong()
                audioEvent.setBytesProcessed(bytesProcessed)

                // Read, convert and process consecutive overlapping buffers.
                // Slide the buffer.
                try {
                    bytesRead = readNextAudioBlock()
                    audioEvent.overlap = floatOverlap
                } catch (e: IOException) {
                    val message = "Error while reading audio input stream: " + e.message
                    LOG.warning(message)
                    throw Error(message)
                }
            }
        }

        // Notify all processors that no more data is available.
        // when stop() is called processingFinished is called explicitly, no need to do this again.
        // The explicit call is to prevent timing issues.
        if (!isStopped) {
            stop()
        }
    }

    private fun skipToStart() {
        var skipped = 0L
        try {
            skipped = audioInputStream.skip(bytesToSkip)
            if (skipped != bytesToSkip) {
                throw IOException()
            }
            bytesProcessed += bytesToSkip
        } catch (e: IOException) {
            val message = String.format(
                "Did not skip the expected amount of bytes,  %d skipped, %d expected!",
                skipped,
                bytesToSkip
            )
            LOG.warning(message)
            throw Error(message)
        }
    }

    /**
     * Stops dispatching audio data.
     */
    fun stop() {
        isStopped = true
        for (processor in audioProcessors) {
            processor.processingFinished()
        }
        try {
            audioInputStream.close()
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, "Closing audio stream error.", e)
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
     *
     * @return The number of bytes read.
     * @throws IOException When something goes wrong while reading the stream. In
     * particular, an IOException is thrown if the input stream has
     * been closed.
     */
    @Throws(IOException::class)
    private fun readNextAudioBlock(): Int {
        assert(floatOverlap < audioFloatBuffer.size)

        // Is this the first buffer?
        val isFirstBuffer = bytesProcessed == 0L || bytesProcessed == bytesToSkip
        val offsetInBytes: Int
        val offsetInSamples: Int
        val bytesToRead: Int
        //Determine the amount of bytes to read from the stream
        if (isFirstBuffer && !zeroPadFirstBuffer) {
            //If this is the first buffer and we do not want to zero pad the
            //first buffer then read a full buffer
            bytesToRead = audioByteBuffer.size
            // With an offset in bytes of zero;
            offsetInBytes = 0
            offsetInSamples = 0
        } else {
            //In all other cases read the amount of bytes defined by the step size
            bytesToRead = byteStepSize
            offsetInBytes = byteOverlap
            offsetInSamples = floatOverlap
        }

        //Shift the audio information using array copy since it is probably faster than manually shifting it.
        // No need to do this on the first buffer
        if (!isFirstBuffer && audioFloatBuffer.size == floatOverlap + floatStepSize) {
            System.arraycopy(audioFloatBuffer, floatStepSize, audioFloatBuffer, 0, floatOverlap)
            /*
			for(int i = floatStepSize ; i < floatStepSize+floatOverlap ; i++){
				audioFloatBuffer[i-floatStepSize] = audioFloatBuffer[i];
			}*/
        }

        // Total amount of bytes read
        var totalBytesRead = 0

        // The amount of bytes read from the stream during one iteration.
        var bytesRead = 0

        // Is the end of the stream reached?
        var endOfStream = false

        // Always try to read the 'bytesToRead' amount of bytes.
        // unless the stream is closed (stopped is true) or no bytes could be read during one iteration
        while (!isStopped && !endOfStream && totalBytesRead < bytesToRead) {
            bytesRead = try {
                audioInputStream.read(audioByteBuffer, offsetInBytes + totalBytesRead, bytesToRead - totalBytesRead)
            } catch (e: IndexOutOfBoundsException) {
                // The pipe decoder generates an out of bounds if end
                // of stream is reached. Ugly hack...
                -1
            }
            if (bytesRead == -1) {
                // The end of the stream is reached if the number of bytes read during this iteration equals -1
                endOfStream = true
            } else {
                // Otherwise add the number of bytes read to the total
                totalBytesRead += bytesRead
            }
        }
        if (endOfStream) {
            // Could not read a full buffer from the stream, there are two options:
            if (zeroPadLastBuffer) {
                //Make sure the last buffer has the same length as all other buffers and pad with zeros
                for (i in offsetInBytes + totalBytesRead until audioByteBuffer.size) {
                    audioByteBuffer[i] = 0
                }
                converter!!.toFloatArray(
                    audioByteBuffer,
                    offsetInBytes,
                    audioFloatBuffer,
                    offsetInSamples,
                    floatStepSize
                )
            } else {
                // Send a smaller buffer through the chain.
                val audioByteBufferContent = audioByteBuffer
                audioByteBuffer = ByteArray(offsetInBytes + totalBytesRead)
                for (i in audioByteBuffer.indices) {
                    audioByteBuffer[i] = audioByteBufferContent[i]
                }
                val totalSamplesRead = totalBytesRead / format.frameSize
                audioFloatBuffer = FloatArray(offsetInSamples + totalBytesRead / format.frameSize)
                converter!!.toFloatArray(
                    audioByteBuffer,
                    offsetInBytes,
                    audioFloatBuffer,
                    offsetInSamples,
                    totalSamplesRead
                )
            }
        } else if (bytesToRead == totalBytesRead) {
            // The expected amount of bytes have been read from the stream.
            if (isFirstBuffer && !zeroPadFirstBuffer) {
                converter!!.toFloatArray(audioByteBuffer, 0, audioFloatBuffer, 0, audioFloatBuffer.size)
            } else {
                converter!!.toFloatArray(
                    audioByteBuffer,
                    offsetInBytes,
                    audioFloatBuffer,
                    offsetInSamples,
                    floatStepSize
                )
            }
        } else if (!isStopped) {
            // If the end of the stream has not been reached and the number of bytes read is not the
            // expected amount of bytes, then we are in an invalid state;
            throw IOException(
                String.format(
                    "The end of the audio stream has not been reached and the number of bytes read (%d) is not equal "
                            + "to the expected amount of bytes(%d).", totalBytesRead, bytesToRead
                )
            )
        }


        // Makes sure AudioEvent contains correct info.
        audioEvent.floatBuffer = audioFloatBuffer
        audioEvent.overlap = offsetInSamples
        return totalBytesRead
    }

    /**
     * @return The currently processed number of seconds.
     */
    fun secondsProcessed(): Float {
        return bytesProcessed / (format.sampleSizeInBits / 8) / format.sampleRate / format.channels
    }

    fun setAudioFloatBuffer(audioBuffer: FloatArray) {
        audioFloatBuffer = audioBuffer
    }

    companion object {
        /**
         * Log messages.
         */
        private val LOG =
            Logger.getLogger(AudioDispatcher::class.java.name)
    }


    init {
        setStepSizeAndOverlap(audioBufferSize, bufferOverlap)
        audioEvent = AudioEvent(format, audioFloatBuffer, bufferOverlap)
        converter = getConverter(format)
    }
}