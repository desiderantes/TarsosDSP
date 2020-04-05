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

package be.tarsos.dsp.example;

import be.tarsos.dsp.AudioGenerator;
import be.tarsos.dsp.effects.DelayEffect;
import be.tarsos.dsp.effects.FlangerEffect;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.synthesis.AmplitudeLFO;
import be.tarsos.dsp.synthesis.NoiseGenerator;
import be.tarsos.dsp.synthesis.SineGenerator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

/**
 * Shows how a synthesizer can be constructed using some simple ugen blocks.
 *
 * @author Joren Six
 */
public class SynthesisExample implements TarsosDSPDemo {

    public static void main(String... args) throws LineUnavailableException {
        new SynthesisExample().start(args);
    }

    @Override
    public String getName() {
        return "Synthesis Example";
    }

    @Override
    public String getDescription() {
        return "Shows how a synthesizer can be constructed using some simple ugen blocks.";
    }

    @Override
    public void start(String... args) {
        AudioGenerator generator = new AudioGenerator(1024, 0);
        generator.addAudioProcessor(new NoiseGenerator(0.2));
        generator.addAudioProcessor(new LowPassFS(1000, 44100));
        generator.addAudioProcessor(new LowPassFS(1000, 44100));
        generator.addAudioProcessor(new LowPassFS(1000, 44100));
        generator.addAudioProcessor(new SineGenerator(0.05, 220));
        generator.addAudioProcessor(new AmplitudeLFO(10, 0.9));
        generator.addAudioProcessor(new SineGenerator(0.2, 440));
        generator.addAudioProcessor(new SineGenerator(0.1, 880));
        generator.addAudioProcessor(new DelayEffect(1.5, 0.4, 44100));
        generator.addAudioProcessor(new AmplitudeLFO());
        generator.addAudioProcessor(new NoiseGenerator(0.02));
        generator.addAudioProcessor(new SineGenerator(0.05, 1760));
        generator.addAudioProcessor(new SineGenerator(0.01, 2460));
        generator.addAudioProcessor(new AmplitudeLFO(0.1, 0.7));
        generator.addAudioProcessor(new DelayEffect(0.757, 0.4, 44100));
        generator.addAudioProcessor(new FlangerEffect(0.1, 0.2, 44100, 4));
        try {
            generator.addAudioProcessor(new AudioPlayer(new AudioFormat(44100, 16, 1, true, false)));
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        generator.run();
    }
}
