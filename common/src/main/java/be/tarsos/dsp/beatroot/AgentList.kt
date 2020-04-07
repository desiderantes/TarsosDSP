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

/**
 * Class for maintaining the set of all Agents involved in beat tracking a piece of music.
 * Implements a simple linked list terminated by an AgentList with a null Agent (ag).
 */
class AgentList @JvmOverloads constructor(
    /**
     * A beat tracking Agent
     */
    var ag: Agent? = null,
    /**
     * The remainder of the linked list
     */
    var next: AgentList? = null
) {

    /**
     * Deep print of AgentList for debugging
     */
    fun print() {
        println("agentList.print: (size=$count)")
        var ptr: AgentList? = this
        while (ptr!!.ag != null) {
            ptr.ag!!.print(2)
            ptr = ptr.next
        }
        println("End of agentList.print()")
    }

    /**
     * Inserts newAgent into the list in ascending order of beatInterval
     *
     * @param a
     */
    fun add(a: Agent?) {
        add(a, true)
    }

    /**
     * Appends newAgent to list (sort==false), or inserts newAgent into the list
     * in ascending order of beatInterval
     *
     * @param newAgent The agent to be added to the list
     * @param sort     Flag indicating whether the list is sorted or not
     */
    fun add(newAgent: Agent?, sort: Boolean) {
        if (newAgent == null) return
        var ptr: AgentList?
        count++
        ptr = this
        while (ptr!!.ag != null) {
            if (sort && newAgent.beatInterval <= ptr.ag!!.beatInterval) {
                ptr.next = AgentList(ptr.ag, ptr.next)
                ptr.ag = newAgent
                return
            }
            ptr = ptr.next
        }
        ptr.next = AgentList()
        ptr.ag = newAgent
    }

    /**
     * Sorts the AgentList by increasing beatInterval, using a bubble sort
     * since it is assumed that the list is almost sorted.
     */
    fun sort() {
        var sorted = false
        while (!sorted) {
            sorted = true
            var ptr: AgentList? = this
            while (ptr!!.ag != null) {
                if (ptr.next!!.ag != null &&
                    ptr.ag!!.beatInterval > ptr.next!!.ag!!.beatInterval
                ) {
                    val temp = ptr.ag
                    ptr.ag = ptr.next!!.ag
                    ptr.next!!.ag = temp
                    sorted = false
                }
                ptr = ptr.next
            }
        }
    }

    /**
     * Removes the current item from the list.
     * The current item does not need to be the head of the whole list.
     *
     * @param ptr Points to the Agent which is removed from the list
     */
    fun remove(ptr: AgentList?) {
        count--
        ptr!!.ag = ptr.next!!.ag // null-terminated list always has next
        ptr.next = ptr.next!!.next
    }

    /**
     * Removes Agents from the list which are duplicates of other Agents.
     * A duplicate is defined by the tempo and phase thresholds
     * thresholdBI and thresholdBT respectively.
     */
    protected fun removeDuplicates() {
        sort()
        run {
            var ptr: AgentList? = this
            while (ptr!!.ag != null) {
                if (ptr!!.ag!!.phaseScore < 0.0) {
                    ptr = ptr!!.next
                    // already flagged for deletion
                    continue
                }
                var ptr2 = ptr!!.next
                while (ptr2!!.ag != null) {
                    if (ptr2.ag!!.beatInterval - ptr!!.ag!!.beatInterval > thresholdBI) break
                    if (abs(ptr!!.ag!!.beatTime - ptr2.ag!!.beatTime) > thresholdBT) {
                        ptr2 = ptr2.next
                        continue
                    }
                    if (ptr!!.ag!!.phaseScore < ptr2.ag!!.phaseScore) {
                        ptr!!.ag!!.phaseScore = -1.0 // flag for deletion
                        if (ptr2.ag!!.topScoreTime < ptr!!.ag!!.topScoreTime) ptr2.ag!!.topScoreTime =
                            ptr!!.ag!!.topScoreTime
                        break
                    } else {
                        ptr2.ag!!.phaseScore = -1.0 // flag for deletion
                        if (ptr!!.ag!!.topScoreTime < ptr2.ag!!.topScoreTime) ptr!!.ag!!.topScoreTime =
                            ptr2.ag!!.topScoreTime
                    }
                    ptr2 = ptr2.next
                }
                ptr = ptr!!.next
            }
        }
        var ptr: AgentList? = this
        while (ptr!!.ag != null) {
            if (ptr!!.ag!!.phaseScore < 0.0) {
                remove(ptr)
            } else ptr = ptr!!.next
        }
    } // removeDuplicates()
    /**
     * Perform beat tracking on a list of events (onsets).
     *
     * @param el   The list of onsets (or events or peaks) to beat track.
     * @param stop Do not find beats after `stop` seconds.
     */
    /**
     * Perform beat tracking on a list of events (onsets).
     *
     * @param el The list of onsets (or events or peaks) to beat track
     */
    @JvmOverloads
    fun beatTrack(el: EventList, stop: Double = -1.0) {
        val ptr: ListIterator<Event> = el.listIterator()
        val phaseGiven = ag != null &&
                ag!!.beatTime >= 0 // if given for one, assume given for others
        while (ptr.hasNext()) {
            val ev = ptr.next()
            if (stop > 0 && ev.keyDown > stop) break
            var created = phaseGiven
            var prevBeatInterval = -1.0
            var ap: AgentList? = this
            while (ap!!.ag != null) {
                val currentAgent = ap.ag
                if (currentAgent!!.beatInterval != prevBeatInterval) {
                    if (prevBeatInterval >= 0 && !created && ev.keyDown < 5.0) {
                        // Create new agent with different phase
                        val newAgent = Agent(prevBeatInterval)
                        newAgent.considerAsBeat(ev, this)
                        add(newAgent)
                    }
                    prevBeatInterval = currentAgent.beatInterval
                    created = phaseGiven
                }
                if (currentAgent.considerAsBeat(ev, this)) created = true
                if (currentAgent != ap.ag) // new one been inserted, skip it
                    ap = ap.next
                ap = ap!!.next
            }
            removeDuplicates()
        } // loop for each event
    } // beatTrack()
    // beatTrack()/1
    /**
     * Finds the Agent with the highest score in the list.
     *
     * @return The Agent with the highest score
     */
    fun bestAgent(): Agent? {
        var best = -1.0
        var bestAg: Agent? = null
        var ap: AgentList? = this
        while (ap!!.ag != null) {
            val startTime = ap.ag!!.events.l.first.keyDown
            val conf = (ap.ag!!.phaseScore + ap.ag!!.tempoScore) /
                    if (useAverageSalience) ap.ag!!.beatCount.toDouble() else 1.0
            if (conf > best) {
                bestAg = ap.ag
                best = conf
            }
            if (debug) {
                ap.ag!!.print(0)
                System.out.printf(
                    " +%5.3f    Av-salience = %3.1f\n",
                    startTime, conf
                )
            }
            ap = ap.next
        }
        if (debug) {
            if (bestAg != null) {
                print("Best ")
                bestAg.print(0)
                System.out.printf("    Av-salience = %5.1f\n", best)
                // bestAg.events.print();
            } else println("No surviving agent - beat tracking failed")
        }
        return bestAg
    } // bestAgent()

    companion object {
        /**
         * For the purpose of removing duplicate agents, the default JND of IBI
         */
        const val DEFAULT_BI = 0.02

        /**
         * For the purpose of removing duplicate agents, the default JND of phase
         */
        const val DEFAULT_BT = 0.04

        /**
         * Flag for choice between sum and average beat salience values for Agent scores.
         * The use of summed saliences favours faster tempi or lower metrical levels.
         */
        var useAverageSalience = false

        /**
         * Flag for printing debugging output.
         */
        var debug = false

        /**
         * The length of the list (number of beat tracking Agents)
         */
        var count = 0

        /**
         * For the purpose of removing duplicate agents, the JND of IBI.
         * Not changed in the current version.
         */
        var thresholdBI = DEFAULT_BI

        /**
         * For the purpose of removing duplicate agents, the JND of phase.
         * Not changed in the current version.
         */
        var thresholdBT = DEFAULT_BT
    }
    /**
     * Constructor for an AgentList: the Agent a is prepended to the list al.
     *
     * @param ag  The Agent at the head of the list
     * @param next The tail of the list
     */
    /**
     * Default constructor
     */
    init {
        if (next == null) {
            if (ag != null) next = AgentList() // insert null-terminator if it was forgotten
            else {
                count = 0
                thresholdBI = DEFAULT_BI
                thresholdBT = DEFAULT_BT
            }
        }
    }
}
