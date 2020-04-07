package be.tarsos.dsp

class FadeIn // METHODS
// Constructor
// d=duration of the fade in in seconds
    (// VARIABLES
    private val duration: Double
) : AudioProcessor {
    private var firstTime = -1.0
    private var time = 0.0
    private val gp = GainProcessor(0.1)
    private var fadingIn = true

    // Stop fade in processing immediately
    fun stopFadeIn() {
        fadingIn = false
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        // Don't do anything after the end of the Fade In
        if (fadingIn) {
            if (firstTime == -1.0) firstTime = audioEvent.timeStamp


            // Increase the gain according to time since the beginning of the Fade In
            time = audioEvent.timeStamp - firstTime
            gp.setGain(time / duration)
            gp.process(audioEvent)
            if (time > duration) {
                fadingIn = false
            }
        }
        return true
    }

    override fun processingFinished() {
        gp.processingFinished()
    }

}