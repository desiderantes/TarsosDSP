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
package be.tarsos.dsp

class DetermineDurationProcessor : AudioProcessor {
    var durationInSamples: Long = 0
    var sampleRate = 0f
    var lastEvent: AudioEvent? = null
    override fun process(audioEvent: AudioEvent): Boolean {
        lastEvent = audioEvent
        return true
    }

    val durationInSeconds: Double = (durationInSamples / sampleRate).toDouble()

    override fun processingFinished() {
        sampleRate = lastEvent!!.sampleRate
        durationInSamples = lastEvent!!.samplesProcessed + lastEvent!!.floatBuffer.size
    }
}