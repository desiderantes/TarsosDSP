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

class MultichannelToMono(private val channels: Int, private val mean: Boolean) : AudioProcessor {
    override fun process(audioEvent: AudioEvent): Boolean {
        val buffer = audioEvent.floatBuffer
        val newBuffer = FloatArray(buffer.size / channels)
        if (mean) {
            if (channels == 2) {
                var i = 0
                while (i < buffer.size) {
                    newBuffer[i / channels] = (buffer[i] + buffer[i + 1]) / 2.0f
                    i += channels
                }
            } else {
                var i = 0
                while (i < buffer.size) {
                    var sum = 0.0
                    for (j in 0 until channels) {
                        sum += buffer[i + j]
                    }
                    newBuffer[i / channels] = (sum / channels).toFloat()
                    i += channels
                }
            }
        } else {
            var i = 0
            while (i < buffer.size) {
                newBuffer[i / channels] = buffer[i]
                i += channels
            }
        }
        audioEvent.floatBuffer = newBuffer
        return true
    }

    override fun processingFinished() {}

}