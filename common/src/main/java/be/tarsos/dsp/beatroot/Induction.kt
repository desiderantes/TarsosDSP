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
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Performs tempo induction by finding clusters of similar
 * inter-onset intervals (IOIs), ranking them according to the number
 * of intervals and relationships between them, and returning a set
 * of tempo hypotheses for initialising the beat tracking agents.
 */
object Induction {
    /**
     * The maximum difference in IOIs which are in the same cluster
     */
    var clusterWidth = 0.025

    /**
     * The minimum IOI for inclusion in a cluster
     */
    var minIOI = 0.070

    /**
     * The maximum IOI for inclusion in a cluster
     */
    var maxIOI = 2.500

    /**
     * The minimum inter-beat interval (IBI), i.e. the maximum tempo
     * hypothesis that can be returned.
     * 0.30 seconds == 200 BPM
     * 0.25 seconds == 240 BPM
     */
    var minIBI = 0.3

    /**
     * The maximum inter-beat interval (IBI), i.e. the minimum tempo
     * hypothesis that can be returned.
     * 1.00 seconds ==  60 BPM
     * 0.75 seconds ==  80 BPM
     * 0.60 seconds == 100 BPM
     */
    var maxIBI = 1.0 //  60BPM	// was 0.75 =>  80

    /**
     * The maximum number of tempo hypotheses to return
     */
    var topN = 10

    /**
     * Flag to enable debugging output
     */
    var debug = false

    /**
     * Performs tempo induction (see JNMR 2001 paper by Simon Dixon for details).
     *
     * @param events The onsets (or other events) from which the tempo is induced
     * @return A list of beat tracking agents, where each is initialised with one
     * of the top tempo hypotheses but no beats
     */
    fun beatInduction(events: EventList): AgentList {
        var i: Int
        var j: Int
        var b: Int
        var submult: Boolean
        var intervals = 0 // number of interval clusters
        val bestn = IntArray(topN) // count of high-scoring clusters
        var ratio: Double
        var err: Double
        var degree: Int
        val maxClusterCount = ceil((maxIOI - minIOI) / clusterWidth).toInt()
        val clusterMean = DoubleArray(maxClusterCount)
        val clusterSize = IntArray(maxClusterCount)
        val clusterScore = IntArray(maxClusterCount)
        val ptr1: ListIterator<Event>
        var ptr2: ListIterator<Event?>
        var e1: Event
        var e2: Event
        ptr1 = events.listIterator()
        while (ptr1.hasNext()) {
            e1 = ptr1.next()
            ptr2 = events.listIterator()
            e2 = ptr2.next()
            while (e2 != e1) e2 = ptr2.next()
            while (ptr2.hasNext()) {
                e2 = ptr2.next()
                val ioi = e2.keyDown - e1.keyDown
                if (ioi < minIOI) // skip short intervals
                    continue
                if (ioi > maxIOI) // ioi too long
                    break
                b = 0
                while (b < intervals) {
                    // assign to nearest cluster
                    if (abs(clusterMean[b] - ioi) < clusterWidth) {
                        if (b < intervals - 1 && abs(clusterMean[b + 1] - ioi) <
                            abs(clusterMean[b] - ioi)
                        ) b++ // next cluster is closer
                        clusterMean[b] = (clusterMean[b] * clusterSize[b] + ioi) /
                                (clusterSize[b] + 1)
                        clusterSize[b]++
                        break
                    }
                    b++
                }
                if (b == intervals) {    // no suitable cluster; create new one
                    if (intervals == maxClusterCount) {
                        System.err.println("Warning: Too many clusters")
                        continue  // ignore this IOI
                    }
                    intervals++
                    while (b > 0 && clusterMean[b - 1] > ioi) {
                        clusterMean[b] = clusterMean[b - 1]
                        clusterSize[b] = clusterSize[b - 1]
                        b--
                    }
                    clusterMean[b] = ioi
                    clusterSize[b] = 1
                }
            }
        }
        if (debug) { // output IOI histogram in Matlab format
            println(
                """
                    Inter-onset interval histogram:
                    StartMatlabCode
                    ioi = [
                    """.trimIndent()
            )
            b = 0
            while (b < intervals) {
                System.out.printf(
                    "%4d %7.3f %7d\n",
                    b, clusterMean[b], clusterSize[b]
                )
                b++
            }
            println("]; ioiclusters(ioi, name);\nEndMatlabCode\n")
        }
        b = 0
        while (b < intervals) {
            // merge similar intervals
            // TODO: they are now in order, so don't need the 2nd loop
            // TODO: check BOTH sides before averaging or upper gps don't work
            i = b + 1
            while (i < intervals) {
                if (abs(clusterMean[b] - clusterMean[i]) < clusterWidth) {
                    clusterMean[b] = (clusterMean[b] * clusterSize[b] +
                            clusterMean[i] * clusterSize[i]) /
                            (clusterSize[b] + clusterSize[i])
                    clusterSize[b] = clusterSize[b] + clusterSize[i]
                    --intervals
                    j = i + 1
                    while (j <= intervals) {
                        clusterMean[j - 1] = clusterMean[j]
                        clusterSize[j - 1] = clusterSize[j]
                        j++
                    }
                }
                i++
            }
            b++
        }
        if (intervals == 0) return AgentList()
        b = 0
        while (b < intervals) {
            clusterScore[b] = 10 * clusterSize[b]
            b++
        }
        bestn[0] = 0
        var bestCount: Int = 1
        b = 0
        while (b < intervals) {
            i = 0
            while (i <= bestCount) {
                if (i < topN && (i == bestCount ||
                            clusterScore[b] > clusterScore[bestn[i]])
                ) {
                    if (bestCount < topN) bestCount++
                    j = bestCount - 1
                    while (j > i) {
                        bestn[j] = bestn[j - 1]
                        j--
                    }
                    bestn[i] = b
                    break
                }
                i++
            }
            b++
        }
        if (debug) {
            println("Best $bestCount clusters (before):")
            b = 0
            while (b < bestCount) {
                System.out.printf(
                    "%5.3f : %5d\n", clusterMean[bestn[b]],
                    clusterScore[bestn[b]]
                )
                b++
            }
        }
        b = 0
        while (b < intervals) {
            // score intervals
            i = b + 1
            while (i < intervals) {
                ratio = clusterMean[b] / clusterMean[i]
                submult = ratio < 1
                degree = if (submult) (1 / ratio).roundToInt() else ratio.roundToInt()
                if (degree in 2..8) {
                    err =
                        if (submult) abs(clusterMean[b] * degree - clusterMean[i]) else abs(clusterMean[b] - clusterMean[i] * degree)
                    if (err < (if (submult) clusterWidth else clusterWidth * degree)) {
                        degree = if (degree >= 5) 1 else 6 - degree
                        clusterScore[b] += degree * clusterSize[i]
                        clusterScore[i] += degree * clusterSize[b]
                    }
                }
                i++
            }
            b++
        }
        if (debug) {
            println("Best $bestCount clusters (after):")
            b = 0
            while (b < bestCount) {
                System.out.printf(
                    "%5.3f : %5d\n", clusterMean[bestn[b]],
                    clusterScore[bestn[b]]
                )
                b++
            }
        }
        if (debug) {
            println("Inter-onset interval histogram 2:")
            b = 0
            while (b < intervals) {
                System.out.printf(
                    "%3d: %5.3f : %3d (score: %5d)\n",
                    b, clusterMean[b], clusterSize[b], clusterScore[b]
                )
                b++
            }
        }
        val a = AgentList()
        for (index in 0 until bestCount) {
            b = bestn[index]
            // Adjust it, using the size of super- and sub-intervals
            var newSum = clusterMean[b] * clusterScore[b]
            //int newCount = clusterSize[b];
            var newWeight = clusterScore[b]
            i = 0
            while (i < intervals) {
                if (i == b) {
                    i++
                    continue
                }
                ratio = clusterMean[b] / clusterMean[i]
                if (ratio < 1) {
                    degree = (1 / ratio).roundToInt()
                    if (degree in 2..8) {
                        err = abs(clusterMean[b] * degree - clusterMean[i])
                        if (err < clusterWidth) {
                            newSum += clusterMean[i] / degree * clusterScore[i]
                            //newCount += clusterSize[i];
                            newWeight += clusterScore[i]
                        }
                    }
                } else {
                    degree = ratio.roundToInt()
                    if (degree in 2..8) {
                        err = abs(clusterMean[b] - degree * clusterMean[i])
                        if (err < clusterWidth * degree) {
                            newSum += clusterMean[i] * degree * clusterScore[i]
                            //newCount += clusterSize[i];
                            newWeight += clusterScore[i]
                        }
                    }
                }
                i++
            }
            var beat = newSum / newWeight
            // Scale within range ... hope the grouping isn't ternary :(
            while (beat < minIBI) // Maximum speed
                beat *= 2.0
            while (beat > maxIBI) // Minimum speed
                beat /= 2.0
            if (beat >= minIBI) {
                a.add(Agent(beat))
                if (debug) System.out.printf(" %5.3f", beat)
            }
        }
        if (debug) println(" IBI")
        return a
    } // beatInduction()

    /**
     * For variable cluster widths in newInduction().
     *
     * @param low The lowest IOI allowed in the cluster
     * @return The highest IOI allowed in the cluster
     */
    internal fun top(low: Int): Int {
        return low + 25 // low/10;
    } // top()

    /**
     * An alternative (incomplete) tempo induction method (not used).
     * Uses integer (millisecond) resolution.
     *
     * @param events The events on which tempo induction is performed
     */
    fun newInduction(events: EventList) {
        val MAX_MS = 2500
        val count = IntArray(MAX_MS)
        for (i in 0 until MAX_MS) count[i] = 0
        val ptr1: ListIterator<Event>
        var ptr2: ListIterator<Event?>
        var e1: Event
        var e2: Event
        ptr1 = events.listIterator()
        while (ptr1.hasNext()) {
            e1 = ptr1.next()
            ptr2 = events.listIterator()
            e2 = ptr2.next()
            while (e2 != e1) e2 = ptr2.next()
            while (ptr2.hasNext()) {
                e2 = ptr2.next()
                val diff = ((e1.keyDown - e2.keyDown) * 1000).roundToInt()
                if (diff < MAX_MS) count[diff]++ else break
            }
        }
        val MAX_CL = 10
        val cluster = IntArray(MAX_CL)
        val csize = IntArray(MAX_CL)
        var clnum: Int = 0
        while (clnum < MAX_CL) {
            var sum = 0
            var max = 0
            var maxp = 0
            var hi = 70
            var lo = hi
            while (hi < MAX_MS) {
                if (hi >= top(lo)) sum -= count[lo++] else {
                    sum += count[hi++]
                    if (sum > max) {
                        max = sum
                        maxp = lo
                    }
                }
            }
            if (max == 0) break
            hi = top(maxp)
            if (hi > MAX_MS) hi = MAX_MS
            sum = 0
            var cnt = sum
            lo = maxp
            while (lo < hi) {
                sum += lo * count[lo]
                cnt += count[lo]
                count[lo] = 0
                lo++
            }
            if (cnt != max) System.err.println("Rounding error in newInduction")
            cluster[clnum] = sum / cnt
            csize[clnum] = cnt
            System.out.printf(" %5.3f", sum / 1000.0 / cnt)
            clnum++
        }
        println(" IBI")
    }
}
