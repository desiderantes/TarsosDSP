package be.tarsos.dsp

import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromPipe
import be.tarsos.dsp.io.jvm.AudioPlayer
import javax.sound.sampled.LineUnavailableException
import kotlin.concurrent.thread
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.asJavaRandom

class AmplitudeModulatedNoise : AudioProcessor {
    var rnd = Random.asJavaRandom()
    var dry = 0.7f
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioBuffer = audioEvent.floatBuffer
        var max = 0f
        for (v in audioBuffer) {
            max = v.coerceAtLeast(max)
        }
        val noiseBuffer = FloatArray(audioBuffer.size)
        for (i in audioBuffer.indices) {
            if (rnd.nextBoolean()) noiseBuffer[i] =
                (rnd.nextGaussian() * max).toFloat() else noiseBuffer[i] =
                (rnd.nextGaussian() * max * -1).toFloat()
        }
        val stdDevNoise = standardDeviation(noiseBuffer)
        val stdDevAudio = standardDeviation(audioBuffer)
        for (i in audioBuffer.indices) {
            audioBuffer[i] = audioBuffer[i] / stdDevNoise * stdDevAudio
        }
        for (i in audioBuffer.indices) {
            audioBuffer[i] = audioBuffer[i] * dry + noiseBuffer[i] * (1.0f - dry)
        }
        return true
    }

    private fun standardDeviation(data: FloatArray): Float {
        var sum = 0f
        for (v in data) {
            sum += v
        }
        val mean = sum / data.size.toFloat()
        sum = 0f
        for (datum in data) {
            sum += (datum - mean) * (datum - mean)
        }
        val variance = sum / (data.size - 1).toFloat()
        return sqrt(variance.toDouble()).toFloat()
    }

    override fun processingFinished() {}

    companion object {
        @Throws(LineUnavailableException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val file = "/home/joren/Desktop/parklife/Alles_Is_Op.wav"
            val d = fromPipe(file, 44100, 1024, 0)
            val noise = AmplitudeModulatedNoise()
            d.addAudioProcessor(noise)
            d.addAudioProcessor(AudioPlayer(d.format))
            thread {
                var i = 0f
                while (i < 1.10) {
                    noise.dry = i
                    println(i)
                    try {
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                    i += 0.1f
                }
            }.start()
            d.run()
        }
    }
}