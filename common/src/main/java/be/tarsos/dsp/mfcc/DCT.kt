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
package be.tarsos.dsp.mfcc

import be.tarsos.dsp.util.PI
import java.io.*
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

class DCT(var f: Array<IntArray>) {
    fun transform(): Array<IntArray> {
        val g = Array(8) { IntArray(8) }
        for (i in 0..7) {
            for (j in 0..7) {
                var ge = 0.0
                for (x in 0..7) {
                    for (y in 0..7) {
                        val cg1 =
                            (2.0 * x.toDouble() + 1.0) * i.toDouble() * PI / 16.0
                        val cg2 =
                            (2.0 * y.toDouble() + 1.0) * j.toDouble() * PI / 16.0
                        ge += f[x][y].toDouble() * cos(cg1) * cos(
                            cg2
                        )
                    }
                }
                val ci = if (i == 0) 1.0 / sqrt(2.0) else 1.0
                val cj = if (j == 0) 1.0 / sqrt(2.0) else 1.0
                ge *= ci * cj * 0.25
                g[i][j] = ge.roundToInt()
            }
        }
        return g
    }

    fun inverse(g: Array<IntArray>): Array<IntArray> {
        val inv = Array(8) { IntArray(8) }
        for (x in 0..7) {
            for (y in 0..7) {
                var ge = 0.0
                for (i in 0..7) {
                    val cg1 =
                        (2.0 * x.toDouble() + 1.0) * i.toDouble() * PI / 16.0
                    val ci = if (i == 0) 1.0 / sqrt(2.0) else 1.0
                    for (j in 0..7) {
                        val cg2 =
                            (2.0 * y.toDouble() + 1.0) * j.toDouble() * PI / 16.0
                        val cj =
                            if (j == 0) 1.0 / sqrt(2.0) else 1.0
                        val cij4 = ci * cj * 0.25
                        ge += cij4 * cos(cg1) * cos(cg2) * g[i][j].toDouble()
                    }
                }
                inv[x][y] = ge.roundToInt()
            }
        }
        return inv
    }

    fun printout(g: Array<IntArray>) {
        for (i in 0..7) {
            print("\n")
            for (k in 0..7) {
                print(g[i][k].toString() + " ")
            }
        }
    }

    fun printoutinv(inv: Array<IntArray>) {
        for (i in 0..7) {
            print("\n")
            for (k in 0..7) {
                print(inv[i][k].toString() + " ")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val fm = Array(8) { IntArray(8) }
            if (args.size != 1) {
                println("usage: java DCT <matrix-filename>")
                return
            }
            val f = File(args[0])
            if (!f.canRead()) {
                println("Error! can't open " + args[0] + " for reading")
                return
            }
            try {
                val br = BufferedReader(FileReader(f))
                for (i in 0..7) {
                    val line = br.readLine()
                    val tok = StringTokenizer(line, ", ")
                    if (tok.countTokens() != 8) {
                        println("Error! File format error: 8 tokens required!")
                        throw IOException("Error")
                    }
                    for (j in 0..7) {
                        val numstr = tok.nextToken()
                        val num = numstr.toInt()
                        fm[i][j] = num
                    }
                }
                br.close()
            } catch (e: FileNotFoundException) {
                println("Error! can't create FileReader for " + args[0])
                return
            } catch (e: IOException) {
                println("Error! during read of " + args[0])
                return
            } catch (e: NumberFormatException) {
                println("Error! NumberFormatExecption")
                return
            }
            val dct = DCT(fm)
            val g = dct.transform()
            dct.printout(g)
            val inv = dct.inverse(g)
            dct.printoutinv(inv)
        }
    }

}