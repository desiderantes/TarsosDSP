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

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer

object Shared {
    var osName: String = System.getProperty("os.name")
    fun getMixerInfo(
        supportsPlayback: Boolean, supportsRecording: Boolean
    ): Vector<Mixer.Info> {
        val infos = Vector<Mixer.Info>()
        val mixers = AudioSystem.getMixerInfo()
        for (mixerinfo in mixers) {
            if (supportsRecording
                && AudioSystem.getMixer(mixerinfo).targetLineInfo.isNotEmpty()
            ) {
                // Mixer capable of recording audio if target LineWavelet length != 0
                infos.add(mixerinfo)
            } else if (supportsPlayback
                && AudioSystem.getMixer(mixerinfo).sourceLineInfo.isNotEmpty()
            ) {
                // Mixer capable of audio play back if source LineWavelet length != 0
                infos.add(mixerinfo)
            }
        }
        return infos
    }

    fun toLocalString(info: Any): String {
        if (!isWindows) return info.toString()
        val defaultEncoding = Charset.defaultCharset()
        return try {
            String(info.toString().toByteArray(charset("windows-1252")), defaultEncoding)
        } catch (ex: UnsupportedEncodingException) {
            info.toString()
        }
    }

    val isWindows: Boolean
        get() = osName.startsWith("Windows")
}