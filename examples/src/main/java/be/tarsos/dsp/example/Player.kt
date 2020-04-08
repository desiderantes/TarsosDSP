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
package be.tarsos.dsp.example

import be.tarsos.dsp.*
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters.Companion.slowdownDefaults
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromFile
import be.tarsos.dsp.io.jvm.AudioPlayer
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

class Player(
    private val beforeWSOLAProcessor: AudioProcessor,
    private val afterWSOLAProcessor: AudioProcessor
) : AudioProcessor {
    private val support = PropertyChangeSupport(this)
    private var state: PlayerState = PlayerState.NO_FILE_LOADED
    private var loadedFile: File? = null
    private var gainProcessor: GainProcessor? = null
    private var audioPlayer: AudioPlayer? = null
    private var wsola: WaveformSimilarityBasedOverlapAdd? = null
    private var dispatcher: AudioDispatcher? = null
    private var durationInSeconds = 0.0
    private var currentTime = 0.0
    private var pausedAt = 0.0
    private var gain: Double = 1.0
    private var tempo: Double = 1.0
    fun load(file: File?) {
        if (state != PlayerState.NO_FILE_LOADED) {
            eject()
        }
        loadedFile = file
        val fileFormat: AudioFileFormat
        fileFormat = try {
            AudioSystem.getAudioFileFormat(loadedFile)
        } catch (e: UnsupportedAudioFileException) {
            throw Error(e)
        } catch (e: IOException) {
            throw Error(e)
        }
        val format = fileFormat.format
        durationInSeconds = fileFormat.frameLength / format.frameRate.toDouble()
        pausedAt = 0.0
        currentTime = 0.0
        setState(PlayerState.FILE_LOADED)
    }

    fun eject() {
        loadedFile = null
        stop()
        setState(PlayerState.NO_FILE_LOADED)
    }

    fun play() {
        check(state != PlayerState.NO_FILE_LOADED) { "Can not play when no file is loaded" }
        if (state == PlayerState.PAUSED) {
            play(pausedAt)
        } else {
            play(0.0)
        }
    }

    fun play(startTime: Double) {
        check(state != PlayerState.NO_FILE_LOADED) { "Can not play when no file is loaded" }
        try {
            val fileFormat = AudioSystem.getAudioFileFormat(loadedFile)
            val format = fileFormat.format
            gainProcessor = GainProcessor(gain)
            audioPlayer = AudioPlayer(format)
            wsola = WaveformSimilarityBasedOverlapAdd(
                slowdownDefaults(
                    tempo,
                    format.sampleRate.toDouble()
                )
            )
            dispatcher = fromFile(
                loadedFile,
                wsola!!.inputBufferSize,
                wsola!!.overlap
            )
            wsola!!.setDispatcher(dispatcher)
            dispatcher!!.skip(startTime)
            dispatcher!!.addAudioProcessor(this)
            dispatcher!!.addAudioProcessor(beforeWSOLAProcessor)
            dispatcher!!.addAudioProcessor(wsola!!)
            dispatcher!!.addAudioProcessor(afterWSOLAProcessor)
            dispatcher!!.addAudioProcessor(gainProcessor!!)
            dispatcher!!.addAudioProcessor(audioPlayer!!)
            val t = Thread(dispatcher, "Audio Player Thread")
            t.start()
            setState(PlayerState.PLAYING)
        } catch (e: UnsupportedAudioFileException) {
            throw Error(e)
        } catch (e: IOException) {
            throw Error(e)
        } catch (e: LineUnavailableException) {
            throw Error(e)
        }
    }

    @JvmOverloads
    fun pause(pauseAt: Double = currentTime) {
        pausedAt = if (state == PlayerState.PLAYING || state == PlayerState.PAUSED) {
            setState(PlayerState.PAUSED)
            dispatcher!!.stop()
            pauseAt
        } else {
            throw IllegalStateException("Can not pauze when nothing is playing")
        }
    }

    fun stop() {
        if (state == PlayerState.PLAYING || state == PlayerState.PAUSED) {
            setState(PlayerState.STOPPED)
            dispatcher!!.stop()
        } else check(state == PlayerState.STOPPED) { "Can not stop when nothing is playing" }
    }

    fun setGain(newGain: Double) {
        gain = newGain
        if (state == PlayerState.PLAYING) {
            gainProcessor!!.gain = gain
        }
    }

    fun setTempo(newTempo: Double) {
        tempo = newTempo
        if (state == PlayerState.PLAYING) {
            wsola!!.applyNewParameters(
                slowdownDefaults(
                    tempo,
                    dispatcher!!.format.sampleRate.toDouble()
                )
            )
        }
    }

    fun getDurationInSeconds(): Double {
        check(state != PlayerState.NO_FILE_LOADED) { "No file loaded, unable to determine the duration in seconds" }
        return durationInSeconds
    }

    fun getState(): PlayerState {
        return state
    }

    private fun setState(newState: PlayerState) {
        val oldState = state
        state = newState
        support.firePropertyChange("state", oldState, newState)
    }

    fun addPropertyChangeListener(l: PropertyChangeListener?) {
        support.addPropertyChangeListener(l)
    }

    fun removePropertyChangeListener(l: PropertyChangeListener?) {
        support.removePropertyChangeListener(l)
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        currentTime = audioEvent.timeStamp
        return true
    }

    override fun processingFinished() {
        if (state == PlayerState.PLAYING) {
            setState(PlayerState.STOPPED)
        }
    }

    /**
     * Defines the state of the audio player.
     *
     * @author Joren Six
     */
    enum class PlayerState {
        /**
         * No file is loaded.
         */
        NO_FILE_LOADED,
        /**
         * A file is loaded and ready to be played.
         */
        FILE_LOADED,
        /**
         * The file is playing
         */
        PLAYING,
        /**
         * Audio play back is paused.
         */
        PAUSED,
        /**
         * Audio play back is stopped.
         */
        STOPPED
    }

}