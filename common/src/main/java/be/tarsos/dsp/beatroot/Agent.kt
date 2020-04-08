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
/*  BeatRoot: An interactive beat tracking system
    Copyright (C) 2001, 2006 by Simon Dixon

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program (the file gpl.txt); if not, download it from
	http://www.gnu.org/licenses/gpl.txt or write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package be.tarsos.dsp.beatroot

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Agent is the central class for beat tracking.
 * Each Agent object has a tempo hypothesis, a history of tracked beats, and
 * a score evaluating the continuity, regularity and salience of its beat track.
 */
class Agent {
    /**
     * The size of the outer half-window before the predicted beat time.
     */
    var preMargin = 0.0

    /**
     * The size of the outer half-window after the predicted beat time.
     */
    var postMargin = 0.0

    /**
     * To be used in real-time version??
     */
    @JvmField
    var tempoScore = 0.0

    /**
     * Sum of salience values of the Events which have been interpreted
     * as beats by this Agent, weighted by their nearness to the predicted beat times.
     */
    @JvmField
    var phaseScore = 0.0

    /**
     * How long has this agent been the best?  For real-time version; otherwise not used.
     */
    @JvmField
    var topScoreTime = 0.0

    /**
     * The number of beats found by this Agent, including interpolated beats.
     */
    @JvmField
    var beatCount = 0

    /**
     * The current tempo hypothesis of the Agent, expressed as the beat period in seconds.
     */
    @JvmField
    var beatInterval = 0.0

    /**
     * The initial tempo hypothesis of the Agent, expressed as the beat period in seconds.
     */
    var initialBeatInterval = 0.0

    /**
     * The time of the most recent beat accepted by this Agent.
     */
    @JvmField
    var beatTime = 0.0

    /**
     * The list of Events (onsets) accepted by this Agent as beats, plus interpolated beats.
     */
    @JvmField
    var events: EventList

    /**
     * The Agent's unique identity number.
     */
    protected var idNumber = 0

    /**
     * Copy constructor.
     *
     * @param clone The Agent to duplicate.
     */
    constructor(clone: Agent) {
        idNumber = idCounter++
        phaseScore = clone.phaseScore
        tempoScore = clone.tempoScore
        topScoreTime = clone.topScoreTime
        beatCount = clone.beatCount
        beatInterval = clone.beatInterval
        initialBeatInterval = clone.initialBeatInterval
        beatTime = clone.beatTime
        events = EventList(clone.events)
        postMargin = clone.postMargin
        preMargin = clone.preMargin
    } // copy constructor

    /**
     * Constructor: the work is performed by init()
     *
     * @param ibi The beat period (inter-beat interval) of the Agent's tempo hypothesis.
     */
    constructor(ibi: Double) {
        innerMargin = INNER_MARGIN
        correctionFactor =
            DEFAULT_CORRECTION_FACTOR
        expiryTime = DEFAULT_EXPIRY_TIME
        decayFactor = 0.0
        beatInterval = ibi
        initialBeatInterval = ibi
        postMargin = ibi * POST_MARGIN_FACTOR
        preMargin = ibi * PRE_MARGIN_FACTOR
        idNumber = idCounter++
        phaseScore = 0.0
        tempoScore = 0.0
        topScoreTime = 0.0
        beatCount = 0
        beatTime = -1.0
        events = EventList()
    } // init()
    /**
     * Output debugging information about this Agent.
     *
     * @param level The level of detail in debugging
     */
    /**
     * Output debugging information about this Agent, at the default (highest) level of detail.
     */
    @JvmOverloads
    fun print(level: Int = 100) {
        System.out.printf("\tAg#%4d: %5.3f", idNumber, beatInterval)
        if (level >= 1) {
            System.out.printf(
                "  Beat#%3d  Time=%7.3f  Score=%4.2f:P%4.2f:%3.1f",
                beatCount, beatTime, tempoScore, phaseScore,
                topScoreTime
            )
        }
        if (level >= 2) println()
        if (level >= 3) events.print()
    } // print()
    // print()/0
    /**
     * Accept a new Event as a beat time, and update the state of the Agent accordingly.
     *
     * @param e     The Event which is accepted as being on the beat.
     * @param err   The difference between the predicted and actual beat times.
     * @param beats The number of beats since the last beat that matched an Event.
     */
    protected fun accept(e: Event, err: Double, beats: Int) {
        beatTime = e.keyDown
        events.add(e)
        if (abs(
                initialBeatInterval - beatInterval - err / correctionFactor
            ) < MAX_CHANGE * initialBeatInterval
        ) beatInterval += err / correctionFactor // Adjust tempo
        beatCount += beats
        val conFactor = 1.0 - CONF_FACTOR * err /
                if (err > 0) postMargin else -preMargin
        if (decayFactor > 0) {
            val memFactor = 1.0 - 1.0 / threshold(
                beatCount.toDouble(),
                1.0,
                decayFactor
            )
            phaseScore = memFactor * phaseScore +
                    (1.0 - memFactor) * conFactor * e.salience
        } else phaseScore += conFactor * e.salience
        if (debug) {
            print(1)
            System.out.printf(
                """  Err=${if (err < 0) "" else "+"}%5.3f${if (abs(err) > innerMargin) '*' else ' '}%5.3f
""",
                err, conFactor
            )
        }
    } // accept()

    private fun threshold(value: Double, min: Double, max: Double): Double {
        if (value < min) return min
        return if (value > max) max else value
    }

    /**
     * The given Event is tested for a possible beat time. The following situations can occur:
     * 1) The Agent has no beats yet; the Event is accepted as the first beat.
     * 2) The Event is beyond expiryTime seconds after the Agent's last 'confirming' beat; the Agent is terminated.
     * 3) The Event is within the innerMargin of the beat prediction; it is accepted as a beat.
     * 4) The Event is within the outerMargin's of the beat prediction; it is accepted as a beat by this Agent,
     * and a new Agent is created which doesn't accept it as a beat.
     * 5) The Event is ignored because it is outside the windows around the Agent's predicted beat time.
     *
     * @param e The Event to be tested
     * @param a The list of all agents, which is updated if a new agent is created.
     * @return Indicate whether the given Event was accepted as a beat by this Agent.
     */
    fun considerAsBeat(e: Event, a: AgentList): Boolean {
        val err: Double
        if (beatTime < 0) {    // first event
            accept(e, 0.0, 1)
            return true
        } else {            // subsequent events
            if (e.keyDown - events.l.last.keyDown > expiryTime) {
                phaseScore = -1.0 // flag agent to be deleted
                return false
            }
            val beats = ((e.keyDown - beatTime) / beatInterval).roundToLong().toDouble()
            err = e.keyDown - beatTime - beats * beatInterval
            if (beats > 0 && -preMargin <= err && err <= postMargin) {
                if (abs(err) > innerMargin) // Create new agent that skips this
                    a.add(Agent(this)) //  event (avoids large phase jump)
                accept(e, err, beats.toInt())
                return true
            }
        }
        return false
    } // considerAsBeat()

    /**
     * Interpolates missing beats in the Agent's beat track, starting from the beginning of the piece.
     */
    protected fun fillBeats() {
        fillBeats(-1.0)
    } // fillBeats()/0

    /**
     * Interpolates missing beats in the Agent's beat track.
     *
     * @param start Ignore beats earlier than this start time
     */
    fun fillBeats(start: Double) {
        var prevBeat = 0.0
        var nextBeat: Double
        var currentInterval: Double
        var beats: Double
        val list = events.listIterator()
        if (list.hasNext()) {
            prevBeat = list.next().keyDown
            // alt. to fill from 0:
            // prevBeat = Math.mod(list.next().keyDown, beatInterval);
            list.previous()
        }
        while (list.hasNext()) {
            nextBeat = list.next().keyDown
            list.previous()
            beats = ((nextBeat - prevBeat) / beatInterval - 0.01).roundToLong().toDouble() //prefer slow
            currentInterval = (nextBeat - prevBeat) / beats
            while (nextBeat > start && beats > 1.5) {
                prevBeat += currentInterval
                if (debug) System.out.printf(
                    "Insert beat at: %8.3f (n=%1.0f)\n",
                    prevBeat, beats - 1.0
                )
                list.add(newBeat(prevBeat, 0)) // more than once OK??
                beats--
            }
            prevBeat = nextBeat
            list.next()
        }
    } // fillBeats()

    /**
     * Creates a new Event object representing a beat.
     *
     * @param time    The time of the beat in seconds
     * @param beatNum The index of the beat
     * @return The Event object representing the beat
     */
    private fun newBeat(time: Double, beatNum: Int): Event {
        return Event(time, time, time, 56, 64, beatNum.toDouble(), 0.0, 1)
    } // newBeat()
    /**
     * Show detailed debugging output describing the beat tracking behaviour of this agent.
     *
     * @param allEvents An EventList of all onsets
     * @param level     The metrical level of beat tracking relative to the notated beat (used to count beats)
     */
    /**
     * Show detailed debugging output describing the beat tracking behaviour of this agent.
     * Calls showTracking()/1 with a default metrical level of 1.
     *
     * @param allEvents An EventList of all onsets
     */
    @JvmOverloads
    fun showTracking(allEvents: EventList, level: Double = 1.0) {
        var count = 1
        var gapCount: Int
        var prevBeat: Double
        var nextBeat: Double
        var gap: Double
        val beats = events.listIterator() // point to 1st beat
        val all = allEvents.listIterator() // point to 1st event
        if (!beats.hasNext()) {
            System.err.println("No beats found")
            return
        }
        prevBeat = events.l.first.keyDown
        // prevBeat = fmod(beats.next().keyDown, beatInterval);
        print("Beat  (IBI)   BeatTime   Other Events")
        var first = true
        while (all.hasNext()) {    // print each real event
            val currentEvent = all.next()
            var currentBeat: Event? = null
            while (beats.hasNext()) {    // if event was chosen as beat
                currentBeat = beats.next()
                if (currentBeat.keyDown > currentEvent.keyDown + Induction.clusterWidth) break
                gap = currentBeat.keyDown - prevBeat
                gapCount = (gap / beatInterval).roundToLong().toInt()
                for (j in 1 until gapCount) {    //empty beat(s) before event
                    nextBeat = prevBeat + gap / gapCount
                    System.out.printf(
                        "\n%4d (%5.3f) [%7.3f ]",
                        count++, nextBeat - prevBeat, nextBeat
                    )
                    prevBeat = nextBeat
                }
                System.out.printf(
                    "\n%4d (%5.3f) ",
                    count++, currentEvent.keyDown - prevBeat
                )
                prevBeat = currentBeat.keyDown
                currentBeat = null
                first = false
            }
            if (currentBeat != null && currentBeat.keyDown > currentEvent.keyDown) {
                gap = currentBeat.keyDown - prevBeat
                gapCount = (gap / beatInterval).roundToInt()
                for (j in 1 until gapCount) {    //empty beat(s) before event
                    nextBeat = prevBeat + gap / gapCount
                    if (nextBeat >= currentEvent.keyDown) break
                    System.out.printf(
                        "\n%4d (%5.3f) [%7.3f ]",
                        count++, nextBeat - prevBeat, nextBeat
                    )
                    prevBeat = nextBeat
                }
                first = false
            }
            if (first) // for correct formatting of any initial (pre-beat) events
                print("\n                       ")
            System.out.printf(
                "%8.3f%c ", currentEvent.keyDown,
                if (abs(
                        currentEvent.scoreBeat / level -
                                (currentEvent.scoreBeat / level).roundToLong()
                    ) < 0.001
                ) '*' else ' '
            )
            first = false
        }
        println()
    } // showTracking()

    // showTracking()/1
    companion object {
        /**
         * The default value of innerMargin, which is the maximum time (in seconds) that a
         * beat can deviate from the predicted beat time without a fork occurring.
         */
        const val INNER_MARGIN = 0.040

        /**
         * The reactiveness/inertia balance, i.e. degree of change in the tempo, is controlled by the correctionFactor
         * variable.  This constant defines its default value, which currently is not subsequently changed. The
         * beat period is updated by the reciprocal of the correctionFactor multiplied by the difference between the
         * predicted beat time and matching onset.
         */
        const val DEFAULT_CORRECTION_FACTOR = 50.0

        /**
         * The default value of expiryTime, which is the time (in seconds) after which an Agent that
         * has no Event matching its beat predictions will be destroyed.
         */
        const val DEFAULT_EXPIRY_TIME = 10.0

        /**
         * Print debugging information
         */
        var debug = false

        /**
         * The maximum amount by which a beat can be later than the predicted beat time,
         * expressed as a fraction of the beat period.
         */
        var POST_MARGIN_FACTOR = 0.3

        /**
         * The maximum amount by which a beat can be earlier than the predicted beat time,
         * expressed as a fraction of the beat period.
         */
        var PRE_MARGIN_FACTOR = 0.15

        /**
         * The maximum allowed deviation from the initial tempo, expressed as a fraction of the initial beat period.
         */
        var MAX_CHANGE = 0.2

        /**
         * The slope of the penalty function for onsets which do not coincide precisely with predicted beat times.
         */
        var CONF_FACTOR = 0.5

        /**
         * The identity number of the next created Agent
         */
        protected var idCounter = 0

        /**
         * The maximum time (in seconds) that a beat can deviate from the predicted beat time
         * without a fork occurring (i.e. a 2nd Agent being created).
         */
        protected var innerMargin = 0.0

        /**
         * Controls the reactiveness/inertia balance, i.e. degree of change in the tempo.  The
         * beat period is updated by the reciprocal of the correctionFactor multiplied by the difference between the
         * predicted beat time and matching onset.
         */
        protected var correctionFactor = 0.0

        /**
         * The time (in seconds) after which an Agent that
         * has no Event matching its beat predictions will be destroyed.
         */
        protected var expiryTime = 0.0

        /**
         * For scoring Agents in a (non-existent) real-time version (otherwise not used).
         */
        protected var decayFactor = 0.0
    }
} // class Agent
