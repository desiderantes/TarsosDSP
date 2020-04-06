package be.tarsos.dsp.ui.layers

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromPipe
import be.tarsos.dsp.ui.Axis
import be.tarsos.dsp.ui.CoordinateSystem
import be.tarsos.dsp.ui.layers.TooltipLayer.TooltipTextGenerator
import be.tarsos.dsp.util.PitchConverter
import be.tarsos.dsp.wavelet.HaarWaveletTransform
import be.tarsos.dsp.wavelet.lift.Daubechies4Wavelet
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class Scalogram(private val cs: CoordinateSystem, private val audioFile: String) : Layer, TooltipTextGenerator {
    private var features: TreeMap<Double, ScalogramFrame>? = null
    override fun draw(graphics: Graphics2D) {
        if (features == null) {
            return
        }
        val spectralInfoSubMap: Map<Double, ScalogramFrame> =
            features!!.subMap(
                cs.getMin(Axis.X) / 1000.0,
                cs.getMax(Axis.X) / 1000.0
            )
        for ((timeStart, frame) in spectralInfoSubMap) {
            for (level in frame.dataPerScale.indices) {
                for (block in frame.dataPerScale[level].indices) {
                    var color = Color.black
                    val centsStartingPoint = frame.startFrequencyPerLevel[level]
                    val centsHeight =
                        frame.stopFrequencyPerLevel[level] - centsStartingPoint
                    // only draw the visible frequency range
                    if (centsStartingPoint + centsHeight >= cs.getMin(Axis.Y) && centsStartingPoint <= cs.getMax(
                            Axis.Y
                        )
                    ) {
                        val factor = abs(
                            frame.dataPerScale[level][block] / frame.currentMax
                        )
                        val startTimeBlock =
                            timeStart + (block + 1) * frame.durationsOfBlockPerLevel[level]
                        val timeDuration = frame.durationsOfBlockPerLevel[level].toDouble()
                        var greyValue = (factor * 0.99 * 255).toInt()
                        greyValue = 0.coerceAtLeast(greyValue)
                        color = Color(greyValue, greyValue, greyValue)
                        graphics.color = color
                        graphics.fillRect(
                            (startTimeBlock * 1000).roundToInt(),
                            centsStartingPoint.roundToInt(),
                            (timeDuration * 1000).roundToInt(),
                            ceil(centsHeight.toDouble()).toInt()
                        )
                    }
                }
            }
        }
    }

    override val name: String
        get() = "Scalogram"

    override fun generateTooltip(cs: CoordinateSystem, point: Point2D): String {
        return "Scale info"
    }

    private class ScalogramFrame(transformedData: FloatArray, var currentMax: Float) {
        var dataPerScale: Array<FloatArray>
        var durationsOfBlockPerLevel: FloatArray
        var startFrequencyPerLevel //cents
                : FloatArray
        var stopFrequencyPerLevel //cents
                : FloatArray

        private fun mra(
            transformedData: FloatArray,
            level: Int,
            dataPerScale: FloatArray
        ): FloatArray {
            for ((j, i) in ((transformedData.size / HaarWaveletTransform.pow2(dataPerScale.size - level)) until (transformedData.size / HaarWaveletTransform.pow2(
                dataPerScale.size - level - 1
            ))).withIndex()) {
                dataPerScale[j] = -1 * transformedData[i]
            }
            return normalize(dataPerScale)
        }

        private fun normalize(data: FloatArray): FloatArray {
            for (i in data.indices) {
                currentMax = abs(data[i]).coerceAtLeast(currentMax)
            }
            for (i in data.indices) {
                //data[i]=data[i]/maxValue;
            }
            return data
        }

        init {
            val levels = HaarWaveletTransform.log2(transformedData.size)
            durationsOfBlockPerLevel = FloatArray(levels)
            startFrequencyPerLevel = FloatArray(levels)
            stopFrequencyPerLevel = FloatArray(levels)
            dataPerScale = Array(levels) { i ->
                val samples = HaarWaveletTransform.pow2(i)
                durationsOfBlockPerLevel[i] = 131072 / samples.toFloat() / 44100.0f
                stopFrequencyPerLevel[i] = PitchConverter.hertzToAbsoluteCent(
                    44100.0 / HaarWaveletTransform.pow2(levels - i)
                ).toFloat()
                if (i > 0) {
                    startFrequencyPerLevel[i] = stopFrequencyPerLevel[i - 1]
                }
                return@Array mra(transformedData, i, FloatArray(samples))
            }
        }
    }

    init {
        thread(start = true, name = "Extract Scalogram") {
            val adp =
                fromPipe(audioFile, 44100, 131072, 0)
            adp.addAudioProcessor(object : AudioProcessor {
                var wt = Daubechies4Wavelet()
                var calculatingFeatures =
                    TreeMap<Double, ScalogramFrame>()
                var prevFrame: ScalogramFrame? = null
                override fun process(audioEvent: AudioEvent): Boolean {
                    val audioBuffer = audioEvent.floatBuffer.clone()
                    wt.forwardTrans(audioBuffer)
                    var currentMax = 0f
                    if (prevFrame != null) {
                        currentMax = prevFrame!!.currentMax * 0.99f
                    }
                    val currentFrame = ScalogramFrame(audioBuffer, currentMax)
                    calculatingFeatures[audioEvent.timeStamp] = currentFrame
                    prevFrame = currentFrame
                    return true
                }

                override fun processingFinished() {
                    features = calculatingFeatures
                }
            })
            adp.run()
        }
    }
}