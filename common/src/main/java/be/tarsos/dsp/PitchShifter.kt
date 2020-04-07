package be.tarsos.dsp

import be.tarsos.dsp.util.PI
import be.tarsos.dsp.util.TWO_PI
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.cos
import kotlin.math.sin

/**
 * This is a translation of code by Stephan M. Bernsee. See the following explanation on this code:
 * [Pitch shifting using the STFT](http://www.dspdimension.com/admin/pitch-shifting-using-the-ft/).
 *
 * @author Joren Six
 * @author Stephan M. Bernsee
 */
class PitchShifter(
    private var pitchShiftRatio: Double,
    private val sampleRate: Double,
    private val size: Int,
    overlap: Int
) :
    AudioProcessor {
    private val fft: FFT = FFT(size)
    private val currentMagnitudes: FloatArray = FloatArray(size / 2)
    private val currentPhase: FloatArray = FloatArray(size / 2)
    private val currentFrequencies: FloatArray = FloatArray(size / 2)
    private val outputAccumulator: FloatArray = FloatArray(size * 2)
    private val summedPhase: FloatArray = FloatArray(size / 2)
    private val previousPhase: FloatArray = FloatArray(size / 2)
    private val osamp: Long = size / (size - overlap).toLong()
    private val excpt: Double = TWO_PI * (size - overlap).toDouble() / size.toDouble()

    override fun process(audioEvent: AudioEvent): Boolean {
        //see http://downloads.dspdimension.com/smbPitchShift.cpp

        /* ***************** ANALYSIS ******************* */
        val fftData = audioEvent.floatBuffer.clone()
        for (i in 0 until size) {
            val window =
                (-.5 * cos(2.0 * PI * i.toDouble() / size.toDouble()) + .5).toFloat()
            fftData[i] = window * fftData[i]
        }
        //Fourier transform the audio
        fft.forwardTransform(fftData)
        //Calculate the magnitudes and phase information.
        fft.powerAndPhaseFromFFT(fftData, currentMagnitudes, currentPhase)
        val freqPerBin =
            (sampleRate / size.toFloat()).toFloat() // distance in Hz between FFT bins
        for (i in 0 until size / 2) {
            val phase = currentPhase[i]

            /* compute phase difference */
            var tmp = phase - previousPhase[i].toDouble()
            previousPhase[i] = phase

            /* subtract expected phase difference */tmp -= i.toDouble() * excpt

            /* map delta phase into +/- Pi interval */
            var qpd = (tmp / PI).toLong()
            if (qpd >= 0) qpd += qpd and 1 else qpd -= qpd and 1
            tmp -= Math.PI * qpd.toDouble()

            /* get deviation from bin frequency from the +/- Pi interval */tmp = osamp * tmp / (2.0 * Math.PI)

            /* compute the k-th partials' true frequency */tmp = i.toDouble() * freqPerBin + tmp * freqPerBin

            /* store magnitude and true frequency in analysis arrays */currentFrequencies[i] = tmp.toFloat()
        }

        /* ***************** PROCESSING ******************* */
        /* this does the actual pitch shifting */
        val newMagnitudes = FloatArray(size / 2)
        val newFrequencies = FloatArray(size / 2)
        for (i in 0 until size / 2) {
            val index = (i * pitchShiftRatio).toInt()
            if (index < size / 2) {
                newMagnitudes[index] += currentMagnitudes[i]
                newFrequencies[index] = (currentFrequencies[i] * pitchShiftRatio).toFloat()
            }
        }

        ///Synthesis****
        val newFFTData = FloatArray(size)
        for (i in 0 until size / 2) {
            val magn = newMagnitudes[i]
            var tmp = newFrequencies[i].toDouble()

            /* subtract bin mid frequency */
            tmp -= i.toDouble() * freqPerBin

            /* get bin deviation from freq deviation */
            tmp /= freqPerBin.toDouble()

            /* take osamp into account */
            tmp = TWO_PI * tmp / osamp

            /* add the overlap phase advance back in */
            tmp += i.toDouble() * excpt

            /* accumulate delta phase to get bin phase */
            summedPhase[i] += tmp.toFloat()
            val phase = summedPhase[i]

            /* get real and imag part and re-interleave */
            newFFTData[2 * i] = (magn * cos(phase.toDouble())).toFloat()
            newFFTData[2 * i + 1] = (magn * sin(phase.toDouble())).toFloat()
        }

        /* zero negative frequencies */for (i in size / 2 + 2 until size) {
            newFFTData[i] = 0f
        }
        fft.backwardsTransform(newFFTData)
        for (i in newFFTData.indices) {
            val window =
                (-.5 * cos(2.0 * Math.PI * i.toDouble() / size.toDouble()) + .5).toFloat()
            //outputAccumulator[i] += 2000*window*newFFTData[i]/(float) (size*osamp);
            outputAccumulator[i] += window * newFFTData[i] / osamp.toFloat()
            if (outputAccumulator[i] > 1.0 || outputAccumulator[i] < -1.0) {
                System.err.println("Clipping!")
            }
        }
        val stepSize = (size / osamp).toInt()


        //Arrays.fill(audioBuffer, 0);
        System.arraycopy(outputAccumulator, stepSize, outputAccumulator, 0, size)
        val audioBuffer = FloatArray(audioEvent.floatBuffer.size)
        audioEvent.floatBuffer = audioBuffer
        System.arraycopy(outputAccumulator, 0, audioBuffer, size - stepSize, stepSize)
        return true
    }

    override fun processingFinished() {}
}