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
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import be.tarsos.dsp.onsets.PrintOnsetHandler
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.*

class PercussionOnsetTest {
    @Test
    @Disabled
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun testOnset() {
        /*
		String file = "/home/joren/Desktop/Fingerprinting/07. Pleasant Shadow Song_original.wav.semitone_up.wav";

		AudioFormat format = AudioSystem.getAudioInputStream(new File(file)).getFormat();

		AudioDispatcher dispatcher = AudioDispatcher.fromFile(new File(file),1024,0);
		dispatcher.addAudioProcessor(new PercussionOnsetDetector(format.getSampleRate(),1024, new PercussionHandler() {
			int i = 0 ;
			@Override
			public void handlePercussion(double timestamp) {
				System.out.println(i++ + "\t" + timestamp);

			}
		},44,4));
		dispatcher.run();
		*/
    }

    companion object {
        @Throws(LineUnavailableException::class, UnsupportedAudioFileException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sampleRate = 44100f
            val bufferSize = 1024
            val overlap = 0

            //available mixers
            val selectedMixerIndex = 3
            for ((index, mixer) in AudioSystem.getMixerInfo().withIndex()) {
                println(index.toString() + ": " + Shared.toLocalString(mixer))
            }
            val selectedMixer = AudioSystem.getMixerInfo()[selectedMixerIndex]
            println("Selected mixer: " + Shared.toLocalString(selectedMixer))

            //open a LineWavelet
            val mixer = AudioSystem.getMixer(selectedMixer)
            val format =
                AudioFormat(sampleRate, 16, 1, true, false)
            val dataLineInfo = DataLine.Info(
                TargetDataLine::class.java, format
            )
            val line: TargetDataLine
            line = mixer.getLine(dataLineInfo) as TargetDataLine
            line.open(format, bufferSize)
            line.start()
            val stream = AudioInputStream(line)
            val inputStream = JVMAudioInputStream(stream)
            //create a new dispatcher
            val dispatcher = AudioDispatcher(inputStream, bufferSize, overlap)

            //add a processor, handle percussion event.
            dispatcher.addAudioProcessor(
                PercussionOnsetDetector(
                    sampleRate,
                    bufferSize,
                    overlap,
                    PrintOnsetHandler()
                )
            )

            //run the dispatcher (on the same thread, use start() to run it on another thread).
            dispatcher.run()
        }
    }
}