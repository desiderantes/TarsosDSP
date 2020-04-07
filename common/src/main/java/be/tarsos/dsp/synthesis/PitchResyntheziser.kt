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
package be.tarsos.dsp.synthesis

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.EnvelopeFollower
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import java.util.*
import kotlin.math.sin

/**
 * This pitch detection handler replaces the audio buffer in the pipeline with a
 * synthesized wave. It either follows the envelope of the original signal or
 * not. Use it wisely. The following demonstrates how it can be used.
 *
 * <pre>
 * `
 * PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.FFT_YIN;
 * PitchResyntheziser prs = new PitchResyntheziser(samplerate);
 * AudioDispatcher dispatcher = AudioDispatcher.fromFile(new File("in.wav"),1024, 0);
 * //Handle pitch detection
 * dispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, prs));
 * //Write the synthesized pitch to an output file.
 * dispatcher.addAudioProcessor(new WaveformWriter(format, "out.wav"));//
 * dispatcher.run();
` *
</pre> *
 *
 * @author Joren Six
 */
class PitchResyntheziser @JvmOverloads constructor(
    samplerate: Float,
    followEnvelope: Boolean = true,
    pureSine: Boolean = false,
    filterSize: Int = 5
) : PitchDetectionHandler {
    private val envelopeFollower: EnvelopeFollower = EnvelopeFollower(samplerate.toDouble(), 0.005, 0.01)
    private val previousFrequencies: DoubleArray = DoubleArray(filterSize)
    private var phase = 0.0
    private var phaseFirst = 0.0
    private var phaseSecond = 0.0
    private var prevFrequency = 0.0
    private val samplerate: Float = samplerate
    private val usePureSine: Boolean = pureSine
    private val followEnvelope: Boolean = followEnvelope
    private var previousFrequencyIndex: Int = 0
    override fun handlePitch(
        pitchDetectionResult: PitchDetectionResult,
        audioEvent: AudioEvent
    ) {
        var frequency = pitchDetectionResult.pitch.toDouble()
        if (frequency == -1.0) {
            frequency = prevFrequency
        } else {
            if (previousFrequencies.isNotEmpty()) {
                //median filter
                //store and adjust pointer
                previousFrequencies[previousFrequencyIndex] = frequency
                previousFrequencyIndex++
                previousFrequencyIndex %= previousFrequencies.size
                //sort to get median frequency
                val frequenciesCopy = previousFrequencies.clone()
                Arrays.sort(frequenciesCopy)
                //use the median as frequency
                frequency = frequenciesCopy[frequenciesCopy.size / 2]
            }
            prevFrequency = frequency
        }
        val twoPiF = 2 * Math.PI * frequency
        val audioBuffer = audioEvent.floatBuffer
        var envelope: FloatArray? = null
        if (followEnvelope) {
            envelope = audioBuffer.clone()
            envelopeFollower.calculateEnvelope(envelope)
        }
        for (sample in audioBuffer.indices) {
            val time = sample / samplerate.toDouble()
            var wave = sin(twoPiF * time + phase)
            if (!usePureSine) {
                wave += 0.05 * sin(twoPiF * 4 * time + phaseFirst)
                wave += 0.01 * sin(twoPiF * 8 * time + phaseSecond)
            }
            audioBuffer[sample] = wave.toFloat()
            if (followEnvelope) {
                audioBuffer[sample] = audioBuffer[sample] * envelope!![sample]
            }
        }
        val timefactor = twoPiF * audioBuffer.size / samplerate
        phase += timefactor
        if (!usePureSine) {
            phaseFirst += 4 * timefactor
            phaseSecond += 8 * timefactor
        }
    }

}