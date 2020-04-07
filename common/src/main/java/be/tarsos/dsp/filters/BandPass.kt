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
 *  Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package be.tarsos.dsp.filters

import kotlin.math.cos

/**
 * A band pass filter is a filter that filters out all frequencies except for
 * those in a band centered on the current frequency of the filter.
 *
 * @author Damien Di Fede
 */
class BandPass(freq: Float, bandWidth: Float, sampleRate: Float) :
    IIRFilter(freq, sampleRate) {
    private var bw = bandWidth

    /**
     * Returns the band width of this filter.
     *
     * @return the band width (in Hz)
     */
    /**
     * Sets the band width of the filter. Doing this will cause the coefficients
     * to be recalculated.
     *
     * @param bandWidth the band width (in Hz)
     */
    var bandWidth: Float
        get() = bw * sampleRate
        set(bandWidth) {
            bw = bandWidth / sampleRate
            calcCoeff()
        }

    override fun calcCoeff() {
        val R = 1 - 3 * bw
        val fracFreq = getFrequency() / sampleRate
        val T = 2 * cos(2 * Math.PI * fracFreq).toFloat()
        val K = (1 - R * T + R * R) / (2 - T)
        a = floatArrayOf(1 - K, (K - R) * T, R * R - K)
        b = floatArrayOf(R * T, -R * R)
    }
}