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
package be.tarsos.dsp.io

import be.tarsos.dsp.util.FFMPEGDownloader
import java.io.*
import java.nio.ByteOrder
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.concurrent.thread

/**
 *
 *
 * Decode audio files to PCM, mono, 16bits per sample, at any sample rate using
 * an external program. By default ffmpeg is used. Other
 * command Line  programs that are able to decode audio and pipe binary PCM
 * samples to STDOUT are possible as well (avconv, mplayer).
 * To install ffmpeg on Debian: `apt-get install ffmpeg`.
 *
 *
 *
 * This adds support for a lot of audio formats and video container formats with
 * relatively little effort. Depending on the program used also http streams,
 * rtpm streams, ... are supported as well.
 *
 *
 *
 * To see which audio decoders are supported, check
 *
 * `<pre>ffmpeg -decoders | grep -E "^A" | sort
 * avconv version 9.8, Copyright (c) 2000-2013 the Libav developers
 * built on Aug 26 2013 09:52:20 with gcc 4.4.3 (Ubuntu 4.4.3-4ubuntu5.1)
 * A... 8svx_exp             8SVX exponential
 * A... 8svx_fib             8SVX fibonacci
 * A... aac                  AAC (Advanced Audio Coding)
 * A... aac_latm             AAC LATM (Advanced Audio Coding LATM syntax)
 * A... ac3                  ATSC A/52A (AC-3)
 * A... adpcm_4xm            ADPCM 4X Movie
 * ...
</pre>` *
 *
 * @author Joren Six
 */
class PipeDecoder {
    private val pipeEnvironment: String
    private val pipeArgument: String
    private val pipeCommand: String
    private val pipeBuffer: Int
    private var printErrorstream = false
    private var decoderBinaryAbsolutePath: String? = null

    constructor() {
        pipeBuffer = 10000

        //Use sensible defaults depending on the platform
        when {
            System.getProperty("os.name").indexOf("indows") > 0 -> {
                pipeEnvironment = "cmd.exe"
                pipeArgument = "/C"
            }
            File("/bin/bash").exists() -> {
                pipeEnvironment = "/bin/bash"
                pipeArgument = "-c"
            }
            File("/system/bin/sh").exists() -> {
                //probably we are on android here
                pipeEnvironment = "/system/bin/sh"
                pipeArgument = "-c"
            }
            else -> {
                LOG.severe("Coud not find a command line environment (cmd.exe or /bin/bash)")
                throw Error("Decoding via a pipe will not work: Coud not find a command line environment (cmd.exe or /bin/bash)")
            }
        }
        val path = System.getenv("PATH")
        val arguments =
            " -ss %input_seeking%  %number_of_seconds% -i \"%resource%\" -vn -ar %sample_rate% -ac %channels% -sample_fmt s16 -f s16le pipe:1"
        if (isAvailable("ffmpeg")) {
            LOG.info("found ffmpeg on the path ($path). Will use ffmpeg for decoding media files.")
            pipeCommand = "ffmpeg$arguments"
        } else if (isAvailable("avconv")) {
            LOG.info("found avconv on your path($path). Will use avconv for decoding media files.")
            pipeCommand = "avconv$arguments"
        } else {
            if (isAndroid) {
                val tempDirectory = System.getProperty("java.io.tmpdir")
                printErrorstream = true
                val f = File(tempDirectory, "ffmpeg")
                if (f.exists() && f.length() > 1000000 && f.canExecute()) {
                    decoderBinaryAbsolutePath = f.absolutePath
                } else {
                    LOG.severe("Could not find an ffmpeg binary for your Android system. Did you forget calling: 'new AndroidFFMPEGLocator(this);' ?")
                    LOG.severe("Tried to unpack a statically compiled ffmpeg binary for your architecture to: " + f.absolutePath)
                }
            } else {
                LOG.warning("Dit not find ffmpeg or avconv on your path($path), will try to download it automatically.")
                val downloader = FFMPEGDownloader()
                decoderBinaryAbsolutePath = downloader.ffmpegBinary()
                if (decoderBinaryAbsolutePath == null) {
                    LOG.severe("Could not download an ffmpeg binary automatically for your system.")
                }
            }
            if (decoderBinaryAbsolutePath == null) {
                pipeCommand = "false"
                throw Error("Decoding via a pipe will not work: Could not find an ffmpeg binary for your system")
            } else {
                pipeCommand = '"'.toString() + decoderBinaryAbsolutePath + '"' + arguments
            }
        }
    }

    constructor(
        pipeEnvironment: String,
        pipeArgument: String,
        pipeCommand: String,
        pipeLogFile: String?,
        pipeBuffer: Int
    ) {
        this.pipeEnvironment = pipeEnvironment
        this.pipeArgument = pipeArgument
        this.pipeCommand = pipeCommand
        this.pipeBuffer = pipeBuffer
    }

    private fun isAvailable(command: String): Boolean {
        return try {
            Runtime.getRuntime().exec("$command -version")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getDecodedStream(
        resource: String,
        targetSampleRate: Int,
        timeOffset: Double,
        numberOfSeconds: Double
    ): InputStream? {
        try {
            var command = pipeCommand
            command = command.replace("%input_seeking%", timeOffset.toString())
            //defines the number of seconds to process
            // -t 10.000 e.g. specifies to process ten seconds
            // from the specified time offset (which is often zero).
            command = if (numberOfSeconds > 0) {
                command.replace("%number_of_seconds%", "-t $numberOfSeconds")
            } else {
                command.replace("%number_of_seconds%", "")
            }
            command = command.replace("%resource%", resource)
            command = command.replace("%sample_rate%", targetSampleRate.toString())
            command = command.replace("%channels%", "1")
            val pb: ProcessBuilder
            pb = ProcessBuilder(pipeEnvironment, pipeArgument, command)
            LOG.info("Starting piped decoding process for $resource")
            LOG.info(" with command: $command")
            val process = pb.start()
            val stdOut: InputStream =
                object : BufferedInputStream(process.inputStream, pipeBuffer) {
                    @Throws(IOException::class)
                    override fun close() {
                        super.close()
                        // try to destroy the ffmpeg command after close
                        process.destroy()
                    }
                }

            //print std error if requested
            if (printErrorstream) {
                ErrorStreamGobbler(process.errorStream, LOG).start()
            }
            thread(start = true, name = "Decoding Pipe") {
                try {
                    process.waitFor()
                    LOG.info("Finished piped decoding process")
                } catch (e: InterruptedException) {
                    LOG.severe("Interrupted while waiting for decoding sub process exit.")
                    e.printStackTrace()
                }
            }
            return stdOut
        } catch (e: IOException) {
            LOG.warning("IO exception while decoding audio via sub process." + e.message)
            e.printStackTrace()
        }
        return null
    }

    fun getDuration(resource: String): Double {
        var duration = -1.0
        try {
            //use " for windows compatibility!
            var command = "ffmpeg -i \"%resource%\""
            command = command.replace("%resource%", resource)
            val pb: ProcessBuilder
            pb = ProcessBuilder(pipeEnvironment, pipeArgument, command)
            LOG.info("Starting duration command for $resource")
            LOG.fine(" with command: $command")
            val process = pb.start()
            val stdOut: InputStream =
                object : BufferedInputStream(process.inputStream, pipeBuffer) {
                    @Throws(IOException::class)
                    override fun close() {
                        super.close()
                        // try to destroy the ffmpeg command after close
                        process.destroy()
                    }
                }
            val essg = ErrorStreamStringGlobber(process.errorStream)
            essg.start()
            thread(start = true, name = "Decoding Pipe") {
                try {
                    process.waitFor()
                    LOG.info("Finished piped decoding process")
                } catch (e: InterruptedException) {
                    LOG.severe("Interrupted while waiting for decoding sub process exit.")
                    e.printStackTrace()
                }
            }
            val stdError = essg.errorStreamAsString
            val regex = Pattern.compile(
                ".*\\s.*Duration:\\s+(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d), .*",
                Pattern.DOTALL or Pattern.MULTILINE
            )
            val regexMatcher = regex.matcher(stdError)
            if (regexMatcher.find()) {
                duration =
                    regexMatcher.group(1).toInt() * 3600 + regexMatcher.group(2).toInt() * 60 + regexMatcher.group(3)
                        .toInt() + ("." + regexMatcher.group(
                        4
                    )).toDouble()
            }
        } catch (e: IOException) {
            LOG.warning("IO exception while decoding audio via sub process." + e.message)
            e.printStackTrace()
        }
        return duration
    }

    fun printBinaryInfo() {
        try {
            val p = Runtime.getRuntime().exec(decoderBinaryAbsolutePath)

            p.errorStream.bufferedReader().useLines {
                println(it)
            }
            p.waitFor()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }//the class is not found when running JVM

    // This class is only available on android
    private val isAndroid: Boolean
        get() = try {
            // This class is only available on android
            Class.forName("android.app.Activity")
            println("Running on Android!")
            true
        } catch (e: ClassNotFoundException) {
            //the class is not found when running JVM
            false
        }

    inner class ErrorStreamGobbler(
        private val `is`: InputStream,
        private val logger: Logger
    ) : Thread() {
        override fun run() {
            try {
                val isr = InputStreamReader(`is`)
                val br = BufferedReader(isr)
                var line: String? = null
                while (br.readLine().also { line = it } != null) {
                    logger.info(line)
                }
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }

    }

    inner class ErrorStreamStringGlobber(private val inputStream: InputStream) : Thread() {
        private val sb: StringBuilder = StringBuilder()
        override fun run() {
            try {
                inputStream.bufferedReader().useLines {
                    sb.append(it)
                }
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }

        val errorStreamAsString: String
            get() = sb.toString()

    }

    companion object {
        private val LOG =
            Logger.getLogger(PipeDecoder::class.java.name)

        /**
         * Constructs the target audio format. The audio format is one channel
         * signed PCM of a given sample rate.
         *
         * @param targetSampleRate The sample rate to convert to.
         * @return The audio format after conversion.
         */
        @JvmStatic
        fun getTargetAudioFormat(targetSampleRate: Int): TarsosDSPAudioFormat {
            return TarsosDSPAudioFormat(
                TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
                targetSampleRate.toFloat(),
                2 * 8,
                1,
                2 * 1,
                targetSampleRate.toFloat(), ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
            )
        }
    }
}