# TarsosDSP

TarsosDSP is a Java library for audio processing. Its aim is to provide an easy-to-use interface to practical music processing algorithms
implemented, as simply as possible, in pure Java and without any other external dependencies. The library tries to hit the sweet spot between
being capable enough to get real tasks done but compact and simple enough to serve as a demonstration on how DSP algorithms works.
TarsosDSP features an implementation of a percussion onset detector and a number of pitch detection algorithms: YIN, the Mcleod Pitch method and
a “Dynamic Wavelet Algorithm Pitch Tracking” algorithm. Also included is a Goertzel DTMF (Dual tone multi frequency) decoding algorithm, a time
stretch algorithm (WSOLA), resampling, filters, simple synthesis, some audio effects, and a pitch shifting algorithm.

To show the capabilities of the library, [TarsosDSP example applications](http://0110.be/tag/TarsosDSP) are available. Head over to
the [TarsosDSP release directory](http://0110.be/releases/TarsosDSP/) for freshly baked binaries and code smell free (that is the goal
anyway), oven-fresh sources.

Some information about TarsosDSP can be found in the paper [*TarsosDSP, a Real-Time Audio Processing Framework in
Java*](http://0110.be/files/attachments/411/aes53_tarsos_dsp.pdf), by Joren Six, Olmo Cornelis, and Marc Leman, in *Proceedings of the 53rd
AES Conference (AES 53rd)*, 2014. If you use TarsosDSP in academic research, please cite this paper.

```latex
    @inproceedings{six2014tarsosdsp,
      author      = {Joren Six and Olmo Cornelis and Marc Leman},
      title       = {{TarsosDSP, a Real-Time Audio Processing Framework in Java}},
      booktitle   = {{Proceedings of the 53rd AES Conference (AES 53rd)}}, 
      year        =  2014
    }
```

## Quickly Getting Started with TarsosDSP

Head over to the [TarsosDSP release repository](http://0110.be/releases/TarsosDSP/) and download the latest
[TarsosDSP library](http://0110.be/releases/TarsosDSP/TarsosDSP-latest/TarsosDSP-latest.jar).
Include the Jar-file in your project and you are ready to go. To get up to speed quickly, check the [TarsosDSP Example
applications](http://0110.be/releases/TarsosDSP/TarsosDSP-latest/TarsosDSP-latest-Examples/) for inspiration and consult the [API
documentation](http://0110.be/releases/TarsosDSP/TarsosDSP-latest/TarsosDSP-latest-Documentation/).

Every [release of TarsosDSP](http://0110.be/releases/TarsosDSP/) contains the following:

  - `TarsosDSP-x.x-Documentation/` JavaDoc documentation
  - `TarsosDSP-x.x-Examples/` Example applications.
  - `TarsosDSP-Android-x.x-bin.jar` Jar library for inclusion in Android
    projects, withouth source files included.
  - `TarsosDSP-Android-x.x.jar` Jar library for inclusion in Android
    projects, with source files included.
  - `TarsosDSP-x.x-Manual.pdf` A manual describing the core concepts of
    TarsosDSP.
  - `TarsosDSP-x.x-Readme.html` This readme.
  - `TarsosDSP-x.x-bin.jar` Jar library for inclusion in Java projects,
    withouth source files included.
  - `TarsosDSP-x.x.jar` Jar library for inclusion in Java projects, with
    source files included.

The source code of TarsosDSP are compatible with Java 1.6. The jar files are compiled for 1.7.

## TarsosDSP on Android

If you want to do audio processing on Android TarsosDSP is a great fit. The main distribution has no dependencies on `javax.sound.xxx` and does
work well on Android by default. To add TarsosDSP to an Android Studio project download the latest [TarsosDSP Android
release](http://0110.be/releases/TarsosDSP/TarsosDSP-latest/TarsosDSP-Android-latest.jar) and place it in the `/app/libs/` folder of your project. By default
Gradle includes jar files placed in that directory in your classpath.

To check if this is the case look for the following lines in the `build.gradle` file in the module `app`:

    <code>dependencies {
        compile fileTree(dir: 'libs', include: ['*.jar'])
    }
    </code>

For futher information, please consult [TarsosDSP on Android - Audio Processing in Java on
Android](http://0110.be/posts/TarsosDSP_on_Android_-_Audio_Processing_in_Java_on_Android).

## TarsosDSP Example Applications

TarsosDSP contains some ready made example applications. Most have a Java Swing user interface. They show which functionality is present in
the library and how to use it.

  - [SoundDetector](http://0110.be/artikels/lees/TarsosDSP%253A_a_small_JAVA_audio_processing_library)
    show how you loudness calculations can be done. When input sound is over a defined limit an event is fired.
  - [PitchDetector](http://0110.be/artikels/lees/TarsosDSP%253A_a_small_JAVA_audio_processing_library)
    this demo application shows real-time pitch detection. When pitch is detected the hertz value is printed together with a probability.
  - [PercussionDetector](http://0110.be/artikels/lees/TarsosDSP%253A_a_small_JAVA_audio_processing_library)
    show the percussion (onset) dectection. Clapping your hands causes an event. This demo application also shows the influence of the two
    parameters on the algorithm.
  - [UtterAsterisk](http://0110.be/artikels/lees/TarsosDSP_sample_application%253A_Utter_Asterisk)
    a game with the goal to sing as close to a melody a possible. Technically it shows real-time pitch detection with YIN or MPM.
  - [Spectrogram in Java](http://0110.be/artikels/lees/Spectrogram_in_Java_with_TarsosDSP)
    shows a spectrogram and detected pitch, either live or from an audio file. It is interesting to see which frequencies are picked as fundamentals.
  - [Goertzel DTMF (Dual tone multi frequency) decoding](http://0110.be/artikels/lees/Dual-Tone_Multi-Frequency_%2528DTMF%2529_Decoding_with_the_Goertzel_Algorithm_in_Java)
    an implementation of the Goertzel Algorithm. A fancy user interface shows what goes on under the hood.
  - [Audio Time Stretching - Implementation in Pure Java Using WSOLA](http://0110.be/artikels/lees/Audio_Time_Stretching_-_Implementation_in_Pure_Java_Using_WSOLA)
    an implementation of a time stretching algorithm. WSOLA (Waveform Similarity Overlap Add) makes it possible to change the play back
    speed of audio without changing the pitch. The play back speed can be changed at any moment, even when there is audio playing.
  - [Audio Feature Extraction](http://0110.be/releases/TarsosDSP/TarsosDSP-1.3/TarsosDSP-1.3-Examples/FeatureExtractor-1.3.jar)
    a command line application to do simple feature extraction.
  - [Audio Synthesis](http://0110.be/releases/TarsosDSP/TarsosDSP-1.3/TarsosDSP-1.3-Examples/Synthesis-1.3.jar)
    a command line application to do simple audio synthesis.
  - [Pitch Shifting](http://0110.be/artikels/lees/Pitch_Shifting_-_Implementation_in_Pure_Java_with_Resampling_and_Time_Stretching)
    an example application that does pitch shifting, either in real-time on a microphone input, or on recorded audio. Also included is a command line application to do pitch shifting. 

## Developing TarsosDSP

If you want to build from source, or want to improve TarsosDSP follow the instructions below. Contributions to TarsosDSP are more than
welcome, if you have an algorithm to add or find a bug, do not hesitate to send me a message.

TarsosDSP uses [Apache Ant](http://ant.apache.org/) as a build system. The instructions below detail how you can build from source. When
everything runs correctly you should be able to run all example applications and have the latest version of the TarsosDSP library for
inclusion in your projects. Also the Javadoc documentation for the API should be available.

### TarsosDSP with Ant

To you need [Apache Ant](http://ant.apache.org/) and [git](http://git-scm.com/) installed on your system. The following
commands fetch the source and build the library and example jars:

    <code>git clone https://JorenSix@github.com/JorenSix/TarsosDSP.git
    cd TarsosDSP
    cd build
    ant tarsos_dsp_library #Builds the core TarsosDSP library
    ant build_examples #Builds all the TarsosDSP examples
    ant javadoc #Creates the documentation in TarsosDSP/doc
    </code>

## Source Code Organization & Developing

The library is separated into five source folders: 1) the main core functionality in `src/core`, TarsosDSP example applications in
`src/examples`, unit tests in `src/test`, JVM audio I/O in `src/jvm` and Android audio I/O in `src/android`.

  - `src` contains the source files of the DSP library. 
      - `src/core` contains the main core classes.
      - `src/test` contains unit tests for some of the DSP functionality.
      - `src/examples` contains a couple of example applications with a Java Swing user interface.
      - `src/android` contains the source files for audio I/O on Android. It is dependent on the Android Runtime.
      - `src/jvm` contains the source files for audio I/O on JRE. It is dependent on the Java Runtime Environment.
      - `src/patcher` I/O for patcher environments like pure data and Max/MSP. It shows how TarsosDSP and pd, MaxMSP can connect.
  - `build` contains ANT build files. Either to build Java documentation or runnable JAR-files for the example applications.
  - `lib` although the TarsosDSP core does not require any external dependencies the `lib` folder does contain two jar-file to easily
    run unit-tests (JUnit 4 and Hamcrest). It also contains a pure data library when one wants to develop for the pure data environment.

To make development with Eclipse easy, make sure the subfolders of `src` are marked as “source folder”, and not the `src` folder itself (as is
usually the case). For Android development exclude the `jvm` folder and include `android` and link to an Android runtime of your choosing.

## Credits

[TarsosDSP](http://0110.be/tag/TarsosDSP) was developed at [University College Ghent, School of Arts](http://schoolofartsgent.be) between 2009
and 2013, from late 2013 the project is supported by [University Ghent, IPEM](http://www.ipem.ugent.be).

The TarsosDSP borrows algorithms from various other libraries or research paper. Below a complete list of credits can be found.

  - The onset detector implementation is based on a [VAMP plugin example](http://vamp-plugins.org/code-doc/PercussionOnsetDetector_8cpp-source.html)
    by *Chris Cannam* at Queen Mary University, London. The method is described in [Drum Source Separation using Percussive Feature
    Detection and Spectral Modulation](http://eprints.nuim.ie/699/1/ELE-Bob9.pdf) by Dan Barry, Derry Fitzgerald, Eugene Coyle and Bob Lawlor, ISSC 2005.
  - For the implementation of the YIN pitch tracking algorithm. Both the [the YIN paper](http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf)
    and the GPL’d [aubio implementation](http://aubio.org) were used as a reference. *Matthias Mauch* (of Queen Mary University, London)
    kindly provided the FastYin implementation which uses an FFT to calculate the difference function, it makes the algorithm up to 3
    times faster.
  - The Average Magnitude Difference (AMDF) pitch estimation algorithm is implemented by *Eder Souza* and adapted for TarsosDSP by myself.
  - For the MPM pitch tracking algorithm, the paper titled [A Smarter Way To Find Pitch](http://miracle.otago.ac.nz/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf)
    by *Philip McLeod* and *Geoff Wyvill* was used. 
  - The Dynamic Wavlet pitch estimation algorithm is described in [Real-Time Time-Domain Pitch Tracking Using Wavelets](http://online.physics.uiuc.edu/courses/phys193/NSF_REU_Reports/2005_reu/Real-Time_Time-Domain_Pitch_Tracking_Using_Wavelets.pdf)
    by Eric Larson and Ross Maddox. The implementation within TarsosDSP is based on the implementation in the [Dynamic Wavelet Algorithm Pitch Tracking library](http://www.schmittmachine.com/dywapitchtrack.html) by
    *Antoine Schmitt*, which is released under the MIT open source license, a license compatible with the GPL.
  - The audio time stretching algorithm is described in [An Overlap-Add Technique Based on Waveform Similarity (WSOLA) For Hight Quality
    Time-Scale Modifications of speech](http://mir.cs.nthu.edu.tw/users/litbee/RelatedPaper/[WSOLA]An%20overlap-add%20technique%20based%20on%20waveform%20similarity%20\(WSOLA\)%20for%20high-quality%20time-scale%20modifications%20of%20speech.pdf)
    by Werner Verhelst and Marc Roelands. As a reference implementation the WSOLA implementation by *Olli Parviainen* in the LGPL
    [SoundTouch - an open-source audio processing
    library](http://www.surina.net/soundtouch/) was used.
  - The FFT implementation used within TarsosDSP is by *Piotr Wendykier* and is included in his GPL’d [JTransforms library](https://sites.google.com/site/piotrwendykier/software/jtransforms). 
    JTransforms is the first, open source, multithreaded FFT library written in pure Java. 
  - The sample rate conversion feature is implemented by *Laszlo systems* in the GPL’d [libresample4j](https://github.com/dnault-laszlo/libresample4j)
    library. libresample4j is a Java port of Dominic Mazzoni’s libresample 0.1.3, which is in turn based on [Julius Smith’s Resample 1.7 library](http://www-ccrma.stanford.edu/~jos/resample/)
  - Various FFT window functions are done by Damien Di Fede and Corban Brook for the GPL’d [Minim](http://code.compartmental.net/tools/minim/) project.
  - Beat induction based on onsets and saliences is done using code from [Simon Dixon’s BeatRoot system](http://www.eecs.qmul.ac.uk/~simond/beatroot/.The) software
    is licensed under the GPL. The algorithm is documented in the 2001 JNMR paper [*Automatic Extraction of Tempo and Beat From Expressive
        Performances*](http://www.tandfonline.com/doi/abs/10.1076/jnmr.30.1.39.7119#.UbcsROcW0r0) and in the 2007 JNMR article [*Evaluation of the Audio Beat Tracking System BeatRoot*](http://www.tandfonline.com/doi/full/10.1080/09298210701653310#.UbcsgecW0r0)
  - A complex domain onset detection function is implemented using Aubio as an inspiration. Aubio, by *Paul Brossiers* contains very clean
    object oriented c-code, the cleanest c-code I have ever seen. The algorithm is described in [*Complex Domain Onset Detection for
    Musical Signals*](http://www-student.elec.qmul.ac.uk/people/juan/Documents/Duxbury-DAFx-2003.pdf)
    by Christopher Duxbury, Mike E. Davies, and Mark B. Sandler, in Proceedings of the Digital Audio Effects Conference, DAFx-03, pages
    90-93, London, UK, 2003
  - An implementation of the Constant-Q transform by Karl Helgason for the GPL’d [RasmusDSP](http://rasmusdsp.sourceforge.net)
    project has been adapted for use in TarsosDSP. More information about the Constant-Q transform can be found in the following papers
    [*Calculation of a Constant Q Spectral Transform*](http://www.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf)
    by Judith C. Brown, [*An Efficient Algorithm for the Calculation of a Constant Q Transform*](http://www.wellesley.edu/Physics/brown/pubs/effalgV92P2698-P2701.pdf),
    by Judith C. Brown and Miller S. Puckette, and [*The Constant Q Transform*](http://wwwmath1.uni-muenster.de/logik/org/staff/blankertz/constQ/constQ.pdf)
    by Benjamin Blankertz
  - The Haar Wavelet Transform implemented in TarsosDSP is based on the pseudocode found in “*Wavelets Made Easy*” by Yves Nievergelt.
  - The lifting scheme wavelet package is developed by [Ian Kaplan](http://www.bearcave.com/software/java/wavelets/basiclift.html)
    and is based on the concepts explained in *Ripples in Mathematics: The Discrete Wavelet Transform* by A. Jensen and A. la Cour-Harbo, Springer. It is only slightly modified for easy use in TarsosDSP
  - The frequency domain pitch shifter is developed by [Stephan M. Bernsee](http://www.dspdimension.com/admin/pitch-shifting-using-the-ft/Transformbasiclift.html)
    and is based on the concepts explained in *Pitch Shifting Using The Fourier Transform*. For the moment it is a rather direct translation
    of the c implementation. It was released under a ’ Wide Open License’. As the name of the license suggests, it is a license without much restrictions. 
  - The granulator is developed for [the beads project](http://www.beadsproject.net/). It was released under a GPL-license. 

## Changelog

<dt>Version 1.0</dt><dd>2012-04-24</dd> First release which includes several pitch trackers and a time stretching algorithm, amongst other things. Downloads and javadoc API can be found at the "TarsosDSP release directory":http://0110.be/releases/TarsosDSP/

<dt>Version 1.1</dt><dd>2012-06-4</dd> 
Changed how the audio dispatcher stops. Added StopAudioProcessor.
Added FastYin implementation by Matthias Mauch
Added AMDF pitch estimator by Eder Souza

<dt>Version 1.2</dt><dd>2012-08-21</dd> 
Modified the interface of PitchDetector to return a more elaborate result structure with pitch, probability and a boolean "is pitched".
Added an implementation of an envelope follower or envelope detector.

<dt>Version 1.3</dt><dd>2012-09-19</dd> 
TarsosDSP can do audio synthesis now. The first simple unit generators are included in the library.
It has a new audio feature extraction feature, implemented in the FeatureExtractor example. 
Added ASCII-art to the source code (this is the main TarsosDSP 1.3 feature). 

<dt>Version 1.4</dt><dd>2012-10-31</dd>
Included a resample feature, implemented by libresample4j. Together with the WSOLA implementation, it can be used for pitch shifting (similar to Phase Vocoding). A pitch shifting example (both with a CLI and a UI) is added in the 1.4 version of the TarsosDSP library as well. 


<dt>Version 1.5</dt><dd>2013-04-30</dd>
Converted TarsosDSP to maven. This is known as the Malaryta-release. The "Malaryta" release is provided to you by "RikkiMongoose":http://github.com/rikkimongoose (idea, documents, git things) and "Ultar":http://github.com/ultar (converting to maven, refactoring). Malaryta is the capital of Malaryta Raion, Brest Region in the Republic of Belarus. Both of developers spent their childhood in Brest, and think that title Malaryta is as strange as Ubuntu or Whistler. The 1.5 release also includes various FFT window functions from the cool "Minim project":http://code.compartmental.net/tools/minim/ by Damien Di Fede. 

<dt>Version 1.6</dt><dd>2013-06-12</dd>
This release features practical onset and beat detection algorithms. A complex domain onset detection and a spectral flux onset detection algorithm are added. This release also includes a way to guess a beat from onsets. Parts of the "BeatRoot system":http://www.eecs.qmul.ac.uk/~simond/beatroot/, by Simon Dixon, are included to this end. Also included in this release is an implementation of the Constant-Q transform.

<dt>Version 1.7</dt><dd>2013-10-08</dd>
This release adds the ability to extract the MFCC from an audio signal. Also an example of the Constant-Q transform is added, together with a reusable visualization class library. The build system is reverted back to pure ANT

<dt>Version 1.8</dt><dd>2014-04-10</dd>
With this release it is possible to extract spectral peaks from an FFT and get precise frequency estimates using phase info. An example application called SpectralPeaks is added as well.

<dt>Version 1.9</dt><dd>2014-08-10</dd>
This release includes a Haar Wavelet Transform and an example of an audio compression algorithm based on Haar Wavelets. It also includes a significant change in package naming.

<dt>Version 2.0</dt><dd>2014-08-13</dd>
The 2.0 version is worth the major version update since it offers out-of-the-box support for Android. The release has no more dependencies on the parts of the java runtime that are not included in Android. To offer this support some packages have been shifted around. The code that does I/O is dependent on the runtime (JVM or Dalvik) and is abstracted using the @be.tarsos.dsp.io@ package.

<dt>Version 2.1</dt><dd>2015-03-03</dd>
The 2.1 version restructures some of the source files. All source can now be found in src. The ant build file is adapted to reflect this change. This version also includes an STFT pitch shifter. There was already a time domain pitch shifter included and now a frequency domain implementation is present as well. 

<dt>Version 2.2</dt><dd>2015-03-03</dd>
The 2.2 version includes a new @AudioDispatcher@. It has been reviewed toroughly and now behaves predictably for the first and last buffers as well. To prevent compatibility issues the version has been changed.  

<dt>Version 2.3</dt><dd>2015-09-01</dd>
The 2.3 version includes improved Android support: "audio decoding on Android":http://0110.be/posts/Decode_MP3s_and_other_Audio_formats_the_easy_way_on_Android can be done using a provided, statically compiled ffmpeg binary. The ffmpeg decoding functionality for JVM has been improved as well. If no ffmpeg executable is found it is downloaded automatically from "here":http://0110.be/releases/TarsosDSP/TarsosDSP-static-ffmpeg. 

<dt>Version 2.4</dt><dd>2016-12-01</dd>
Some small changes to the pipe decoder. Now it is possible to set a start and duration for the incoming decoded audio from ffmpeg. 
