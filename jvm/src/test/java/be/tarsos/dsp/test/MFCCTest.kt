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
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFloatArray
import be.tarsos.dsp.mfcc.MFCC
import org.junit.jupiter.api.Test
import javax.sound.sampled.UnsupportedAudioFileException

class MFCCTest {
    //	private static int counter = 0;
    @Test
    @Throws(UnsupportedAudioFileException::class)
    fun mFCCForSineTest() {
        val sampleRate = 44100
        val bufferSize = 1024
        val bufferOverlap = 128
        val floatBuffer = TestUtilities.audioBufferSine()
        val dispatcher = fromFloatArray(floatBuffer, sampleRate, bufferSize, bufferOverlap)
        val mfcc = MFCC(bufferSize, sampleRate.toFloat(), 40, 50, 300F, 3000F)
        dispatcher.addAudioProcessor(mfcc)
        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun processingFinished() {}
            override fun process(audioEvent: AudioEvent): Boolean {
                return true
            }
        })
        dispatcher.run()
    }
}