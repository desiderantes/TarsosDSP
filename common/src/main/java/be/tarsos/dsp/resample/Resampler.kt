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
/******************************************************************************
 *
 * libresample4j
 * Copyright (c) 2009 Laszlo Systems, Inc. All Rights Reserved.
 *
 * libresample4j is a Java port of Dominic Mazzoni's libresample 0.1.3,
 * which is in turn based on Julius Smith's Resample 1.7 library.
 * http://www-ccrma.stanford.edu/~jos/resample/
 *
 * License: LGPL -- see the file LICENSE.txt for more information
 *
 */
package be.tarsos.dsp.resample

import be.tarsos.dsp.resample.FilterKit.lrsFilterUD
import be.tarsos.dsp.resample.FilterKit.lrsFilterUp
import be.tarsos.dsp.resample.FilterKit.lrsLpFilter
import java.nio.FloatBuffer

class Resampler {
    private val Imp: FloatArray
    private val ImpD: FloatArray
    private val LpScl: Float
    private val Nmult: Int
    private val Nwing: Int
    private val minFactor: Double
    private val maxFactor: Double
    private val XSize: Int
    private val X: FloatArray
    val filterWidth: Int
    private val Y: FloatArray
    private var Xp // Current "now"-sample pointer for input
            : Int
    private var Xread // Position to put new samples
            : Int
    private var Yp: Int
    private var Time: Double

    /**
     * Clone an existing resampling session. Faster than creating one from scratch.
     *
     * @param other
     */
    constructor(other: Resampler) {
        Imp = other.Imp.clone()
        ImpD = other.ImpD.clone()
        LpScl = other.LpScl
        Nmult = other.Nmult
        Nwing = other.Nwing
        minFactor = other.minFactor
        maxFactor = other.maxFactor
        XSize = other.XSize
        X = other.X.clone()
        Xp = other.Xp
        Xread = other.Xread
        filterWidth = other.filterWidth
        Y = other.Y.clone()
        Yp = other.Yp
        Time = other.Time
    }

    /**
     * Create a new resampling session.
     *
     * @param highQuality true for better quality, slower processing time
     * @param minFactor   lower bound on resampling factor for this session
     * @param maxFactor   upper bound on resampling factor for this session
     * @throws IllegalArgumentException if minFactor or maxFactor is not
     * positive, or if maxFactor is less than minFactor
     */
    constructor(highQuality: Boolean, minFactor: Double, maxFactor: Double) {
        require(!(minFactor <= 0.0 || maxFactor <= 0.0)) { "minFactor and maxFactor must be positive" }
        require(maxFactor >= minFactor) { "minFactor must be <= maxFactor" }
        this.minFactor = minFactor
        this.maxFactor = maxFactor
        Nmult = if (highQuality) 35 else 11
        LpScl = 1.0f
        Nwing = Npc * (Nmult - 1) / 2 // # of filter coeffs in right wing
        val Rolloff = 0.90
        val Beta = 6.0
        val Imp64 = DoubleArray(Nwing)
        lrsLpFilter(Imp64, Nwing, 0.5 * Rolloff, Beta, Npc)
        Imp = FloatArray(Nwing)
        ImpD = FloatArray(Nwing)
        for (i in 0 until Nwing) {
            Imp[i] = Imp64[i].toFloat()
        }

        // Storing deltas in ImpD makes linear interpolation
        // of the filter coefficients faster
        for (i in 0 until Nwing - 1) {
            ImpD[i] = Imp[i + 1] - Imp[i]
        }

        // Last coeff. not interpolated
        ImpD[Nwing - 1] = -Imp[Nwing - 1]

        // Calc reach of LP filter wing (plus some creeping room)
        val Xoff_min = ((Nmult + 1) / 2.0 * Math.max(1.0, 1.0 / minFactor) + 10).toInt()
        val Xoff_max = ((Nmult + 1) / 2.0 * Math.max(1.0, 1.0 / maxFactor) + 10).toInt()
        filterWidth = Math.max(Xoff_min, Xoff_max)

        // Make the inBuffer size at least 4096, but larger if necessary
        // in order to store the minimum reach of the LP filter and then some.
        // Then allocate the buffer an extra Xoff larger so that
        // we can zero-pad up to Xoff zeros at the end when we reach the
        // end of the input samples.
        XSize = Math.max(2 * filterWidth + 10, 4096)
        X = FloatArray(XSize + filterWidth)
        Xp = filterWidth
        Xread = filterWidth

        // Make the outBuffer long enough to hold the entire processed
        // output of one inBuffer
        val YSize = (XSize.toDouble() * maxFactor + 2.0).toInt()
        Y = FloatArray(YSize)
        Yp = 0
        Time = filterWidth.toDouble() // Current-time pointer for converter
    }

    /**
     * Process a batch of samples. There is no guarantee that the input buffer will be drained.
     *
     * @param factor    factor at which to resample this batch
     * @param buffers   sample buffer for producing input and consuming output
     * @param lastBatch true if this is known to be the last batch of samples
     * @return true iff resampling is complete (ie. no input samples consumed and no output samples produced)
     */
    fun process(factor: Double, buffers: SampleBuffers, lastBatch: Boolean): Boolean {
        require(!(factor < minFactor || factor > maxFactor)) {
            ("factor " + factor + " is not between minFactor=" + minFactor
                    + " and maxFactor=" + maxFactor)
        }
        val outBufferLen = buffers.outputBufferLength
        val inBufferLen = buffers.inputBufferLength
        val Imp = Imp
        val ImpD = ImpD
        var LpScl = LpScl
        val Nwing = Nwing
        val interpFilt = false // TRUE means interpolate filter coeffs
        var inBufferUsed = 0
        var outSampleCount = 0

        // Start by copying any samples still in the Y buffer to the output
        // buffer
        if (Yp != 0 && outBufferLen - outSampleCount > 0) {
            val len = Math.min(outBufferLen - outSampleCount, Yp)
            buffers.consumeOutput(Y, 0, len)
            //for (int i = 0; i < len; i++) {
            //    outBuffer[outBufferOffset + outSampleCount + i] = this.Y[i];
            //}
            outSampleCount += len
            for (i in 0 until Yp - len) {
                Y[i] = Y[i + len]
            }
            Yp -= len
        }

        // If there are still output samples left, return now - we need
        // the full output buffer available to us...
        if (Yp != 0) {
            return inBufferUsed == 0 && outSampleCount == 0
        }

        // Account for increased filter gain when using factors less than 1
        if (factor < 1) {
            LpScl = (LpScl * factor).toFloat()
        }
        while (true) {

            // This is the maximum number of samples we can process
            // per loop iteration

            /*
             * #ifdef DEBUG
             * printf("XSize: %d Xoff: %d Xread: %d Xp: %d lastFlag: %d\n",
             * this.XSize, this.Xoff, this.Xread, this.Xp, lastFlag); #endif
             */

            // Copy as many samples as we can from the input buffer into X
            var len = XSize - Xread
            if (len >= inBufferLen - inBufferUsed) {
                len = inBufferLen - inBufferUsed
            }
            buffers.produceInput(X, Xread, len)
            //for (int i = 0; i < len; i++) {
            //    this.X[this.Xread + i] = inBuffer[inBufferOffset + inBufferUsed + i];
            //}
            inBufferUsed += len
            Xread += len
            var Nx: Int
            if (lastBatch && inBufferUsed == inBufferLen) {
                // If these are the last samples, zero-pad the
                // end of the input buffer and make sure we process
                // all the way to the end
                Nx = Xread - filterWidth
                for (i in 0 until filterWidth) {
                    X[Xread + i] = 0F
                }
            } else {
                Nx = Xread - 2 * filterWidth
            }

            /*
             * #ifdef DEBUG fprintf(stderr, "new len=%d Nx=%d\n", len, Nx);
             * #endif
             */if (Nx <= 0) {
                break
            }

            // Resample stuff in input buffer
            var Nout: Int
            Nout = if (factor >= 1) { // SrcUp() is faster if we can use it */
                lrsSrcUp(X, Y, factor,  /* &this.Time, */Nx, Nwing, LpScl, Imp, ImpD, interpFilt)
            } else {
                lrsSrcUD(X, Y, factor,  /* &this.Time, */Nx, Nwing, LpScl, Imp, ImpD, interpFilt)
            }

            /*
             * #ifdef DEBUG
             * printf("Nout: %d\n", Nout);
             * #endif
             */Time -= Nx.toDouble() // Move converter Nx samples back in time
            Xp += Nx // Advance by number of samples processed

            // Calc time accumulation in Time
            val Ncreep = Time.toInt() - filterWidth
            if (Ncreep != 0) {
                Time -= Ncreep.toDouble() // Remove time accumulation
                Xp += Ncreep // and add it to read pointer
            }

            // Copy part of input signal that must be re-used
            val Nreuse = Xread - (Xp - filterWidth)
            for (i in 0 until Nreuse) {
                X[i] = X[i + (Xp - filterWidth)]
            }

            /*
            #ifdef DEBUG
            printf("New Xread=%d\n", Nreuse);
            #endif */Xread = Nreuse // Pos in input buff to read new data into
            Xp = filterWidth
            Yp = Nout

            // Copy as many samples as possible to the output buffer
            if (Yp != 0 && outBufferLen - outSampleCount > 0) {
                len = Math.min(outBufferLen - outSampleCount, Yp)
                buffers.consumeOutput(Y, 0, len)
                //for (int i = 0; i < len; i++) {
                //    outBuffer[outBufferOffset + outSampleCount + i] = this.Y[i];
                //}
                outSampleCount += len
                for (i in 0 until Yp - len) {
                    Y[i] = Y[i + len]
                }
                Yp -= len
            }

            // If there are still output samples left, return now,
            //   since we need the full output buffer available
            if (Yp != 0) {
                break
            }
        }
        return inBufferUsed == 0 && outSampleCount == 0
    }

    /**
     * Process a batch of samples. Convenience method for when the input and output are both floats.
     *
     * @param factor       factor at which to resample this batch
     * @param inputBuffer  contains input samples in the range -1.0 to 1.0
     * @param outputBuffer output samples will be deposited here
     * @param lastBatch    true if this is known to be the last batch of samples
     * @return true iff resampling is complete (ie. no input samples consumed and no output samples produced)
     */
    fun process(
        factor: Double,
        inputBuffer: FloatBuffer,
        lastBatch: Boolean,
        outputBuffer: FloatBuffer
    ): Boolean {
        val sampleBuffers: SampleBuffers = object : SampleBuffers {
            override val inputBufferLength: Int
                get() = inputBuffer.remaining()

            override val outputBufferLength: Int
                get() = outputBuffer.remaining()

            override fun produceInput(array: FloatArray, offset: Int, length: Int) {
                inputBuffer[array, offset, length]
            }

            override fun consumeOutput(array: FloatArray, offset: Int, length: Int) {
                outputBuffer.put(array, offset, length)
            }
        }
        return process(factor, sampleBuffers, lastBatch)
    }

    /**
     * Process a batch of samples. Alternative interface if you prefer to work with arrays.
     *
     * @param factor          resampling rate for this batch
     * @param inBuffer        array containing input samples in the range -1.0 to 1.0
     * @param inBufferOffset  offset into inBuffer at which to start processing
     * @param inBufferLen     number of valid elements in the inputBuffer
     * @param lastBatch       pass true if this is the last batch of samples
     * @param outBuffer       array to hold the resampled data
     * @param outBufferOffset Offset in the output buffer.
     * @param outBufferLen    Output buffer length.
     * @return the number of samples consumed and generated
     */
    fun process(
        factor: Double,
        inBuffer: FloatArray?,
        inBufferOffset: Int,
        inBufferLen: Int,
        lastBatch: Boolean,
        outBuffer: FloatArray?,
        outBufferOffset: Int,
        outBufferLen: Int
    ): Result {
        val inputBuffer = FloatBuffer.wrap(inBuffer, inBufferOffset, inBufferLen)
        val outputBuffer = FloatBuffer.wrap(outBuffer, outBufferOffset, outBufferLen)
        process(factor, inputBuffer, lastBatch, outputBuffer)
        return Result(
            inputBuffer.position() - inBufferOffset,
            outputBuffer.position() - outBufferOffset
        )
    }

    /*
     * Sampling rate up-conversion only subroutine; Slightly faster than
     * down-conversion;
     */
    private fun lrsSrcUp(
        X: FloatArray,
        Y: FloatArray,
        factor: Double,
        Nx: Int,
        Nwing: Int,
        LpScl: Float,
        Imp: FloatArray,
        ImpD: FloatArray,
        Interp: Boolean
    ): Int {
        val Xp_array = X
        var Xp_index: Int
        val Yp_array = Y
        var Yp_index = 0
        var v: Float
        var CurrentTime = Time
        val endTime: Double // When Time reaches EndTime, return to user
        val dt: Double = 1.0 / factor // Step through input signal // Output sampling period
        endTime = CurrentTime + Nx
        while (CurrentTime < endTime) {
            val LeftPhase = CurrentTime - Math.floor(CurrentTime)
            val RightPhase = 1.0 - LeftPhase
            Xp_index = CurrentTime.toInt() // Ptr to current input sample
            // Perform left-wing inner product
            v = lrsFilterUp(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index++, LeftPhase, -1)
            // Perform right-wing inner product
            v += lrsFilterUp(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index, RightPhase, 1)
            v *= LpScl // Normalize for unity filter gain
            Yp_array[Yp_index++] = v // Deposit output
            CurrentTime += dt // Move to next sample by time increment
        }
        Time = CurrentTime
        return Yp_index // Return the number of output samples
    }

    private fun lrsSrcUD(
        X: FloatArray,
        Y: FloatArray,
        factor: Double,
        Nx: Int,
        Nwing: Int,
        LpScl: Float,
        Imp: FloatArray,
        ImpD: FloatArray,
        Interp: Boolean
    ): Int {
        val Xp_array = X
        var Xp_index: Int
        val Yp_array = Y
        var Yp_index = 0
        var v: Float
        var CurrentTime = Time
        val dh: Double // Step through filter impulse response
        val dt: Double // Step through input signal
        val endTime: Double // When Time reaches EndTime, return to user
        dt = 1.0 / factor // Output sampling period
        dh = Math.min(
            Npc.toDouble(),
            factor * Npc
        ) // Filter sampling period
        endTime = CurrentTime + Nx
        while (CurrentTime < endTime) {
            val LeftPhase = CurrentTime - Math.floor(CurrentTime)
            val RightPhase = 1.0 - LeftPhase
            Xp_index = CurrentTime.toInt() // Ptr to current input sample
            // Perform left-wing inner product
            v = lrsFilterUD(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index++, LeftPhase, -1, dh)
            // Perform right-wing inner product
            v += lrsFilterUD(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index, RightPhase, 1, dh)
            v *= LpScl // Normalize for unity filter gain
            Yp_array[Yp_index++] = v // Deposit output
            CurrentTime += dt // Move to next sample by time increment
        }
        Time = CurrentTime
        return Yp_index // Return the number of output samples
    }

    class Result(val inputSamplesConsumed: Int, val outputSamplesGenerated: Int)

    companion object {
        // number of values per 1/delta in impulse response
        protected const val Npc = 4096
    }
}