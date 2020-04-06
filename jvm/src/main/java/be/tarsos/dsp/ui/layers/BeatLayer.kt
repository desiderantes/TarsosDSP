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
package be.tarsos.dsp.ui.layers

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class BeatLayer(
    private val cs: CoordinateSystem,
    private val audioFile: File,
    private val showBeats: Boolean,
    private val showOnsets: Boolean
) : Layer {
    private val onsets // in seconds
            : MutableList<Double> = ArrayList()
    private val beats //in seconds
            : MutableList<Double> = ArrayList()
    private val onsetColor: Color = Color.blue
    private val beatColor: Color = Color.red
    private val frameSize: Int = 256
    private val overlap: Int = 0
    override fun draw(graphics: Graphics2D) {
        val maxY = cs.getMax(Axis.Y).roundToInt()
        val minY = cs.getMin(Axis.Y).roundToInt()
        if (onsets.isNotEmpty() && showOnsets) {
            graphics.color = onsetColor
            for (onset in onsets) {
                val onsetTime = (onset * 1000).roundToInt() //in ms
                graphics.drawLine(onsetTime, minY, onsetTime, maxY)
            }
        }
        if (beats.isNotEmpty() && showBeats) {
            graphics.color = beatColor
            for (beat in beats) {
                val beatTime = (beat * 1000).roundToInt() //in ms
                graphics.drawLine(beatTime, minY, beatTime, maxY)
            }
        }
    }

    init {
        try {
            val adp =
                fromFile(audioFile, frameSize, overlap)
            val sampleRate = adp.format.sampleRate
            val lag = frameSize / sampleRate / 2.0 // in seconds
            val detector = ComplexOnsetDetector(frameSize)
            val broeh = BeatRootOnsetEventHandler()
            adp.addAudioProcessor(detector)
            adp.addAudioProcessor(object : AudioProcessor {
                override fun processingFinished() {
                    broeh.trackBeats { time, _ ->
                        beats.add(time - lag)
                    }
                }

                override fun process(audioEvent: AudioEvent): Boolean {
                    return true
                }
            })
            detector.setHandler { time, salience ->
                onsets.add(time - lag)
                broeh.handleOnset(time - lag, salience)
            }
            Thread(adp).start()
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        } catch (e2: IOException) {
            e2.printStackTrace()
        }
    }

    override val name: String
        get() = "Beats Layer"
}