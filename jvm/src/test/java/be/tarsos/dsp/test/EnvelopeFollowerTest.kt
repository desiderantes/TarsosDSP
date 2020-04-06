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
package be.tarsos.dsp.test

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.EnvelopeFollower
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFloatArray
import org.junit.jupiter.api.Test
import javax.sound.sampled.UnsupportedAudioFileException

class EnvelopeFollowerTest {
    @Test
    @Throws(UnsupportedAudioFileException::class)
    fun testFollower() {
        val sine = JVMTestUtilities.audioBufferFlute()
        val follower = EnvelopeFollower(44100.0)
        val dispatcher = fromFloatArray(sine, 44100, 1024, 0)
        dispatcher.addAudioProcessor(follower)
        dispatcher.addAudioProcessor(object : AudioProcessor {
            var counter = 0
            override fun process(audioEvent: AudioEvent): Boolean {
                val buffer = audioEvent.floatBuffer
                for (i in buffer.indices) {
                    println(buffer[i].toString() + ";" + sine[counter++])
                }
                return true
            }

            override fun processingFinished() {
                // TODO Auto-generated method stub
            }
        })
        dispatcher.run()
    }
}