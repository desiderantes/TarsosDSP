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

import be.tarsos.dsp.util.PitchConverter.hertzToRelativeCent
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.*
import javax.swing.JPanel
import kotlin.collections.ArrayList
import kotlin.math.abs

class UtterAsteriskPanel : JPanel() {
    var pattern =
        doubleArrayOf(400.0, 400.0, 600.0, 400.0, 900.0, 800.0, 400.0, 400.0, 600.0, 400.0, 1100.0, 900.0) // in cents
    var timing =
        doubleArrayOf(3.0, 1.0, 4.0, 4.0, 4.0, 6.0, 3.0, 1.0, 4.0, 4.0, 4.0, 6.0) //in eight notes
    var startTimeStamps: ArrayList<Double> = ArrayList()
    var pitches: ArrayList<Double> = ArrayList()
    private val patternLength //in seconds
            : Double
    private var currentMarker: Double
    private var lastReset: Long = 0
    private var score = 0
    private var patternLengthInQuarterNotes = 0.0
    override fun paint(g: Graphics) {
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON
        )
        graphics.background = Color.WHITE
        graphics.clearRect(0, 0, width, height)
        val x = (currentMarker / patternLength.toFloat() * width).toInt()
        if (x < 3 && System.currentTimeMillis() - lastReset > 1000) {
            lastReset = System.currentTimeMillis()
            score()
            pitches.clear()
            startTimeStamps.clear()
        }
        graphics.drawLine(x, 0, x, height)
        if (lastReset != 0L) {
            graphics.drawString("Score: $score", width / 2, 20)
        }
        graphics.color = Color.GRAY
        val lengthPerQuarterNote =
            patternLength / patternLengthInQuarterNotes // in seconds per quarter note
        var currentXPosition = 0.5 // seconds of pause before start
        for (i in pattern.indices) {
            val lengthInSeconds = timing[i] * lengthPerQuarterNote //seconds
            val patternWidth = (lengthInSeconds / patternLength * width).toInt() //pixels
            val patternHeight = (CENTS_DEVIATION / 1200.0 * height).toInt()
            val patternX = (currentXPosition / patternLength * width).toInt()
            val patternY = height - (pattern[i] / 1200.0 * height).toInt() - patternHeight / 2
            graphics.drawRect(patternX, patternY, patternWidth, patternHeight)
            currentXPosition += lengthInSeconds //in seconds
        }
        graphics.color = Color.RED
        for (i in pitches.indices) {
            val pitchInCents = pitches[i]
            val startTimeStamp = startTimeStamps[i] % patternLength
            val patternX = (startTimeStamp / patternLength * width).toInt()
            val patternY = height - (pitchInCents / 1200.0 * height).toInt()
            graphics.drawRect(patternX, patternY, 2, 2)
        }
    }

    private fun score() {
        score = 0
        for (i in pitches.indices) {
            val pitchInCents = pitches[i]
            val startTimeStamp = startTimeStamps[i] % patternLength
            if (startTimeStamp > 0.5 && startTimeStamp <= 0.5 + 0.5 * pattern.size) {
                val lengthPerQuarterNote =
                    patternLength / patternLengthInQuarterNotes // in seconds per quarter note
                var currentXPosition = 0.5 // seconds of pause before start
                for (j in pattern.indices) {
                    val lengthInSeconds = timing[j] * lengthPerQuarterNote //seconds
                    if (startTimeStamp > currentXPosition && startTimeStamp <= currentXPosition + lengthInSeconds && abs(
                            pitchInCents - pattern[j]
                        ) < CENTS_DEVIATION
                    ) {
                        score++
                    }
                    currentXPosition += lengthInSeconds //in seconds
                }
            }
        }
    }

    fun setMarker(timeStamp: Double, frequency: Double) {
        currentMarker = timeStamp % patternLength
        //ignore everything outside 80-2000Hz
        if (frequency > 80 && frequency < 2000) {
            val pitchInCents = hertzToRelativeCent(frequency)
            pitches.add(pitchInCents)
            startTimeStamps.add(timeStamp)
        }
        this.repaint()
    }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -5330666476785715988L
        private const val CENTS_DEVIATION = 30.0
    }

    init {
        for (timeInQuarterNotes in timing) {
            patternLengthInQuarterNotes += timeInQuarterNotes
        }
        patternLength = 12.0
        currentMarker = 0.0
    }
}