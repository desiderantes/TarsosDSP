package be.tarsos.dsp.io.android

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import java.io.*

/**
 *
 *
 * The Android FFMPEG locator determines the current CPU architecture of the
 * running Android device and extracts a statically compiled [ffmpeg](http://ffmpeg.org)
 * binary from the assets folder to the temporary directory of the currently running Android application.
 * For this to work the assets folder should contain these binaries:
 *
 *
 *  *
 * `assets/x86_ffmpeg` for x86
 * `assets/armeabi-v7a_ffmpeg` for armeabi-v7a
 * `assets/armeabi-v7a-neon_ffmpeg` for armeabi-v7a-neon
 * `assets/arm64-v8a_ffmpeg` for arm64-v8a
 *
 *
 *
 *
 *
 * You can download these binaries
 * [here](https://github.com/hiteshsondhi88/ffmpeg-android/releases/download/v0.3.3/prebuilt-binaries.zip)
 * and on the [TarsosDSP ffmpeg repository](http://0110.be/releases/TarsosDSP/TarsosDSP-static-ffmpeg/Android/).
 * Other architectures are currently not supported but could be included in later releases.
 *
 *
 *
 *
 * If you are a masochist and want to compile ffmpeg for Android yourself you can get your fix [here](https://github.com/hiteshsondhi88/ffmpeg-android)
 *
 *
 * @author Joren Six
 */
class AndroidFFMPEGLocator(context: Context) {
    private fun getFFMPEGFileName(architecture: CPUArchitecture?): String {
        val ffmpegFileName: String?
        ffmpegFileName = when (architecture) {
            CPUArchitecture.X86 -> "x86_ffmpeg"
            CPUArchitecture.ARMEABI_V7A -> "armeabi-v7a_ffmpeg"
            CPUArchitecture.ARMEABI_V7A_NEON -> "armeabi-v7a-neon_ffmpeg"
            CPUArchitecture.ARM64_V8A -> "arm64-v8a_ffmpeg"
            else -> {
                val message =
                    "Could not determine your processor architecture correctly, no ffmpeg binary available."
                Log.e(TAG, message)
                throw Error(message)
            }
        }
        return ffmpegFileName
    }

    private fun ffmpegIsCorrectlyInstalled(): Boolean {
        val ffmpegTargetLocation =
            ffmpegTargetLocation()
        //assumed to be correct if existing and executable and larger than 1MB:
        return ffmpegTargetLocation.exists() && ffmpegTargetLocation.canExecute() && ffmpegTargetLocation.length() > 1000000
    }

    private fun unpackFFmpeg(
        assetManager: AssetManager,
        ffmpegAssetFileName: String
    ) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val ffmpegTargetLocation =
                ffmpegTargetLocation()
            inputStream = assetManager.open(ffmpegAssetFileName)
            outputStream = FileOutputStream(ffmpegTargetLocation)
            val buffer = ByteArray(1024)
            var length = 0
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            //makes ffmpeg executable
            ffmpegTargetLocation.setExecutable(true)
            Log.i(
                TAG,
                "Unpacked ffmpeg binary " + ffmpegAssetFileName + " , extracted  " + ffmpegTargetLocation.length() + " bytes. Extracted to: " + ffmpegTargetLocation.absolutePath
            )
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            //cleanup
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun isCPUArchitectureSupported(alias: String): Boolean {
        for (supportedAlias in Build.SUPPORTED_ABIS) {
            if (supportedAlias == alias) return true
        }
        return false
    }// check if NEON is supported:

    // check if device is x86
    private val cPUArchitecture: CPUArchitecture?
        private get() {
            // check if device is x86
            when {
                isCPUArchitectureSupported("x86") -> {
                    return CPUArchitecture.X86
                }
                isCPUArchitectureSupported("arm64-v8a") -> {
                    return CPUArchitecture.ARM64_V8A
                }
                isCPUArchitectureSupported("armeabi-v7a") -> {
                    // check if NEON is supported:
                    return if (isNeonSupported) {
                        CPUArchitecture.ARMEABI_V7A_NEON
                    } else {
                        CPUArchitecture.ARMEABI_V7A
                    }
                }
                else -> return null
            }
        }

    private val isNeonSupported: Boolean
        get() {
            try {
                val input = BufferedReader(
                    InputStreamReader(
                        FileInputStream(
                            File("/proc/cpuinfo")
                        )
                    )
                )
                var line: String? = null
                while (input.readLine().also { line = it } != null) {
                    Log.d(TAG, "CPUINFO line: $line")
                    if (line!!.toLowerCase().contains("neon")) {
                        return true
                    }
                }
                input.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return false
        }

    private enum class CPUArchitecture {
        X86, ARMEABI_V7A, ARMEABI_V7A_NEON, ARM64_V8A
    }

    companion object {
        private const val TAG = "AndroidFFMPEGLocator"
        private fun ffmpegTargetLocation(): File {
            val tempDirectory = System.getProperty("java.io.tmpdir")
            return File(tempDirectory, "ffmpeg")
        }
    }

    init {
        val architecture = cPUArchitecture
        Log.i(
            TAG,
            "Detected Native CPU Architecture: " + architecture!!.name
        )
        if (!ffmpegIsCorrectlyInstalled()) {
            val ffmpegFileName = getFFMPEGFileName(architecture)
            val assetManager = context.assets
            unpackFFmpeg(assetManager, ffmpegFileName)
        }
        val ffmpegTargetLocation =
            ffmpegTargetLocation()
        Log.i(
            TAG,
            "Ffmpeg binary location: " + ffmpegTargetLocation.absolutePath + " is executable? " + ffmpegTargetLocation.canExecute() + " size: " + ffmpegTargetLocation.length() + " bytes"
        )
    }
}