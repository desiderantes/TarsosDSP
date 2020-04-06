package be.tarsos.dsp.test

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromPipe
import be.tarsos.dsp.pitch.GeneralizedGoertzel
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HammingWindow
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException

class CrossCorrelation(query: FloatArray, private val handler: (Float, Float, Float) -> Unit) :
    AudioProcessor {
    private val zeroPaddedInvesedQuery: FloatArray = FloatArray(query.size * 2)
    private val zeroPaddedData: FloatArray = FloatArray(query.size * 2)
    private val fft: FFT
    var prev = false
    override fun process(audioEvent: AudioEvent): Boolean {
        val fftData = audioEvent.floatBuffer.clone()
        Arrays.fill(zeroPaddedData, 0f)
        System.arraycopy(fftData, 0, zeroPaddedData, fftData.size / 2, fftData.size)
        fft.forwardTransform(zeroPaddedData)
        fft.multiply(zeroPaddedData, zeroPaddedInvesedQuery)
        fft.backwardsTransform(zeroPaddedData)
        var maxVal = -100000f
        var maxIndex = 0
        for (i in zeroPaddedData.indices) {
            if (zeroPaddedData[i] > maxVal) {
                maxVal = zeroPaddedData[i]
                maxIndex = i
            }
        }
        val time =
            (audioEvent.timeStamp - audioEvent.bufferSize / audioEvent.sampleRate + maxIndex / 2 / audioEvent.sampleRate).toFloat()
        handler.invoke(audioEvent.timeStamp.toFloat(), time, maxVal)
        return true
    }

    override fun processingFinished() {}

    companion object {
        var query: FloatArray = FloatArray(0)
        var bufferTime = 0f
        var maxTime = 0f

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        @JvmStatic
        fun main(strings: Array<String>) {
            val q = fromFile(
                File("/home/joren/Desktop/44kHz_1024_samples.wav"),
                1024,
                0
            )
            q.addAudioProcessor(object : AudioProcessor {
                override fun processingFinished() {
                    // TODO Auto-generated method stub
                }

                override fun process(audioEvent: AudioEvent): Boolean {
                    query = audioEvent.floatBuffer.clone()
                    return false
                }
            })
            q.run()
            val potentialMatch: MutableList<Float> =
                ArrayList()
            //ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/sort/1044026.mp3",44100, 1024, 1024-128);
            //ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/ref_other_new.wav",44100, 1024, 1024-128);
            //ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/mixed_clip_09.wav",44100, 1024, 1024-128);
            val ref: AudioDispatcher = fromPipe(
                "/home/joren/Recordings/Clip 12",
                44100,
                1024,
                1024 - 128
            )
            //ref = AudioDispatcherFactory.fromPipe("/home/joren/Desktop/clip_09_amplified.wav",44100, 1024, 1024-128);
            val frequencies = doubleArrayOf(697.0, 941.0, 1209.0)
            val crosscorr = CrossCorrelation(query) { audioBufferTime, maxTime, value ->
                if (value > 500) {
                    bufferTime = audioBufferTime
                    Companion.maxTime = maxTime
                    //System.out.println(maxTime + " " + value);
                }
            }

            val gengoe = GeneralizedGoertzel(
                44100.0f,
                1024,
                frequencies
            ) { time, frequencies, powers, allFrequencies, allPowers ->
                if (powers[0] > 3 && powers[1] > 3 && powers[2] > 3 && powers[0] + powers[1] + powers[2] > 20
                ) {
                    if (time.toFloat() == bufferTime) {
                        //System.out.println(time +  " " + powers[0] + " " + powers[1] + " " + powers[2]);
                        //System.out.println(maxTime);
                        potentialMatch.add(maxTime)
                    }
                }
            }
            ref.addAudioProcessor(SilenceDetector(-45.0, true))
            ref.addAudioProcessor(crosscorr)
            ref.addAudioProcessor(gengoe)
            ref.run()
            val maxMsDifference = 207.5f
            val minDifference = 193.5f
            var minI = -1000f
            for (i in potentialMatch.indices) {
                if (minI < potentialMatch[i]) {
                    val max = potentialMatch[i] + maxMsDifference / 1000.0f
                    val min = potentialMatch[i] + minDifference / 1000.0f
                    for (j in i + 1 until potentialMatch.size) {
                        if (potentialMatch[j] in min..max) {
                            println("Match at: " + potentialMatch[i])
                            minI = max
                            break
                        }
                    }
                }
            }
        }
    }

    init {
        var queryIndex = query.size - 1
        for (i in query.size / 2 until query.size + query.size / 2) {
            zeroPaddedInvesedQuery[i] = query[queryIndex]
            queryIndex--
        }
        fft = FFT(zeroPaddedInvesedQuery.size, HammingWindow())
        fft.forwardTransform(zeroPaddedInvesedQuery)
    }
}