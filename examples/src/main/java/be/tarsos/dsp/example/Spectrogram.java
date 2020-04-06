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

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.util.fft.FFT;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Spectrogram extends JFrame implements PitchDetectionHandler, TarsosDSPDemo {

    /**
     *
     */
    private static final long serialVersionUID = 1383896180290138076L;
    private final SpectrogramPanel panel;
    private AudioDispatcher dispatcher;
    private Mixer currentMixer;
    private PitchEstimationAlgorithm algo;
    private double pitch;

    private float sampleRate = 44100;
    private int bufferSize = 1024 * 4;
    AudioProcessor fftProcessor = new AudioProcessor() {

        FFT fft = new FFT(bufferSize);
        float[] amplitudes = new float[bufferSize / 2];

        @Override
        public void processingFinished() {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean process(AudioEvent audioEvent) {
            float[] audioFloatBuffer = audioEvent.getFloatBuffer();
            float[] transformbuffer = new float[bufferSize * 2];
            System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
            fft.forwardTransform(transformbuffer);
            fft.modulus(transformbuffer, amplitudes);
            panel.drawFFT(pitch, amplitudes, fft);
            panel.repaint();
            return true;
        }

    };
    private int overlap = 768 * 4;
    private String fileName;
    private ActionListener algoChangeListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            String name = e.getActionCommand();
            PitchEstimationAlgorithm newAlgo = PitchEstimationAlgorithm.valueOf(name);
            algo = newAlgo;
            try {
                setNewMixer(currentMixer);
            } catch (LineUnavailableException | UnsupportedAudioFileException e1) {
                e1.printStackTrace();
            }
        }
    };


    public Spectrogram(String fileName) {
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Spectrogram");
        panel = new SpectrogramPanel();
        algo = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
        this.fileName = fileName;

        JPanel pitchDetectionPanel = new PitchDetectionPanel(algoChangeListener);

        JPanel inputPanel = new InputPanel();

        inputPanel.addPropertyChangeListener("mixer",
                changeEvent -> {
                    try {
                        setNewMixer((Mixer) changeEvent.getNewValue());
                    } catch (LineUnavailableException | UnsupportedAudioFileException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                });

        JPanel containerPanel = new JPanel(new GridLayout(1, 0));
        containerPanel.add(inputPanel);
        containerPanel.add(pitchDetectionPanel);
        this.add(containerPanel, BorderLayout.NORTH);

        JPanel otherContainer = new JPanel(new BorderLayout());
        otherContainer.add(panel, BorderLayout.CENTER);
        otherContainer.setBorder(new TitledBorder("3. Utter a sound (whistling works best)"));


        this.add(otherContainer, BorderLayout.CENTER);
    }

    public static void main(final String... args) throws InterruptedException,
            InvocationTargetException {
        (args.length == 0 ? new Spectrogram(null) : new Spectrogram(args[0])).start(args);
    }

    private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

        if (dispatcher != null) {
            dispatcher.stop();
        }
        if (fileName == null) {
            final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
                    false);
            final DataLine.Info dataLineInfo = new DataLine.Info(
                    TargetDataLine.class, format);
            TargetDataLine line;
            line = (TargetDataLine) mixer.getLine(dataLineInfo);
            final int numberOfSamples = bufferSize;
            line.open(format, numberOfSamples);
            line.start();
            final AudioInputStream stream = new AudioInputStream(line);

            JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
            // create a new dispatcher
            dispatcher = new AudioDispatcher(audioStream, bufferSize,
                    overlap);
        } else {
            try {
                File audioFile = new File(fileName);
                dispatcher = AudioDispatcherFactory.fromFile(audioFile, bufferSize, overlap);
                AudioFormat format = AudioSystem.getAudioFileFormat(audioFile).getFormat();
                dispatcher.addAudioProcessor(new AudioPlayer(format));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        currentMixer = mixer;

        // add a processor, handle pitch event.
        dispatcher.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, this));
        dispatcher.addAudioProcessor(fftProcessor);

        // run the dispatcher (on a new thread).
        new Thread(dispatcher, "Audio dispatching").start();
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        if (pitchDetectionResult.isPitched()) {
            pitch = pitchDetectionResult.getPitch();
        } else {
            pitch = -1;
        }

    }


    @Override
    public String getDescription() {
        return "Spectrogram Example";
    }

    @Override
    public void start(String... args) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager
                            .getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // ignore failure to set default look en feel;
                }
                JFrame frame = this;
                frame.pack();
                frame.setSize(640, 480);
                frame.setVisible(true);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
