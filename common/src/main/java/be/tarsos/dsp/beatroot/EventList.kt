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
/*
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
	http://www.gnu.org/licenses/gpl.txt or write to the
	Free Software Foundation, Inc.,
	51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package be.tarsos.dsp.beatroot

import java.io.*
import java.util.*

// Adapted from eventList::readMatchFile in beatroot/src/eventMidi.cpp
// Reads in a Prolog score+performance (.match) file; returns it as an eventList
// Lines in the match file can be of the form:
//		hammer_bounce-PlayedNote.
//		info(Attribute, Value).
//		insertion-PlayedNote.
//		ornament(Anchor)-PlayedNote.
//		ScoreNote-deletion.
//		ScoreNote-PlayedNote.
//		ScoreNote-trailing_score_note.
//		trailing_played_note-PlayedNote.
//		trill(Anchor)-PlayedNote.
// where ScoreNote is of the form
//		snote(Anchor,[NoteName,Modifier],Octave,Bar:Beat,Offset,Duration,
//				BeatNumber,DurationInBeats,ScoreAttributesList)
//		e.g. snote(n1,[b,b],5,1:1,0,3/16,0,0.75,[s])
// and PlayedNote is of the form
//		note(Number,[NoteName,Modifier],Octave,Onset,Offset,AdjOffset,Velocity)
//		e.g. note(1,[a,#],5,5054,6362,6768,53)
internal class WormFileParseException(s: String?) // constructor
    : RuntimeException(s) {
    companion object {
        const val serialVersionUID: Long = 0
    }
} // class WormFileParseException

internal class MatchFileParseException(s: String?) // constructor
    : RuntimeException(s) {
    companion object {
        const val serialVersionUID: Long = 0
    }
} // class MatchFileParseException

internal class BTFileParseException(s: String?) // constructor
    : RuntimeException(s) {
    companion object {
        const val serialVersionUID: Long = 0
    }
} // class BTFileParseException

// Process the strings which label extra features of notes in match files.
// We assume no more than 32 distinct labels in a file.
class Flags {
    var labels = arrayOfNulls<String>(32)
    var size = 0
    fun getFlag(s: String?): Int {
        if (s == null || s == "") return 0
        //int val = 1;
        for (i in 0 until size) if (s == labels[i]) return 1 shl i
        if (size == 32) {
            System.err.println("Overflow: Too many flags: $s")
            size--
        }
        labels[size] = s
        return 1 shl size++
    } // getFlag()

    fun getLabel(i: Int): String? {
        return if (i >= size) "ERROR: Unknown flag" else labels[i]
    } // getLabel()
} // class Flags

// A score/match/midi file is represented as an EventList object,
//  which contains pointers to the head and tail links, and some
//  class-wide parameters. Parameters are class-wide, as it is
//  assumed that the Worm has only one input file at a time.
class EventList() {
    @JvmField
    var l: LinkedList<Event> = LinkedList()

    constructor(e: EventList) : this() {
        val it = e.listIterator()
        while (it.hasNext()) add(it.next())
    } // constructor

    constructor(e: Array<Event>) : this() {
        for (event in e) {
            add(event)
        }
    } // constructor

    fun add(e: Event) {
        l.add(e)
    } // add()

    fun add(ev: EventList) {
        l.addAll(ev.l)
    } // add()

    fun insert(newEvent: Event, uniqueTimes: Boolean) {
        val li = l.listIterator()
        while (li.hasNext()) {
            val sgn = newEvent.compareTo(li.next())
            if (sgn < 0) {
                li.previous()
                break
            } else if (uniqueTimes && sgn == 0) {
                li.remove()
                break
            }
        }
        li.add(newEvent)
    } // insert()

    fun listIterator(): MutableListIterator<Event> {
        return l.listIterator()
    } // listIterator()

    operator fun iterator(): Iterator<Event> {
        return l.iterator()
    } // iterator()

    fun size(): Int {
        return l.size
    } // size()

    fun toOnsetArray(): DoubleArray {
        val d = DoubleArray(l.size)
        var i = 0
        val it: Iterator<Event> = l.iterator()
        while (it.hasNext()) {
            d[i] = it.next().keyDown
            i++
        }
        return d
    } // toOnsetArray()

    @JvmOverloads
    fun toArray(match: Int = 0): Array<Event?> {
        var count = 0
        for (e in l) if (match == 0 || e.midiCommand == match) count++
        val a = arrayOfNulls<Event>(count)
        var i = 0
        for (e in l) if (match == 0 || e.midiCommand == match) a[i++] = e
        return a
    } // toArray()

    // toArray()
    fun writeBinary(fileName: String) {
        try {
            val oos = ObjectOutputStream(
                FileOutputStream(fileName)
            )
            oos.writeObject(this)
            oos.close()
        } catch (e: IOException) {
            System.err.println(e)
        }
    }

    fun print() {
        for (event in l) {
            event.print(flags)
        }
    }

    companion object {
        val UNKNOWN = Double.NaN
        protected var timingCorrection = false
        protected var timingDisplacement = 0.0
        protected var clockUnits = 480
        protected var clockRate = 500000
        protected var metricalLevel = 0.0
        protected var noMelody = false
        protected var onlyMelody = false
        protected var flags = Flags()
        fun readBinary(fileName: String): EventList? {
            return try {
                val ois = ObjectInputStream(
                    FileInputStream(fileName)
                )
                val e = ois.readObject() as EventList
                ois.close()
                e
            } catch (e: IOException) {
                System.err.println(e)
                null
            } catch (e: ClassNotFoundException) {
                System.err.println(e)
                null
            }
        } // readBinary()

        fun setTimingCorrection(corr: Double) {
            timingCorrection = corr >= 0
            timingDisplacement = corr
        } // setTimingCorrection()
    }

} // class EventList
