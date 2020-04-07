package be.tarsos.dsp

class FadeOut(
    private val duration: Double
) : AudioProcessor {
    private var firstTime = -1.0
    private var time = 0.0

    /**
     * Set to true to start
     */
    var isFadeOut = false
    private val gp = GainProcessor(0.9)

    override fun process(audioEvent: AudioEvent): Boolean {
        // Don't do anything before the beginning of Fade Out
        if (isFadeOut) {
            if (firstTime == -1.0) firstTime = audioEvent.timeStamp

            // Decrease the gain according to time since the beginning of the Fade Out
            time = audioEvent.timeStamp - firstTime
            gp.setGain(1 - time / duration)
            gp.process(audioEvent)
        }
        return true
    }

    override fun processingFinished() {
        gp.processingFinished()
    }

}