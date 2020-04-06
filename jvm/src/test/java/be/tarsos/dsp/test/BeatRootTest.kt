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

import be.tarsos.dsp.beatroot.AgentList
import be.tarsos.dsp.beatroot.Event
import be.tarsos.dsp.beatroot.EventList
import be.tarsos.dsp.beatroot.Induction
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromInputStream
import be.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.roundToInt

class BeatRootTest {
    @Test
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun testExpectedOnsets() {
        val inputStream = TestUtilities.onsetsAudioFile()
        val contents = TestUtilities.readFileFromJar("NR45_expected_onsets.txt")
        val onsetStrings = contents.split("\n").toTypedArray()
        val expectedOnsets = DoubleArray(onsetStrings.size)
        var i = 0
        for (onset in onsetStrings) {
            expectedOnsets[i] = onset.toDouble()
            i++
        }
        val d = fromInputStream(
            inputStream,
            2048,
            2048 - 441
        )
        d.setZeroPadFirstBuffer(true)
        val b = BeatRootSpectralFluxOnsetDetector(d, 2048, 441)
        b.setHandler(object : OnsetHandler {
            var i = 0
            override fun handleOnset(
                actualTime: Double,
                salience: Double
            ) {
                val expectedTime = expectedOnsets[i]
                Assertions.assertEquals(
                    expectedTime,
                    actualTime,
                    0.0001,
                    "Onset time should be the expected value!"
                )
                i++
            }
        })
        d.addAudioProcessor(b)
        d.run()
    }

    @Test
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun testExpectedBeats() {
        val inputStream = TestUtilities.onsetsAudioFile()
        val contents = TestUtilities.readFileFromJar("NR45_expected_beats.txt")
        val beatsStrings = contents.split("\n").toTypedArray()
        val expectedBeats = DoubleArray(beatsStrings.size)
        var i = 0
        for (beat in beatsStrings) {
            expectedBeats[i] = beat.toDouble()
            i++
        }
        i = 0
        /** beat data encoded as a list of Events  */
        val onsetList = EventList()
        val d = fromInputStream(
            inputStream,
            2048,
            2048 - 441
        )
        d.setZeroPadFirstBuffer(true)
        val b = BeatRootSpectralFluxOnsetDetector(d, 2048, 441)
        b.setHandler { time, salience ->
            val roundedTime = (time * 100).roundToInt() / 100.0
            val e =
                newEvent(roundedTime, 0)
            e.salience = salience
            onsetList.add(e)
        }
        d.addAudioProcessor(b)
        d.run()
        var agents: AgentList? = null
        // tempo not given; use tempo induction
        agents = Induction.beatInduction(onsetList)
        agents.beatTrack(onsetList, -1.0)
        val best = agents.bestAgent()
        if (best != null) {
            best.fillBeats(-1.0)
            val beats = best.events
            val eventIterator =
                beats.iterator()
            while (eventIterator.hasNext()) {
                val beat = eventIterator.next()
                val expectedTime = expectedBeats[i]
                val actualTime = beat.keyDown
                Assertions.assertEquals(
                    expectedTime,
                    actualTime,
                    0.00001,
                    "Beat time should be the expected value!"
                )
                i++
            }
        } else {
            System.err.println("No best agent")
        }
    }

    companion object {
        /**
         * Creates a new Event object representing an onset or beat.
         *
         * @param time    The time of the beat in seconds
         * @param beatNum The index of the beat or onset.
         * @return The Event object representing the beat or onset.
         */
        fun newEvent(time: Double, beatNum: Int): Event {
            return Event(time, time, time, 56, 64, beatNum.toDouble(), 0.0, 1)
        } // newBeat()
    }
}