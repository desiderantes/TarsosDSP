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

import be.tarsos.dsp.util.PI
import java.io.*
import kotlin.math.sin

object TestUtilities {
    /**
     * Constructs and returns a buffer of a two seconds long pure sine of 440Hz
     * sampled at 44.1kHz.
     *
     * @return A buffer of a two seconds long pure sine (440Hz) sampled at
     * 44.1kHz.
     */
    fun audioBufferSine(numberOfSamples: Int = 4 * 44100): FloatArray {
        val sampleRate = 44100.0
        val f0 = 440.0
        val amplitudeF0 = 0.5
        return FloatArray(numberOfSamples) { sample ->
            val time = sample / sampleRate
            return@FloatArray (amplitudeF0 * sin(2 * PI * f0 * time)).toFloat()
        }
    }

    fun fluteFile(): InputStream {
        val file = "flute.novib.ff.A4.wav"
        return streamFromFilename(file)
    }

    fun ccirFile(): InputStream {
        val file = "CCIR_04221.ogg"
        return streamFromFilename(file)
    }

    fun onsetsAudioFile(): InputStream {
        val file = "NR45.wav"
        return streamFromFilename(file)
    }

    fun sineOf4000Samples(): InputStream {
        val file = "4000_samples_of_440Hz_at_44.1kHz.wav"
        return streamFromFilename(file)
    }

    fun sineOf4000SamplesFile(): File? {
        val file = "4000_samples_of_440Hz_at_44.1kHz.wav"
        return try {
            ClassLoader.getSystemClassLoader().getResource(file)?.toURI()?.let { File(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun streamFromFilename(filename: String?): InputStream {
        val classLoader = ClassLoader.getSystemClassLoader()
        return classLoader.getResourceAsStream(filename)!!
    }

    /**
     * Reads the contents of a file.
     *
     * @param name the name of the file to read
     * @return the contents of the file if successful, an empty string
     * otherwise.
     */
    fun readFile(name: String): String {
        val contents = StringBuilder()
        try {
            val file = File(name)
            require(file.exists()) { "File $name does not exist" }
            file.readLines().joinToString(separator = "\n")
        } catch (i1: IOException) {
            throw RuntimeException(i1)
        }
        return contents.toString()
    }

    /**
     * Reads the contents of a file in a jar.
     *
     * @param path the path to read e.g. /package/name/here/help.html
     * @return the contents of the file when successful, an empty string
     * otherwise.
     */
    fun readFileFromJar(path: String?): String? {
        val classLoader = ClassLoader.getSystemClassLoader()
        try {
            return classLoader.getResourceAsStream(path)?.bufferedReader()?.lineSequence()
                ?.joinToString(separator = System.lineSeparator())
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * @return a half a second long silent buffer (all zeros), at 44.1kHz.
     */
    fun audioBufferSilence(): FloatArray {
        val sampleRate = 44100.0
        val seconds = 0.5
        return FloatArray((seconds * sampleRate).toInt())
    }
}