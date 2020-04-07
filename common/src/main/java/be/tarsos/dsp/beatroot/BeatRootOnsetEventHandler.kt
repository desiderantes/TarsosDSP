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
package be.tarsos.dsp.beatroot

import be.tarsos.dsp.onsets.OnsetHandler
import kotlin.math.roundToLong

/**
 * Forms a bridge between the BeatRoot beat tracking system and an
 * interchangeable onset detector. The beat tracker does not work in real-time.
 * First all onsets need to be detected. In a post-processing step a beat
 * estimation is done using reocurring inter onset intervals (IOI's). To return
 * the time of the beats an OnsetHandler is abused.
 *
 * @author Joren Six
 */
class BeatRootOnsetEventHandler : OnsetHandler {
    private val onsetList = EventList()
    override fun handleOnset(time: Double, salience: Double) {
        val roundedTime = (time * 100).roundToLong() / 100.0
        val e = newEvent(roundedTime, 0)
        e.salience = salience
        onsetList.add(e)
    }

    /**
     * Creates a new Event object representing an onset or beat.
     *
     * @param time    The time of the beat in seconds
     * @param beatNum The index of the beat or onset.
     * @return The Event object representing the beat or onset.
     */
    private fun newEvent(time: Double, beatNum: Int): Event {
        return Event(time, time, time, 56, 64, beatNum.toDouble(), 0.0, 1)
    }

    /**
     * Guess the beats using the populated list of onsets.
     *
     * @param beatHandler Use this handler to get the time of the beats. The salience of
     * the beat is not calculated: -1 is returned.
     */
    fun trackBeats(beatHandler: OnsetHandler) {
        // tempo not given; use tempo induction
        val agents: AgentList = Induction.beatInduction(onsetList)
        agents.beatTrack(onsetList, -1.0)
        val best = agents.bestAgent()
        if (best != null) {
            best.fillBeats(-1.0)
            val beats = best.events
            val eventIterator = beats.iterator()
            while (eventIterator.hasNext()) {
                val beat = eventIterator.next()
                val time = beat.keyDown
                beatHandler.handleOnset(time, -1.0)
            }
        } else {
            System.err.println("No best agent")
        }
    }
}