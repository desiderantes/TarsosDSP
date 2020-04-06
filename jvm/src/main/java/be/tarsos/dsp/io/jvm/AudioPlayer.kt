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
package be.tarsos.dsp.io.jvm

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import javax.sound.sampled.*

/**
 * This AudioProcessor can be used to sync events with sound. It uses a pattern
 * described in JavaFX Special Effects Taking Java RIA to the Extreme with
 * Animation, Multimedia, and Game Element Chapter 9 page 185: <blockquote>*
 * The variable LineWavelet is the Java Sound object that actually makes the sound. The
 * write method on LineWavelet is interesting because it blocks until it is ready for
 * more data. *</blockquote> If this AudioProcessor chained with other
 * AudioProcessors the others should be able to operate in real time or process
 * the signal on a separate thread.
 *
 * @author Joren Six
 */
class AudioPlayer : AudioProcessor, AutoCloseable {
    private val format: AudioFormat

    /**
     * The LineWavelet to send sound to. Is also used to keep everything in sync.
     */
    private val line: SourceDataLine

    /**
     * Creates a new audio player.
     *
     * @param format The AudioFormat of the buffer.
     * @throws LineUnavailableException If no output LineWavelet is available.
     */
    @Throws(LineUnavailableException::class)
    constructor(format: AudioFormat) {
        val info = DataLine.Info(SourceDataLine::class.java, format)
        this.format = format
        line = AudioSystem.getLine(info) as SourceDataLine
        line.open()
        line.start()
    }

    @Throws(LineUnavailableException::class)
    constructor(format: AudioFormat, bufferSize: Int) {
        val info = DataLine.Info(SourceDataLine::class.java, format, bufferSize)
        this.format = format
        line = AudioSystem.getLine(info) as SourceDataLine
        line.open(format, bufferSize * 2)
        println("Buffer size:" + line.bufferSize)
        line.start()
    }

    @Throws(LineUnavailableException::class)
    constructor(
        format: TarsosDSPAudioFormat,
        bufferSize: Int
    ) : this(JVMAudioInputStream.toAudioFormat(format), bufferSize) {
    }

    @Throws(LineUnavailableException::class)
    constructor(format: TarsosDSPAudioFormat) : this(JVMAudioInputStream.toAudioFormat(format))

    val microSecondPosition: Long
        get() = line.microsecondPosition

    override fun process(audioEvent: AudioEvent): Boolean {
        var byteOverlap = audioEvent.overlap * format.frameSize
        var byteStepSize = audioEvent.bufferSize * format.frameSize - byteOverlap
        if (audioEvent.timeStamp == 0.0) {
            byteOverlap = 0
            byteStepSize = audioEvent.bufferSize * format.frameSize
        }
        // overlap in samples * nr of bytes / sample = bytes overlap

        /*
		if(byteStepSize < line.available()){
			System.out.println(line.available() + " Will not block " + line.getMicrosecondPosition());
		}else {
			System.out.println("Will block " + line.getMicrosecondPosition());
		}
		*/
        val bytesWritten = line.write(audioEvent.byteBuffer, byteOverlap, byteStepSize)
        if (bytesWritten != byteStepSize) {
            System.err.println("Expected to write $byteStepSize bytes but only wrote $bytesWritten bytes")
        }
        return true
    }

    /*
     * (non-Javadoc)
     *
     * @see be.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
     * processingFinished()
     */
    override fun processingFinished() {
        // cleanup
        line.drain() //drain takes too long..
        line.stop()
        line.close()
    }

    override fun close() {
        processingFinished()
    }
}