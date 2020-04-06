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

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

class WaveFormLayer @JvmOverloads constructor(
    private val cs: CoordinateSystem,
    private val audioFile: File,
    private val waveFormColor: Color = Color.black
) : Layer {
    private var samples: FloatArray? = null
    private var sampleRate = 0f
    override fun draw(graphics: Graphics2D) {
        graphics.color = waveFormColor
        drawWaveForm(graphics)
    }

    private fun drawWaveForm(graphics: Graphics2D) {
        val waveFormXMin = cs.getMin(Axis.X).toInt()
        val waveFormXMax = cs.getMax(Axis.X).toInt()
        graphics.color = Color.GRAY
        graphics.drawLine(waveFormXMin, 0, waveFormXMax, 0)
        graphics.color = Color.BLACK
        if (samples != null && samples!!.isNotEmpty()) {
            //graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            val waveFormHeightInUnits = cs.getDelta(Axis.Y).toInt()
            val lengthInMs = samples!!.size / sampleRate * 1000
            val amountOfSamples = samples!!.size
            val sampleCalculateFactor = amountOfSamples / lengthInMs
            val amplitudeFactor = waveFormHeightInUnits / 2

            //every millisecond:
            val step = 1
            var i = 0.coerceAtLeast(waveFormXMin)
            while (i < waveFormXMax.toFloat().coerceAtMost(lengthInMs)) {
                val index = (i * sampleCalculateFactor).toInt()
                if (index < samples!!.size) {
                    graphics.drawLine(i, 0, i, (samples!![index] * amplitudeFactor).toInt())
                }
                i += step
            }
            //graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }

    init {
        try {
            //max 20min
            val adp: AudioDispatcher = fromFile(audioFile, 44100 * 60 * 20, 0)
            adp.setZeroPadLastBuffer(false)
            sampleRate = adp.format.sampleRate
            adp.addAudioProcessor(object : AudioProcessor {
                override fun processingFinished() {}
                override fun process(audioEvent: AudioEvent): Boolean {
                    val audioFloatBuffer = audioEvent.floatBuffer
                    samples = audioFloatBuffer.clone()
                    return true
                }
            })
            Thread(adp).start()
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        } catch (e2: IOException) {
            e2.printStackTrace()
        }
    }

    override val name: String
        get() = "Waveform layer"

}