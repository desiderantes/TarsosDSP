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
package be.tarsos.dsp.example

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromPipe
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

/**
 * Provides support for different types of command LineWavelet audio feature extraction.
 *
 * @author Joren Six
 */
class FeatureExtractor() : TarsosDSPDemo {
    private val featureExtractors: List<FeatureExtractorApp> = listOf(
        SoundPressureLevelExtractor(),
        PitchExtractor(),
        RootMeanSquareExtractor(),
        OnsetExtractor(),
        BeatExtractor()
    )

    private fun printError() {
        SharedCommandLineUtilities.printPrefix()
        System.err.println("Name:")
        System.err.println("\tTarsosDSP feature extractor")
        SharedCommandLineUtilities.printLine()
        System.err.println("Synopsis:")
        System.err.println("\tjava -jar FeatureExtractor.jar SUB_COMMAND [options...]")
        SharedCommandLineUtilities.printLine()
        System.err.println("Description:")
        System.err.println("\t Extracts features from an audio file, SUB_COMMAND needs\n\tto be one of the following:")
        for (app in featureExtractors) {
            System.err.println("\t\t" + app.name())
        }
    }

    private fun printHelp(appToExecute: FeatureExtractorApp) {
        SharedCommandLineUtilities.printPrefix()
        System.err.println("Name:")
        System.err.println("\tTarsosDSP " + appToExecute.name() + " feature extractor")
        SharedCommandLineUtilities.printLine()
        System.err.println("Synopsis:")
        System.err.println("\tjava -jar FeatureExtractor.jar " + appToExecute.name() + " " + appToExecute.synopsis())
        SharedCommandLineUtilities.printLine()
        System.err.println("Description:")
        System.err.println(appToExecute.description())
    }

    override val name: String
        get() = "Feature Extractor"

    override val description: String
        get() = "Extracts features from an audio file, SUB_COMMAND needs" +
                "to be one of the following:" + featureExtractors.joinToString { it.name() }

    override fun start(vararg arguments: String) {
        if (arguments.isEmpty()) {
            printError()
        } else {
            val subCommand = arguments[0].toLowerCase()
            var appToExecute: FeatureExtractorApp? = null
            for (app in featureExtractors) {
                if (subCommand.equals(app.name(), ignoreCase = true)) {
                    appToExecute = app
                }
            }
            if (appToExecute == null) {
                printError()
            } else {
                try {
                    if (!appToExecute.run(*arguments)) {
                        printHelp(appToExecute)
                    }
                } catch (e: UnsupportedAudioFileException) {
                    printHelp(appToExecute)
                    SharedCommandLineUtilities.printLine()
                    System.err.println("Error:")
                    System.err.println("\tThe audio file is not supported!")
                } catch (e: IOException) {
                    printHelp(appToExecute)
                    SharedCommandLineUtilities.printLine()
                    System.err.println("Current error:")
                    System.err.println("\tIO error, maybe the audio file is not found or not supported!")
                }
            }
        }
    }

    private interface FeatureExtractorApp {
        fun name(): String
        fun description(): String
        fun synopsis(): String

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        fun run(vararg args: String): Boolean
    }

    private inner class RootMeanSquareExtractor : FeatureExtractorApp {
        override fun name(): String {
            return "rms"
        }

        override fun description(): String {
            return "\tCalculates the root mean square of an audio signal for each \n\tblock of 2048 samples. The output gives you a timestamp and the RMS value,\n\tSeparated by a semicolon.\n\n\t\n\ninput.wav: a\treadable audio file."
        }

        override fun synopsis(): String {
            return "input.wav"
        }

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        override fun run(vararg args: String): Boolean {
            if (args.size != 2) {
                return false
            }
            val inputFile = args[1]
            val audioFile = File(inputFile)
            val size = 2048
            val overlap = 0
            val dispatcher =
                fromFile(audioFile, size, overlap)
            dispatcher.addAudioProcessor(object : AudioProcessor {
                override fun processingFinished() {}
                override fun process(audioEvent: AudioEvent): Boolean {
                    println(audioEvent.timeStamp.toString() + "," + audioEvent.rms)
                    return true
                }
            })
            dispatcher.run()
            return true
        }
    }

    private inner class SoundPressureLevelExtractor : FeatureExtractorApp {
        override fun name(): String {
            return "sound_pressure_level"
        }

        override fun description(): String {
            return "\tCalculates a sound pressure level in dB for each\n\tblock of 2048 samples.The output gives you a timestamp and a value in dBSPL.\n\tSeparated by a semicolon.\n\n\t\n\nWith input.wav\ta readable audio file."
        }

        override fun synopsis(): String {
            return "input.wav"
        }

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        override fun run(vararg args: String): Boolean {
            if (args.size != 2) {
                return false
            }
            val inputFile = args[1]
            val audioFile = File(inputFile)
            val size = 2048
            val overlap = 0
            val silenceDetecor = SilenceDetector()
            val dispatcher =
                fromFile(audioFile, size, overlap)
            dispatcher.addAudioProcessor(silenceDetecor)
            dispatcher.addAudioProcessor(object : AudioProcessor {
                override fun processingFinished() {}
                override fun process(audioEvent: AudioEvent): Boolean {
                    println(
                        audioEvent.timeStamp.toString() + "," + silenceDetecor.currentSPL
                    )
                    return true
                }
            })
            dispatcher.run()
            return true
        }
    }

    private inner class PitchExtractor : FeatureExtractorApp, PitchDetectionHandler {
        override fun name(): String {
            return "pitch"
        }

        override fun description(): String {
            var descr =
                "\tCalculates pitch in Hz for each block of 2048 samples. \n\tThe output is a semicolon separated list of a timestamp, frequency in hertz and \n\ta probability which describes how pitched the sound is at the given time. "
            descr += "\n\n\tinput.wav\t\ta readable wav file."
            descr += "\n\t--detector DETECTOR\tdefaults to FFT_YIN or one of these:\n\t\t\t\t"
            for (algo in PitchEstimationAlgorithm.values()) {
                descr += """${algo.name}
				"""
            }
            return descr
        }

        override fun synopsis(): String {
            return "[--detector DETECTOR] input.wav"
        }

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        override fun run(vararg args: String): Boolean {
            var algo = PitchEstimationAlgorithm.FFT_YIN
            var inputFile = args[1]
            if (args.size == 1 || args.size == 3) {
                return false
            } else if (args.size == 4 && !args[1].equals("--detector", ignoreCase = true)) {
                return false
            } else if (args.size == 4 && args[1].equals("--detector", ignoreCase = true)) {
                try {
                    algo = PitchEstimationAlgorithm.valueOf(args[2].toUpperCase())
                    inputFile = args[3]
                } catch (e: IllegalArgumentException) {
                    //if enum value string is not recognized
                    return false
                }
            }
            val audioFile = File(inputFile)
            val samplerate =
                AudioSystem.getAudioFileFormat(audioFile).format.sampleRate
            val size = 1024
            val overlap = 0
            val dispatcher =
                fromFile(audioFile, size, overlap)
            dispatcher.addAudioProcessor(PitchProcessor(algo, samplerate, size, this))
            dispatcher.run()
            return true
        }

        override fun handlePitch(
            pitchDetectionResult: PitchDetectionResult,
            audioEvent: AudioEvent
        ) {
            val timeStamp = audioEvent.timeStamp
            val pitch = pitchDetectionResult.pitch
            val probability = pitchDetectionResult.probability
            println("$timeStamp,$pitch,$probability")
        }
    }

    private inner class OnsetExtractor : FeatureExtractorApp, OnsetHandler {
        override fun name(): String {
            return "onset"
        }

        override fun description(): String {
            var descr =
                """	Calculates onsets using a complex domain onset detector.
	The output is a semicolon separated list of a timestamp, and a salliance. """
            descr += "\n\n\tinput.wav\t\ta readable wav file."
            descr += ""
            return descr
        }

        override fun synopsis(): String {
            return "input.wav"
        }

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        override fun run(vararg args: String): Boolean {
            val inputFile = args[1]
            val audioFile = File(inputFile)
            val size = 512
            val overlap = 256
            val dispatcher = fromPipe(
                audioFile.absolutePath,
                44100,
                size,
                overlap
            )
            val detector = ComplexOnsetDetector(size, 0.7, 0.1)
            detector.setHandler(this)
            dispatcher.addAudioProcessor(detector)
            dispatcher.run()
            return true
        }

        override fun handleOnset(time: Double, salience: Double) {
            println("$time,$salience")
        }
    }

    private inner class BeatExtractor : FeatureExtractorApp, OnsetHandler {
        override fun name(): String {
            return "beat"
        }

        override fun description(): String {
            var descr =
                """	Calculates onsets using a complex domain onset detector.
	The output is a semicolon separated list of a timestamp, and a salliance. """
            descr += "\n\n\tinput.wav\t\ta readable wav file."
            descr += ""
            return descr
        }

        override fun synopsis(): String {
            return "input.wav"
        }

        @Throws(UnsupportedAudioFileException::class, IOException::class)
        override fun run(vararg args: String): Boolean {
            val inputFile = args[1]
            val audioFile = File(inputFile)
            val size = 512
            val overlap = 256
            val dispatcher =
                fromFile(audioFile, size, overlap)
            val detector = ComplexOnsetDetector(size)
            val handler = BeatRootOnsetEventHandler()
            detector.setHandler(handler)
            dispatcher.addAudioProcessor(detector)
            dispatcher.run()
            handler.trackBeats(this)
            return true
        }

        override fun handleOnset(time: Double, salience: Double) {
            println(time)
        }
    }

    companion object {
        /**
         * @param arguments
         */
        @JvmStatic
        fun main(arguments: Array<String>) {
            FeatureExtractor().start(*arguments)
        }
    }
}