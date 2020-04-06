package be.tarsos.dsp.io.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat

/**
 * Plays audio from an [be.tarsos.dsp.AudioDispatcher] or [be.tarsos.dsp.AudioGenerator]
 * on an Android [AudioTrack]. This class only supports mono, 16 bit PCM. Depending on your device,
 * some sample rates could not be supported. This class uses the method that writes floats
 * to [android.media.AudioTrack] which is only introduced in Android API Level 21.
 *
 * @author Alex Mikhalev
 * @author Joren Six
 * @see AudioTrack
 */
class AndroidAudioPlayer @JvmOverloads constructor(
    audioFormat: TarsosDSPAudioFormat,
    bufferSizeInSamples: Int = 4096,
    streamType: Int = DEFAULT_STREAM_TYPE
) : AudioProcessor {
    private val audioTrack: AudioTrack

    /**
     * {@inheritDoc}
     */
    override fun process(audioEvent: AudioEvent): Boolean {
        val overlapInSamples = audioEvent.overlap
        val stepSizeInSamples = audioEvent.bufferSize - overlapInSamples
        val byteBuffer = audioEvent.byteBuffer

        //val ret = audioTrack.write(audioEvent.getFloatBuffer(),overlapInSamples,stepSizeInSamples,AudioTrack.WRITE_BLOCKING);
        val ret = audioTrack.write(byteBuffer, overlapInSamples * 2, stepSizeInSamples * 2)
        if (ret < 0) {
            Log.e(
                TAG,
                "AudioTrack.write returned error code $ret"
            )
        }
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun processingFinished() {
        audioTrack.flush()
        audioTrack.stop()
        audioTrack.release()
    }

    companion object {
        /**
         * The default stream type to use.
         */
        const val DEFAULT_STREAM_TYPE = AudioManager.STREAM_MUSIC
        private const val TAG = "AndroidAudioPlayer"
    }
    /**
     * Constructs a new AndroidAudioPlayer from an audio format, default buffer size and stream type.
     *
     * @param audioFormat         The audio format of the stream that this AndroidAudioPlayer will process.
     * This can only be 1 channel, PCM 16 bit.
     * @param bufferSizeInSamples The requested buffer size in samples.
     * @param streamType          The type of audio stream that the internal AudioTrack should use. For
     * example, [AudioManager.STREAM_MUSIC].
     * @throws IllegalArgumentException if audioFormat is not valid or if the requested buffer size is invalid.
     * @see AudioTrack
     */
    /**
     * Constructs a new AndroidAudioPlayer from an audio format.
     *
     * @param audioFormat The audio format that this AndroidAudioPlayer will process.
     * @see AndroidAudioPlayer.AndroidAudioPlayer
     */
    init {
        require(audioFormat.channels == 1) { "TarsosDSP only supports mono audio channel count: " + audioFormat.channels }

        // The requested sample rate
        val sampleRate = audioFormat.sampleRate.toInt()

        //The buffer size in bytes is twice the buffer size expressed in samples if 16bit samples are used:
        val bufferSizeInBytes = bufferSizeInSamples * audioFormat.sampleSizeInBits / 8

        // From the Android API about getMinBufferSize():
        // The total size (in bytes) of the internal buffer where audio data is read from for playback.
        // If track's creation mode is MODE_STREAM, you can write data into this buffer in chunks less than or equal to this size,
        // and it is typical to use chunks of 1/2 of the total size to permit double-buffering. If the track's creation mode is MODE_STATIC,
        // this is the maximum length sample, or audio clip, that can be played by this instance. See getMinBufferSize(int, int, int) to determine
        // the minimum required buffer size for the successful creation of an AudioTrack instance in streaming mode. Using values smaller
        // than getMinBufferSize() will result in an initialization failure.
        val minBufferSizeInBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBufferSizeInBytes <= bufferSizeInBytes) { "The buffer size should be at least " + minBufferSizeInBytes / (audioFormat.sampleSizeInBits / 8) + " (samples) according to  AudioTrack.getMinBufferSize()." }

        audioTrack = AudioTrack.Builder().setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setAudioAttributes(AudioAttributes.Builder()
            .setLegacyStreamType(streamType)
            .build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .build()

        audioTrack.play()
    }
}