# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.4] - 2016-12-01
### Changed
 - Some small changes to the pipe decoder. Now it is possible to set a start and duration for the incoming decoded audio from ffmpeg. 

## [2.3] - 2015-09-01
### Changed
 - Improved Android support: [Audio decoding on Android](http://0110.be/posts/Decode_MP3s_and_other_Audio_formats_the_easy_way_on_Android) can be done using a provided, statically compiled ffmpeg binary. 
 - The ffmpeg decoding functionality for JVM has been improved as well. If no ffmpeg executable is found it is downloaded automatically from [here](http://0110.be/releases/TarsosDSP/TarsosDSP-static-ffmpeg) 

## [2.2] - 2015-03-03
### Added
 - A new `AudioDispatcher`. It has been reviewed thoroughly and now behaves predictably for the first and last buffers as well.
### Changed
 - To prevent compatibility issues the version has been changed.  

## [2.1] - 2015-03-03
### Added
 - STFT pitch shifter. 
 - Besides the time domain pitch shifter included, now a frequency domain implementation is present as well.
### Changed
 - Restructured some of the source files. All source can now be found in src. The ant build file is adapted to reflect this change. 

## [2.0] - 2014-08-13
### Added
 - Out-of-the-box support for Android. 
### Changed
 - Removed dependencies on the parts of the java runtime that are not included in Android.
 - Moved code that does I/O to depend on the runtime (JVM or Dalvik) and is abstracted using the `be.tarsos.dsp.io` package.
 
## [1.9] - 2014-08-10
### Added
 - A Haar Wavelet Transform and an example of an audio compression algorithm based on Haar Wavelets.
### Changed
 - Package naming.
  
## [1.8] - 2014-04-10
### Added
 - Extract spectral peaks from an FFT and get precise frequency estimates using phase info.
 - Example application called SpectralPeaks.

## [1.7] - 2013-10-08
### Added
 - The ability to extract the MFCC from an audio signal.
 - An example of the Constant-Q transform, together with a reusable visualization class library.
### Changed
 - Build system is reverted back to pure ANT

## [1.6] - 2013-06-12
### Added
 - Practical onset and beat detection algorithms.
 - A complex domain onset detection and a spectral flux onset detection algorithm. 
 - A mechanism to guess a beat from onsets. Parts of the [BeatRoot system](http://www.eecs.qmul.ac.uk/~simond/beatroot/), by Simon Dixon, are included to this end.
 - An implementation of the Constant-Q transform.

## [1.5] - 2013-04-30
### Added
 - Various FFT window functions from the cool [Minim project](http://code.compartmental.net/tools/minim/) by Damien Di Fede.
### Changed
 - Converted TarsosDSP to maven. This is known as the Malaryta-release. The "Malaryta" release is provided to you by [RikkiMongoose](http://github.com/rikkimongoose) (idea, documents, git things) and [Ultar](http://github.com/ultar) (converting to maven, refactoring). Malaryta is the capital of Malaryta Raion, Brest Region in the Republic of Belarus. Both of developers spent their childhood in Brest, and think that title Malaryta is as strange as Ubuntu or Whistler. 

## [1.4] - 2012-10-31
### Added
 - Included a resample feature, implemented by libresample4j. Together with the WSOLA implementation, it can be used for pitch shifting (similar to Phase Vocoding).
 - A pitch shifting example (both with a CLI and a UI) is added in the 1.4 version of the TarsosDSP library as well. 

## [1.3]  - 2012-09-19
### Added
 - TarsosDSP can do audio synthesis now. The first simple unit generators are included in the library.
It has a new audio feature extraction feature, implemented in the FeatureExtractor example. 
 - ASCII-art to the source code (this is the main TarsosDSP 1.3 feature).

## [1.2] - 2012-08-21 
### Added
 - An implementation of an envelope follower or envelope detector.
### Changed
 - Modified the interface of PitchDetector to return a more elaborate result structure with pitch, probability and a boolean "is pitched".

## [1.1] - 2012-06-4 
### Added
 - StopAudioProcessor.
 - FastYin implementation by Matthias Mauch
 - AMDF pitch estimator by Eder Souza
### Changed
 - Stop logic for the audio dispatcher.

## [1.0] - 2012-04-24
### Added
 - First release which includes several pitch trackers and a time stretching algorithm, amongst other things. Downloads and javadoc API can be found at the [TarsosDSP release directory](http://0110.be/releases/TarsosDSP/)