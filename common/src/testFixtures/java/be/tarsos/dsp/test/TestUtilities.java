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


package be.tarsos.dsp.test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.stream.Collectors;


public class TestUtilities {

    /**
     * Constructs and returns a buffer of a two seconds long pure sine of 440Hz
     * sampled at 44.1kHz.
     *
     * @return A buffer of a two seconds long pure sine (440Hz) sampled at
     * 44.1kHz.
     */
    public static float[] audioBufferSine() {
        final double sampleRate = 44100.0;
        final double f0 = 440.0;
        final double amplitudeF0 = 0.5;
        final double seconds = 4.0;
        final float[] buffer = new float[(int) (seconds * sampleRate)];
        for (int sample = 0; sample < buffer.length; sample++) {
            final double time = sample / sampleRate;
            buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0 * time));
        }
        return buffer;
    }

    public static float[] audioBufferSine(int numberOfSamples) {
        final double sampleRate = 44100.0;
        final double f0 = 440.0;
        final double amplitudeF0 = 0.5;
        final float[] buffer = new float[numberOfSamples];
        for (int sample = 0; sample < buffer.length; sample++) {
            final double time = sample / sampleRate;
            buffer[sample] = (float) (amplitudeF0 * Math.sin(2 * Math.PI * f0 * time));
        }
        return buffer;
    }


    public static InputStream fluteFile() {
        String file = "flute.novib.ff.A4.wav";

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(file);
    }

    public static InputStream ccirFile() {
        String file = "CCIR_04221.ogg";
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(file);
    }

    public static InputStream onsetsAudioFile() {
        String file = "NR45.wav";
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(file);
    }

    public static InputStream sineOf4000Samples() {
        String file = "4000_samples_of_440Hz_at_44.1kHz.wav";
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(file);
    }

    public static File sineOf4000SamplesFile(){
        String file = "4000_samples_of_440Hz_at_44.1kHz.wav";
        try {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            return Paths.get(classLoader.getResource(file).toURI()).toFile();
        }catch (Exception e) {
            return null;
        }
    }


    /**
     * Reads the contents of a file.
     *
     * @param name the name of the file to read
     * @return the contents of the file if successful, an empty string
     * otherwise.
     */
    public static String readFile(final String name) {
        FileReader fileReader = null;
        final StringBuilder contents = new StringBuilder();
        try {
            final File file = new File(name);
            if (!file.exists()) {
                throw new IllegalArgumentException("File " + name + " does not exist");
            }
            fileReader = new FileReader(file);
            final BufferedReader reader = new BufferedReader(fileReader);
            String inputLine = reader.readLine();
            while (inputLine != null) {
                contents.append(inputLine).append("\n");
                inputLine = reader.readLine();
            }
            reader.close();
        } catch (final IOException i1) {
            throw new RuntimeException(i1);
        }
        return contents.toString();
    }

    /**
     * Reads the contents of a file in a jar.
     *
     * @param path the path to read e.g. /package/name/here/help.html
     * @return the contents of the file when successful, an empty string
     * otherwise.
     */
    public static String readFileFromJar(final String path) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(path)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * @return a half a second long silent buffer (all zeros), at 44.1kHz.
     */
    public static float[] audioBufferSilence() {
        final double sampleRate = 44100.0;
        final double seconds = 0.5;
        return new float[(int) (seconds * sampleRate)];
    }
}
