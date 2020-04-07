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

import kotlin.math.sign

data class Event @JvmOverloads constructor(
    var keyDown: Double,
    var keyUp: Double,
    var pedalUp: Double,
    var midiPitch: Int,
    var midiVelocity: Int,
    var scoreBeat: Double,
    var scoreDuration: Double,
    var flags: Int,
    var midiCommand: Int = 144,
    var midiChannel: Int = 1,
    var midiTrack: Int = 0,
    var salience: Double = 0.0
) : Comparable<Event> {

    override fun compareTo(other: Event): Int {
        return sign(keyDown - other.keyDown).toInt()
    }

    override fun toString(): String {
        return "n=" + midiPitch + " v=" + midiVelocity + " t=" + keyDown +
                " to " + keyUp + " (" + pedalUp + ")"
    }

    fun print(f: Flags?) {
        System.out.printf("Event:\n")
        System.out.printf(
            "\tkeyDown / Up / pedalUp: %5.3f / %5.3f /  %5.3f\n",
            keyDown, keyUp, pedalUp
        )
        //System.out.printf("\tkeyUp: %5.3f\n", keyUp);
        //System.out.printf("\tpedalUp: %5.3f\n", pedalUp);
        System.out.printf("\tmidiPitch: %d\n", midiPitch)
        System.out.printf("\tmidiVelocity: %d\n", midiVelocity)
        System.out.printf("\tmidiCommand: %02x\t", midiCommand or midiChannel)
        //System.out.printf("\tmidiChannel: %d\n", midiChannel);
        System.out.printf("\tmidiTrack: %d\n", midiTrack)
        System.out.printf("\tsalience: %5.3f\t", salience)
        System.out.printf("\tscoreBeat: %5.3f\t", scoreBeat)
        System.out.printf("\tscoreDuration: %5.3f\n", scoreDuration)
        System.out.printf("\tflags: %X", flags)
        if (f != null) {
            var ff = flags
            var i = 0
            while (ff != 0) {
                if (ff % 2 == 1) print(" " + f.getLabel(i))
                ff = ff ushr 1
                i++
            }
        }
        print("\n\n")
    }

}
