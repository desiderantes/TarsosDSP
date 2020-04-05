package be.tarsos.dsp.test;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;

public class JVMTestUtilities {

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a flute played double forte at A6 (theoretically 440Hz) without vibrato
     */
    public static float[] audioBufferFlute() {
        int lengthInSamples = 4096;
        String file = "/be/tarsos/dsp/test/resources/flute.novib.ff.A4.wav";
        return audioBufferFile(file, lengthInSamples);
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a flute played double forte at B6 (theoretically 1975.53Hz) without vibrato
     */
    public static float[] audioBufferHighFlute() {
        int lengthInSamples = 4096;
        String file = "/be/tarsos/dsp/test/resources/flute.novib.ff.B6.wav";
        return audioBufferFile(file, lengthInSamples);
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a piano played double forte at A4 (theoretically 440Hz)
     */
    public static float[] audioBufferPiano() {
        int lengthInSamples = 4096;
        String file = "/be/tarsos/dsp/test/resources/piano.ff.A4.wav";
        return audioBufferFile(file, lengthInSamples);
    }

    /**
     * @return a 4096 samples long 44.1kHz sampled float buffer with the sound
     * of a piano played double forte at C3 (theoretically 130.81Hz)
     */
    public static float[] audioBufferLowPiano() {
        int lengthInSamples = 4096;
        String file = "/be/tarsos/dsp/test/resources/piano.ff.C3.wav";
        return audioBufferFile(file, lengthInSamples);
    }

    private static float[] audioBufferFile(String file, int lengthInSamples) {
        float[] buffer = new float[lengthInSamples];
        try {
            final URL url = TestUtilities.class.getResource(file);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = audioStream.getFormat();
            TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(format));
            byte[] bytes = new byte[lengthInSamples * format.getSampleSizeInBits()];
            audioStream.read(bytes);
            converter.toFloatArray(bytes, buffer);
        } catch (IOException e) {
            throw new Error("Test audio file should be present.");
        } catch (UnsupportedAudioFileException e) {
            throw new Error("Test audio file format should be supported.");
        }
        return buffer;
    }
}
