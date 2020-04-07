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
package be.tarsos.dsp.pitch

import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/* dywapitchtrack.c

Dynamic Wavelet Algorithm Pitch Tracking library
Released under the MIT open source licence

Copyright (c) 2010 Antoine Schmitt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
/**
 *
 *
 * The pitch is the main frequency of the waveform (the 'note' being played or
 * sung). It is expressed as a float in Hz.
 *
 * Unlike the human ear, pitch detection is difficult to achieve for computers.
 * Many algorithms have been designed and experimented, but there is no 'best'
 * algorithm. They all depend on the context and the tradeoffs acceptable in
 * terms of speed and latency. The context includes the quality and 'cleanness'
 * of the audio : obviously polyphonic sounds (multiple instruments playing
 * different notes at the same time) are extremely difficult to track,
 * percussive or noisy audio has no pitch, most real-life audio have some noisy
 * moments, some instruments have a lot of harmonics, etc...
 *
 *
 * The dywapitchtrack is based on a custom-tailored algorithm which is of very
 * high quality: both very accurate (precision < 0.05 semitones), very low
 * latency (< 23 ms) and very low error rate. It has been thoroughly tested on
 * human voice.
 *
 *
 *
 * It can best be described as a dynamic wavelet algorithm (dywa):
 *
 *
 *
 * The heart of the algorithm is a very powerful wavelet algorithm, described in
 * a paper by Eric Larson and Ross Maddox [](http://online.physics.uiuc)/courses/phys498pom/NSF_REU_Reports/2005_reu/Real
 * -Time_Time-Domain_Pitch_Tracking_Using_Wavelets.pdf">"Real-Time Time-Domain
 * Pitch Tracking Using Wavelets
 *
 *
 * @author Antoine Schmitt
 * @author Joren Six
 */
class DynamicWavelet(private val sampleRate: Float, bufferSize: Int) : PitchDetector {
    // algorithm parameters
    private val maxFLWTlevels = 6
    private val maxF = 3000.0
    private val differenceLevelsN = 3
    private val maximaThresholdRatio = 0.75

    /**
     * The result of the pitch detection iteration.
     */
    private val result: PitchDetectionResult = PitchDetectionResult()
    var distances: IntArray = IntArray(bufferSize)
    var mins: IntArray = IntArray(bufferSize)
    var maxs: IntArray = IntArray(bufferSize)
    override fun getPitch(audioBuffer: FloatArray): PitchDetectionResult {
        var audioBuffer = audioBuffer
        var pitchF = -1.0f
        var curSamNb = audioBuffer.size
        var nbMins: Int
        var nbMaxs: Int

        //check if the buffer size changed
        if (distances.size == audioBuffer.size) {
            //if not fill the arrays with zero
            Arrays.fill(distances, 0)
            Arrays.fill(mins, 0)
            Arrays.fill(maxs, 0)
        } else {
            //otherwise create new ones
            distances = IntArray(audioBuffer.size)
            mins = IntArray(audioBuffer.size)
            maxs = IntArray(audioBuffer.size)
        }
        val ampltitudeThreshold: Double
        var theDC = 0.0


        //compute ampltitudeThreshold and theDC
        //first compute the DC and maxAMplitude
        var maxValue = 0.0
        var minValue = 0.0
        for (i in audioBuffer.indices) {
            val sample = audioBuffer[i].toDouble()
            theDC += sample
            maxValue = max(maxValue, sample)
            minValue = min(sample, minValue)
        }
        theDC /= audioBuffer.size
        maxValue -= theDC
        minValue -= theDC
        val amplitudeMax = if (maxValue > -minValue) maxValue else -minValue
        ampltitudeThreshold = amplitudeMax * maximaThresholdRatio

        // levels, start without downsampling..
        var curLevel = 0
        var curModeDistance = -1.0
        var delta: Int

        //TODO: refactor to make this more java, break it up in methods, remove the wile and branching statements...
        search@ while (true) {
            delta = (sampleRate / (2.0.pow(curLevel.toDouble()) * maxF)).toInt()
            if (curSamNb < 2) break@search

            // compute the first maximums and minumums after zero-crossing
            // store if greater than the min threshold
            // and if at a greater distance than delta
            var dv: Double
            var previousDV = -1000.0
            nbMaxs = 0
            nbMins = nbMaxs
            var lastMinIndex = -1000000
            var lastmaxIndex = -1000000
            var findMax = false
            var findMin = false
            for (i in 2 until curSamNb) {
                val si = audioBuffer[i] - theDC
                val si1 = audioBuffer[i - 1] - theDC
                if (si1 <= 0 && si > 0) findMax = true
                if (si1 >= 0 && si < 0) findMin = true

                // min or max ?
                dv = si - si1
                if (previousDV > -1000) {
                    if (findMin && previousDV < 0 && dv >= 0) {

                        // minimum
                        if (abs(si) >= ampltitudeThreshold) {
                            if (i > lastMinIndex + delta) {
                                mins[nbMins++] = i
                                lastMinIndex = i
                                findMin = false
                            }
                        }
                    }
                    if (findMax && previousDV > 0 && dv <= 0) {
                        // maximum
                        if (abs(si) >= ampltitudeThreshold) {
                            if (i > lastmaxIndex + delta) {
                                maxs[nbMaxs++] = i
                                lastmaxIndex = i
                                findMax = false
                            }
                        }
                    }
                }
                previousDV = dv
            }
            if (nbMins == 0 && nbMaxs == 0) {
                // no best distance !
                //asLog("dywapitch no mins nor maxs, exiting\n");

                // if DEBUGG then put "no mins nor maxs, exiting"
                break@search
            }
            var d: Int
            Arrays.fill(distances, 0)
            for (i in 0 until nbMins) {
                for (j in 1 until differenceLevelsN) {
                    if (i + j < nbMins) {
                        d = abs(mins[i] - mins[i + j])
                        //asLog("dywapitch i=%ld j=%ld d=%ld\n", i, j, d);
                        distances[d] = distances[d] + 1
                    }
                }
            }
            var bestDistance = -1
            var bestValue = -1
            for (i in 0 until curSamNb) {
                var summed = 0
                for (j in -delta..delta) {
                    if (i + j in 0 until curSamNb) summed += distances[i + j]
                }
                //asLog("dywapitch i=%ld summed=%ld bestDistance=%ld\n", i, summed, bestDistance);
                if (summed == bestValue) {
                    if (i == 2 * bestDistance) bestDistance = i
                } else if (summed > bestValue) {
                    bestValue = summed
                    bestDistance = i
                }
            }

            // averaging
            var distAvg = 0.0
            var nbDists = 0.0
            for (j in -delta..delta) {
                if (bestDistance + j >= 0 && bestDistance + j < audioBuffer.size) {
                    val nbDist = distances[bestDistance + j]
                    if (nbDist > 0) {
                        nbDists += nbDist.toDouble()
                        distAvg += (bestDistance + j) * nbDist.toDouble()
                    }
                }
            }

            // this is our mode distance !
            distAvg /= nbDists
            //asLog("dywapitch distAvg=%f\n", distAvg);

            // continue the levels ?
            if (curModeDistance > -1.0) {
                val similarity = abs(distAvg * 2 - curModeDistance)
                if (similarity <= 2 * delta) {
                    //if DEBUGG then put "similarity="&similarity&&"delta="&delta&&"ok"
                    //asLog("dywapitch similarity=%f OK !\n", similarity);
                    // two consecutive similar mode distances : ok !
                    pitchF = (sampleRate / (2.0.pow(curLevel - 1.toDouble()) * curModeDistance)).toFloat()
                    break@search
                }
                //if DEBUGG then put "similarity="&similarity&&"delta="&delta&&"not"
            }

            // not similar, continue next level
            curModeDistance = distAvg
            curLevel += 1
            if (curLevel >= maxFLWTlevels) {
                // put "max levels reached, exiting"
                //asLog("dywapitch max levels reached, exiting\n");
                break@search
            }

            // downsample
            if (curSamNb < 2) {
                //asLog("dywapitch not enough samples, exiting\n");
                break@search
            }
            //do not modify original audio buffer, make a copy buffer, if
            //downsampling is needed (only once).
            var newAudioBuffer = audioBuffer
            if (curSamNb == distances.size) {
                newAudioBuffer = FloatArray(curSamNb / 2)
            }
            for (i in 0 until curSamNb / 2) {
                newAudioBuffer[i] = (audioBuffer[2 * i] + audioBuffer[2 * i + 1]) / 2.0f
            }
            audioBuffer = newAudioBuffer
            curSamNb /= 2
        }
        result.pitch = pitchF
        result.isPitched = -1f != pitchF
        result.probability = -1f
        return result
    }
}