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

/**
 * The oscilloscope generates a float array with
 * array[i] an x coordinate in percentage
 * array[i+1] the value of the amplitude in audio buffer
 * array[i+2] another x coordinate in percentage
 * array[i+3] the next amplitude in the audio buffer
 *
 *
 * The implementation is based on the one by Dan Ellis found at http://www.ee.columbia.edu/~dpwe/resources/Processing/
 *
 * @author Dan Ellis
 * @author Joren Six
 */
class Oscilloscope(private val handler: OscilloscopeEventHandler) : AudioProcessor {
    var dataBuffer: FloatArray? = null
    override fun process(audioEvent: AudioEvent): Boolean {
        val audioBuffer = audioEvent.floatBuffer
        var offset = 0
        var maxdx = 0f
        for (i in 0 until audioBuffer.size / 4) {
            val dx = audioBuffer[i + 1] - audioBuffer[i]
            if (dx > maxdx) {
                offset = i
                maxdx = dx
            }
        }
        val tbase = audioBuffer.size / 2.toFloat()
        val length = tbase.toInt().coerceAtMost(audioBuffer.size - offset)
        if (dataBuffer == null || dataBuffer!!.size != length * 4) {
            dataBuffer = FloatArray(length * 4)
        }
        var j = 0
        for (i in 0 until length - 1) {
            val x1 = i / tbase
            val x2 = i / tbase
            dataBuffer!![j] = x1
            dataBuffer!![j + 1] = audioBuffer[i + offset]
            dataBuffer!![j + 2] = x2
            dataBuffer!![j + 3] = audioBuffer[i + 1 + offset]
            j += 4
        }
        handler.handleEvent(dataBuffer!!, audioEvent)
        return true
    }

    override fun processingFinished() {}

    @FunctionalInterface
    interface OscilloscopeEventHandler {
        /**
         * @param data  The data contains a float array with:
         * array[i] an x coordinate in percentage
         * array[i+1] the value of the amplitude in audio buffer
         * array[i+2] another x coordinate in percentage
         * array[i+3] the next amplitude in the audio buffer
         * @param event An audio Event.
         */
        fun handleEvent(data: FloatArray, event: AudioEvent)
    }

}