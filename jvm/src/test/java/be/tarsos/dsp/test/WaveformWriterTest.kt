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

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.io.jvm.WaveformWriter
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import javax.sound.sampled.*

class WaveformWriterTest {
    @Disabled
    @Test
    @Throws(
        UnsupportedAudioFileException::class,
        InterruptedException::class,
        LineUnavailableException::class,
        FileNotFoundException::class
    )
    fun testSilenceWriter() {
        val sampleRate = 44100f
        val bufferSize = 1024
        val overlap = 0

        // available mixers
        val selectedMixerIndex = 4
        for ((index, mixer) in AudioSystem.getMixerInfo().withIndex()) {
            println(index.toString() + ": " + Shared.toLocalString(mixer))
        }
        val selectedMixer = AudioSystem.getMixerInfo()[selectedMixerIndex]
        println("Selected mixer: " + Shared.toLocalString(selectedMixer))

        // open a LineWavelet
        val mixer = AudioSystem.getMixer(selectedMixer)
        val format = AudioFormat(sampleRate, 16, 1, true, true)
        val dataLineInfo = DataLine.Info(
            TargetDataLine::class.java, format
        )
        val line: TargetDataLine
        line = mixer.getLine(dataLineInfo) as TargetDataLine
        line.open(format, bufferSize)
        line.start()
        val stream = AudioInputStream(line)
        val inpustStream = JVMAudioInputStream(stream)
        // create a new dispatcher
        val dispatcher = AudioDispatcher(
            inpustStream, bufferSize,
            overlap
        )
        var writer = WaveformWriter(format, "01.file.wav")
        // add a processor, handle percussion event.
        dispatcher.addAudioProcessor(SilenceDetector())
        dispatcher.addAudioProcessor(writer)

        // run the dispatcher (on the same thread, use start() to run it on
        // another thread).
        Thread(dispatcher).start()
        Thread.sleep(3000)
        dispatcher.removeAudioProcessor(writer)
        writer = WaveformWriter(format, "02.file.wav")
        dispatcher.addAudioProcessor(writer)
        Thread.sleep(3000)
        dispatcher.stop()
    }
}