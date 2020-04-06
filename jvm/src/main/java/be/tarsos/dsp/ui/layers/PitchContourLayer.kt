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
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.LayerUtilities.pixelsToUnits
import be.tarsos.dsp.util.PitchConverter
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.roundToInt

class PitchContourLayer(
    private val cs: CoordinateSystem,
    private val audioFile: File,
    private val color: Color,
    private val frameSize: Int,
    private val overlap: Int
) : Layer {
    private var features: TreeMap<Double, FloatArray> = TreeMap()
    override fun draw(graphics: Graphics2D) {
        graphics.color = color
        val ovalWidth = pixelsToUnits(graphics, 4, true).roundToInt()
        val ovalHeight = pixelsToUnits(graphics, 4, false).roundToInt()

        // every second
        val submap: Map<Double, FloatArray> = features.subMap(
            cs.getMin(Axis.X) / 1000.0,
            cs.getMax(Axis.X) / 1000.0
        )
        for ((time, value) in submap) {
            val pitch = value[0].toDouble() // in cents
            if (pitch > cs.getMin(Axis.Y) && pitch < cs.getMax(Axis.Y)) {
                graphics.drawOval((time * 1000).toInt(), pitch.toInt(), ovalWidth, ovalHeight)
            }
        }
    }

    init {
        try {
            val adp =
                fromFile(audioFile, frameSize, overlap)
            val timeLag = frameSize / 44100.0
            val fe =
                TreeMap<Double, FloatArray>()
            adp.addAudioProcessor(
                PitchProcessor(
                    PitchEstimationAlgorithm.FFT_YIN, 44100F, frameSize,
                    PitchDetectionHandler { pitchDetectionResult, audioEvent ->
                        if (pitchDetectionResult.isPitched) {
                            val pitch = FloatArray(1)
                            pitch[0] = PitchConverter
                                .hertzToAbsoluteCent(
                                    pitchDetectionResult
                                        .pitch.toDouble()
                                ).toFloat()
                            fe[audioEvent.timeStamp - timeLag] = pitch
                        }
                    })
            )
            adp.addAudioProcessor(object : AudioProcessor {
                override fun processingFinished() {
                    features = fe
                }

                override fun process(audioEvent: AudioEvent): Boolean {
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
        get() = "Pitch contour layer"
}