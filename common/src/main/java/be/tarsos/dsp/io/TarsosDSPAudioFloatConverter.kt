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
/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package be.tarsos.dsp.io

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.util.shl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * This class is used to convert between 8,16,24,32,32+ bit signed/unsigned
 * big/litle endian fixed/floating point byte buffers and float buffers.
 *
 * @author Karl Helgason
 */
sealed class TarsosDSPAudioFloatConverter {
    var format: TarsosDSPAudioFormat? = null
        private set

    abstract fun toFloatArray(
        in_buff: ByteArray, in_offset: Int,
        out_buff: FloatArray, out_offset: Int, out_len: Int
    ): FloatArray

    fun toFloatArray(
        in_buff: ByteArray, out_buff: FloatArray,
        out_offset: Int, out_len: Int
    ): FloatArray {
        return toFloatArray(in_buff, 0, out_buff, out_offset, out_len)
    }

    fun toFloatArray(
        in_buff: ByteArray, in_offset: Int,
        out_buff: FloatArray, out_len: Int
    ): FloatArray {
        return toFloatArray(in_buff, in_offset, out_buff, 0, out_len)
    }

    fun toFloatArray(in_buff: ByteArray, out_buff: FloatArray, out_len: Int): FloatArray {
        return toFloatArray(in_buff, 0, out_buff, 0, out_len)
    }

    fun toFloatArray(in_buff: ByteArray, out_buff: FloatArray): FloatArray {
        return toFloatArray(in_buff, 0, out_buff, 0, out_buff.size)
    }

    abstract fun toByteArray(
        in_buff: FloatArray, in_offset: Int,
        in_len: Int, out_buff: ByteArray, out_offset: Int
    ): ByteArray

    fun toByteArray(
        in_buff: FloatArray, in_len: Int, out_buff: ByteArray,
        out_offset: Int
    ): ByteArray {
        return toByteArray(in_buff, 0, in_len, out_buff, out_offset)
    }

    fun toByteArray(
        in_buff: FloatArray, in_offset: Int, in_len: Int,
        out_buff: ByteArray
    ): ByteArray {
        return toByteArray(in_buff, in_offset, in_len, out_buff, 0)
    }

    fun toByteArray(in_buff: FloatArray, in_len: Int, out_buff: ByteArray): ByteArray {
        return toByteArray(in_buff, 0, in_len, out_buff, 0)
    }

    fun toByteArray(in_buff: FloatArray, out_buff: ByteArray): ByteArray {
        return toByteArray(in_buff, 0, in_buff.size, out_buff, 0)
    }

    /***************************************************************************
     *
     * LSB Filter, used filter least significant byte in samples arrays.
     *
     * Is used filter out data in lsb byte when SampleSizeInBits is not
     * dividable by 8.
     *
     */
    private class AudioFloatLSBFilter(
        converter: TarsosDSPAudioFloatConverter?,
        format: TarsosDSPAudioFormat
    ) : TarsosDSPAudioFloatConverter() {
        private val offset: Int
        private val stepsize: Int
        private var mask: Byte = 0
        private val converter: TarsosDSPAudioFloatConverter?
        private var mask_buffer: ByteArray? = null
        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            val ret = converter!!.toByteArray(
                in_buff, in_offset, in_len,
                out_buff, out_offset
            )
            val out_offset_end = in_len * stepsize
            var i = out_offset + offset
            while (i < out_offset_end) {
                out_buff[i] = (out_buff[i] and mask)
                i += stepsize
            }
            return ret
        }

        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            if (mask_buffer == null || mask_buffer!!.size < in_buff.size) mask_buffer = ByteArray(in_buff.size)
            System.arraycopy(in_buff, 0, mask_buffer, 0, in_buff.size)
            val in_offset_end = out_len * stepsize
            var i = in_offset + offset
            while (i < in_offset_end) {
                mask_buffer!![i] = (mask_buffer!![i] and mask)
                i += stepsize
            }
            return converter!!.toFloatArray(
                mask_buffer!!, in_offset,
                out_buff, out_offset, out_len
            )
        }

        init {
            val bits = format.sampleSizeInBits
            val bigEndian = format.isBigEndian
            this.converter = converter
            stepsize = (bits + 7) / 8
            offset = if (bigEndian) stepsize - 1 else 0
            val lsb_bits = bits % 8
            mask =
                if (lsb_bits == 0) 0x00.toByte() else if (lsb_bits == 1) 0x80.toByte() else if (lsb_bits == 2) 0xC0.toByte() else if (lsb_bits == 3) 0xE0.toByte() else if (lsb_bits == 4) 0xF0.toByte() else if (lsb_bits == 5) 0xF8.toByte() else if (lsb_bits == 6) 0xFC.toByte() else if (lsb_bits == 7) 0xFE.toByte() else 0xFF.toByte()
        }
    }

    /***************************************************************************
     *
     * 64 bit float, little/big-endian
     *
     */
    // PCM 64 bit float, little-endian
    private class AudioFloatConversion64L : TarsosDSPAudioFloatConverter() {
        var bytebuffer: ByteBuffer? = null
        var floatbuffer: DoubleBuffer? = null
        var double_buff: DoubleArray? = null
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            val in_len = out_len * 8
            if (bytebuffer == null || bytebuffer!!.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                    ByteOrder.LITTLE_ENDIAN
                ).also {
                    floatbuffer = it.asDoubleBuffer()
                }
            }
            bytebuffer!!.position(0)
            floatbuffer!!.position(0)
            bytebuffer!!.put(in_buff, in_offset, in_len)
            if (double_buff == null
                || double_buff!!.size < out_len + out_offset
            ) double_buff = DoubleArray(out_len + out_offset)
            floatbuffer!![double_buff, out_offset, out_len]
            val out_offset_end = out_offset + out_len
            for (i in out_offset until out_offset_end) {
                out_buff[i] = double_buff!![i].toFloat()
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            val out_len = in_len * 8
            if (bytebuffer == null || bytebuffer!!.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                    ByteOrder.LITTLE_ENDIAN
                ).also {
                    floatbuffer = it.asDoubleBuffer()
                }
            }
            floatbuffer?.position(0)
            bytebuffer?.position(0)
            if (double_buff == null || double_buff!!.size < in_offset + in_len) double_buff =
                DoubleArray(in_offset + in_len)
            val in_offset_end = in_offset + in_len
            for (i in in_offset until in_offset_end) {
                double_buff!![i] = in_buff[i].toDouble()
            }
            floatbuffer!!.put(double_buff, in_offset, in_len)
            bytebuffer!![out_buff, out_offset, out_len]
            return out_buff
        }
    }

    // PCM 64 bit float, big-endian
    private class AudioFloatConversion64B : TarsosDSPAudioFloatConverter() {
        var bytebuffer: ByteBuffer? = null
        var floatbuffer: DoubleBuffer? = null
        var double_buff: DoubleArray? = null
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            val in_len = out_len * 8
            if (bytebuffer == null || bytebuffer!!.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                    ByteOrder.BIG_ENDIAN
                ).also {
                    floatbuffer = it.asDoubleBuffer()
                }
            }
            bytebuffer?.position(0)
            floatbuffer?.position(0)
            bytebuffer!!.put(in_buff, in_offset, in_len)
            if (double_buff == null
                || double_buff!!.size < out_len + out_offset
            ) double_buff = DoubleArray(out_len + out_offset)
            floatbuffer!![double_buff, out_offset, out_len]
            val out_offset_end = out_offset + out_len
            for (i in out_offset until out_offset_end) {
                out_buff[i] = double_buff!![i].toFloat()
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            val out_len = in_len * 8
            if (bytebuffer == null || bytebuffer!!.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                    ByteOrder.BIG_ENDIAN
                ).also {
                    floatbuffer = it.asDoubleBuffer()
                }

            }
            floatbuffer!!.position(0)
            bytebuffer!!.position(0)
            if (double_buff == null || double_buff!!.size < in_offset + in_len) double_buff =
                DoubleArray(in_offset + in_len)
            val in_offset_end = in_offset + in_len
            for (i in in_offset until in_offset_end) {
                double_buff!![i] = in_buff[i].toDouble()
            }
            floatbuffer!!.put(double_buff, in_offset, in_len)
            bytebuffer!![out_buff, out_offset, out_len]
            return out_buff
        }
    }

    /***************************************************************************
     *
     * 32 bit float, little/big-endian
     *
     */
    // PCM 32 bit float, little-endian
    private class AudioFloatConversion32L : TarsosDSPAudioFloatConverter() {
        var bytebuffer: ByteBuffer? = null
        var floatbuffer: FloatBuffer? = null
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            val in_len = out_len * 4
            if (bytebuffer == null || bytebuffer!!.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                    ByteOrder.LITTLE_ENDIAN
                ).also {
                    floatbuffer = it.asFloatBuffer()
                }
            }
            bytebuffer!!.position(0)
            floatbuffer!!.position(0)
            bytebuffer!!.put(in_buff, in_offset, in_len)
            floatbuffer!![out_buff, out_offset, out_len]
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            val out_len = in_len * 4
            if (bytebuffer == null || bytebuffer!!.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                    ByteOrder.LITTLE_ENDIAN
                ).also {
                    floatbuffer = it.asFloatBuffer()
                }
            }
            floatbuffer!!.position(0)
            bytebuffer!!.position(0)
            floatbuffer!!.put(in_buff, in_offset, in_len)
            bytebuffer!![out_buff, out_offset, out_len]
            return out_buff
        }
    }

    // PCM 32 bit float, big-endian
    private class AudioFloatConversion32B : TarsosDSPAudioFloatConverter() {
        var bytebuffer: ByteBuffer? = null
        var floatbuffer: FloatBuffer? = null
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            val in_len = out_len * 4
            if (bytebuffer == null || bytebuffer!!.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                    ByteOrder.BIG_ENDIAN
                ).also {
                    floatbuffer = it.asFloatBuffer()
                }
            }
            bytebuffer!!.position(0)
            floatbuffer!!.position(0)
            bytebuffer!!.put(in_buff, in_offset, in_len)
            floatbuffer!![out_buff, out_offset, out_len]
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            val out_len = in_len * 4
            if (bytebuffer == null || bytebuffer!!.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                    ByteOrder.BIG_ENDIAN
                ).also {
                    floatbuffer = it.asFloatBuffer()
                }
            }
            floatbuffer!!.position(0)
            bytebuffer!!.position(0)
            floatbuffer!!.put(in_buff, in_offset, in_len)
            bytebuffer!![out_buff, out_offset, out_len]
            return out_buff
        }
    }

    /***************************************************************************
     *
     * 8 bit signed/unsigned
     *
     */
    // PCM 8 bit, signed
    private class AudioFloatConversion8S : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) out_buff[ox++] = in_buff[ix++] * (1.0f / 127.0f)
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) out_buff[ox++] = (in_buff[ix++] * 127.0f).toByte()
            return out_buff
        }
    }

    // PCM 8 bit, unsigned
    private class AudioFloatConversion8U : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) out_buff[ox++] = (((in_buff[ix++] and 0xFF.toByte()) - 127)
                    * (1.0f / 127.0f))
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) out_buff[ox++] = (127 + in_buff[ix++] * 127.0f).toByte()
            return out_buff
        }
    }

    /***************************************************************************
     *
     * 16 bit signed/unsigned, little/big-endian
     *
     */
    // PCM 16 bit, signed, little-endian
    private class AudioFloatConversion16SL : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            val len = out_offset + out_len
            for (ox in out_offset until len) {
                out_buff[ox] = (in_buff[ix++] and 0xFF.toByte() or
                        (in_buff[ix++] shl 8.toByte())).toShort() * (1.0f / 32767.0f)
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ox = out_offset
            val len = in_offset + in_len
            for (ix in in_offset until len) {
                val x = (in_buff[ix] * 32767.0).toInt()
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
            }
            return out_buff
        }
    }

    // PCM 16 bit, signed, big-endian
    private class AudioFloatConversion16SB : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                out_buff[ox++] = (in_buff[ix++] shl 8 or
                        (in_buff[ix++] and 0xFF.toByte())).toShort() * (1.0f / 32767.0f)
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = (in_buff[ix++] * 32767.0).toInt()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
            }
            return out_buff
        }
    }

    // PCM 16 bit, unsigned, little-endian
    private class AudioFloatConversion16UL : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                val x: Int =
                    (in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8.toByte())).toInt()
                out_buff[ox++] = (x - 32767) * (1.0f / 32767.0f)
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = 32767 + (in_buff[ix++] * 32767.0).toInt()
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
            }
            return out_buff
        }
    }

    // PCM 16 bit, unsigned, big-endian
    private class AudioFloatConversion16UB : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                val x: Int =
                    (in_buff[ix++] and 0xFF.toByte() shl 8.toByte() or (in_buff[ix++] and 0xFF.toByte())).toInt()
                out_buff[ox++] = (x - 32767) * (1.0f / 32767.0f)
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = 32767 + (in_buff[ix++] * 32767.0).toInt()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
            }
            return out_buff
        }
    }

    /***************************************************************************
     *
     * 24 bit signed/unsigned, little/big-endian
     *
     */
    // PCM 24 bit, signed, little-endian
    private class AudioFloatConversion24SL : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = ((in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8)
                        or (in_buff[ix++] and 0xFF.toByte() shl 16)).toInt())
                if (x > 0x7FFFFF) x -= 0x1000000
                out_buff[ox++] = x * (1.0f / 0x7FFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFF.toFloat()).toInt()
                if (x < 0) x += 0x1000000
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
            }
            return out_buff
        }
    }

    // PCM 24 bit, signed, big-endian
    private class AudioFloatConversion24SB : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = ((in_buff[ix++] and 0xFF.toByte() shl 16
                        or (in_buff[ix++] and 0xFF.toByte() shl 8) or (in_buff[ix++] and 0xFF.toByte())).toInt())
                if (x > 0x7FFFFF) x -= 0x1000000
                out_buff[ox++] = x * (1.0f / 0x7FFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFF.toFloat()).toInt()
                if (x < 0) x += 0x1000000
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
            }
            return out_buff
        }
    }

    // PCM 24 bit, unsigned, little-endian
    private class AudioFloatConversion24UL : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = ((in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8)
                        or (in_buff[ix++] and 0xFF.toByte() shl 16)).toInt())
                x -= 0x7FFFFF
                out_buff[ox++] = x * (1.0f / 0x7FFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFF.toFloat()).toInt()
                x += 0x7FFFFF
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
            }
            return out_buff
        }
    }

    // PCM 24 bit, unsigned, big-endian
    private class AudioFloatConversion24UB : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = ((in_buff[ix++] and 0xFF.toByte() shl 16
                        or (in_buff[ix++] and 0xFF.toByte() shl 8) or (in_buff[ix++] and 0xFF.toByte())).toInt())
                x -= 0x7FFFFF
                out_buff[ox++] = x * (1.0f / 0x7FFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFF.toFloat()).toInt()
                x += 0x7FFFFF
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
            }
            return out_buff
        }
    }

    /***************************************************************************
     *
     * 32 bit signed/unsigned, little/big-endian
     *
     */
    // PCM 32 bit, signed, little-endian
    private class AudioFloatConversion32SL : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                val x: Int = (in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8) or
                        (in_buff[ix++] and 0xFF.toByte() shl 16) or
                        (in_buff[ix++] and 0xFF.toByte() shl 24)).toInt()
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 24).toByte()
            }
            return out_buff
        }
    }

    // PCM 32 bit, signed, big-endian
    private class AudioFloatConversion32SB : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                val x: Int = (in_buff[ix++] and 0xFF.toByte() shl 24 or
                        (in_buff[ix++] and 0xFF.toByte() shl 16) or
                        (in_buff[ix++] and 0xFF.toByte() shl 8) or (in_buff[ix++] and 0xFF.toByte())).toInt()
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                out_buff[ox++] = (x ushr 24).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
            }
            return out_buff
        }
    }

    // PCM 32 bit, unsigned, little-endian
    private class AudioFloatConversion32UL : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = (in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8) or
                        (in_buff[ix++] and 0xFF.toByte() shl 16) or
                        (in_buff[ix++] and 0xFF.toByte() shl 24)).toInt()
                x -= 0x7FFFFFFF
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                x += 0x7FFFFFFF
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 24).toByte()
            }
            return out_buff
        }
    }

    // PCM 32 bit, unsigned, big-endian
    private class AudioFloatConversion32UB : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = (in_buff[ix++] and 0xFF.toByte() shl 24 or
                        (in_buff[ix++] and 0xFF.toByte() shl 16) or
                        (in_buff[ix++] and 0xFF.toByte() shl 8) or (in_buff[ix++] and 0xFF.toByte())).toInt()
                x -= 0x7FFFFFFF
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                x += 0x7FFFFFFF
                out_buff[ox++] = (x ushr 24).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
            }
            return out_buff
        }
    }

    /***************************************************************************
     *
     * 32+ bit signed/unsigned, little/big-endian
     *
     */
    // PCM 32+ bit, signed, little-endian
    private class AudioFloatConversion32xSL(val xbytes: Int) : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                ix += xbytes
                val x: Int = ((in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8)
                        or (in_buff[ix++] and 0xFF.toByte() shl 16)
                        or (in_buff[ix++] and 0xFF.toByte() shl 24)).toInt())
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                for (j in 0 until xbytes) {
                    out_buff[ox++] = 0
                }
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 24).toByte()
            }
            return out_buff
        }

    }

    // PCM 32+ bit, signed, big-endian
    private class AudioFloatConversion32xSB(val xbytes: Int) : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                val x: Int = ((in_buff[ix++] and 0xFF.toByte() shl 24
                        or (in_buff[ix++] and 0xFF.toByte() shl 16)
                        or (in_buff[ix++] and 0xFF.toByte() shl 8)
                        or (in_buff[ix++] and 0xFF.toByte())).toInt())
                ix += xbytes
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                val x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                out_buff[ox++] = (x ushr 24).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
                for (j in 0 until xbytes) {
                    out_buff[ox++] = 0
                }
            }
            return out_buff
        }

    }

    // PCM 32+ bit, unsigned, little-endian
    private class AudioFloatConversion32xUL(val xbytes: Int) : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                ix += xbytes
                var x: Int = ((in_buff[ix++] and 0xFF.toByte() or (in_buff[ix++] and 0xFF.toByte() shl 8)
                        or (in_buff[ix++] and 0xFF.toByte() shl 16)
                        or (in_buff[ix++] and 0xFF.toByte() shl 24)).toInt())
                x -= 0x7FFFFFFF
                out_buff[ox++] = x * (1.0f / 0x7FFFFFFF.toFloat())
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 0x7FFFFFFF.toFloat()).toInt()
                x += 0x7FFFFFFF
                for (j in 0 until xbytes) {
                    out_buff[ox++] = 0
                }
                out_buff[ox++] = x.toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 24).toByte()
            }
            return out_buff
        }

    }

    // PCM 32+ bit, unsigned, big-endian
    private class AudioFloatConversion32xUB(val xbytes: Int) : TarsosDSPAudioFloatConverter() {
        override fun toFloatArray(
            in_buff: ByteArray, in_offset: Int,
            out_buff: FloatArray, out_offset: Int, out_len: Int
        ): FloatArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until out_len) {
                var x: Int = (in_buff[ix++] and 0xFF.toByte() shl 24.toByte() or
                        (in_buff[ix++] and 0xFF.toByte() shl 16.toByte()) or
                        (in_buff[ix++] and 0xFF.toByte() shl 8.toByte()) or (in_buff[ix++] and 0xFF.toByte())).toInt()
                ix += xbytes
                x -= 2147483647
                out_buff[ox++] = x * (1.0f / 2147483647.0f)
            }
            return out_buff
        }

        override fun toByteArray(
            in_buff: FloatArray, in_offset: Int, in_len: Int,
            out_buff: ByteArray, out_offset: Int
        ): ByteArray {
            var ix = in_offset
            var ox = out_offset
            for (i in 0 until in_len) {
                var x = (in_buff[ix++] * 2147483647.0).toInt()
                x += 2147483647
                out_buff[ox++] = (x ushr 24).toByte()
                out_buff[ox++] = (x ushr 16).toByte()
                out_buff[ox++] = (x ushr 8).toByte()
                out_buff[ox++] = x.toByte()
                for (j in 0 until xbytes) {
                    out_buff[ox++] = 0
                }
            }
            return out_buff
        }

    }

    companion object {
        @JvmStatic
        fun getConverter(format: TarsosDSPAudioFormat): TarsosDSPAudioFloatConverter? {
            var conv: TarsosDSPAudioFloatConverter? = null
            if (format.frameSize == 0) return null
            if (format.frameSize !=
                (format.sampleSizeInBits + 7) / 8 * format.channels
            ) {
                return null
            }
            if (format.encoding == TarsosDSPAudioFormat.Encoding.PCM_SIGNED) {
                if (format.isBigEndian) {
                    when {
                        format.sampleSizeInBits <= 8 -> {
                            conv = AudioFloatConversion8S()
                        }
                        format.sampleSizeInBits in 9..16
                        -> {
                            conv = AudioFloatConversion16SB()
                        }
                        format.sampleSizeInBits in 17..24
                        -> {
                            conv = AudioFloatConversion24SB()
                        }
                        format.sampleSizeInBits in 25..32
                        -> {
                            conv = AudioFloatConversion32SB()
                        }
                        format.sampleSizeInBits > 32 -> {
                            conv = AudioFloatConversion32xSB(
                                (format
                                    .sampleSizeInBits + 7) / 8 - 4
                            )
                        }
                    }
                } else {
                    when {
                        format.sampleSizeInBits <= 8 -> {
                            conv = AudioFloatConversion8S()
                        }
                        format.sampleSizeInBits in 9..16
                        -> {
                            conv = AudioFloatConversion16SL()
                        }
                        format.sampleSizeInBits in 17..24
                        -> {
                            conv = AudioFloatConversion24SL()
                        }
                        format.sampleSizeInBits in 25..32
                        -> {
                            conv = AudioFloatConversion32SL()
                        }
                        format.sampleSizeInBits > 32 -> {
                            conv = AudioFloatConversion32xSL(
                                (format
                                    .sampleSizeInBits + 7) / 8 - 4
                            )
                        }
                    }
                }
            } else if (format.encoding == TarsosDSPAudioFormat.Encoding.PCM_UNSIGNED) {
                if (format.isBigEndian) {
                    when {
                        format.sampleSizeInBits <= 8 -> {
                            conv = AudioFloatConversion8U()
                        }
                        format.sampleSizeInBits in 9..16
                        -> {
                            conv = AudioFloatConversion16UB()
                        }
                        format.sampleSizeInBits in 17..24
                        -> {
                            conv = AudioFloatConversion24UB()
                        }
                        format.sampleSizeInBits in 25..32
                        -> {
                            conv = AudioFloatConversion32UB()
                        }
                        format.sampleSizeInBits > 32 -> {
                            conv =
                                AudioFloatConversion32xUB((format.sampleSizeInBits + 7) / 8 - 4)
                        }
                    }
                } else {
                    when {
                        format.sampleSizeInBits <= 8 -> {
                            conv = AudioFloatConversion8U()
                        }
                        format.sampleSizeInBits in 9..16
                        -> {
                            conv = AudioFloatConversion16UL()
                        }
                        format.sampleSizeInBits in 17..24
                        -> {
                            conv = AudioFloatConversion24UL()
                        }
                        format.sampleSizeInBits in 25..32
                        -> {
                            conv = AudioFloatConversion32UL()
                        }
                        format.sampleSizeInBits > 32 -> {
                            conv =
                                AudioFloatConversion32xUL((format.sampleSizeInBits + 7) / 8 - 4)
                        }
                    }
                }
            } else if (format.encoding == TarsosDSPAudioFormat.Encoding.PCM_FLOAT) {
                if (format.sampleSizeInBits == 32) {
                    conv = if (format.isBigEndian) AudioFloatConversion32B() else AudioFloatConversion32L()
                } else if (format.sampleSizeInBits == 64) {
                    conv = if (format.isBigEndian) AudioFloatConversion64B() else AudioFloatConversion64L()
                }
            }
            if ((format.encoding == TarsosDSPAudioFormat.Encoding.PCM_SIGNED || format.encoding == TarsosDSPAudioFormat.Encoding.PCM_UNSIGNED) &&
                format.sampleSizeInBits % 8 != 0
            ) {
                conv = AudioFloatLSBFilter(conv, format)
            }
            if (conv != null) conv.format = format
            return conv
        }
    }
}