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
/**********************************************************
 *
 * Class CubicSplineFast
 *
 * Class for performing an interpolation using a cubic spline
 * setTabulatedArrays and interpolate adapted, with modification to
 * an object-oriented approach, from Numerical Recipes in C (http://www.nr.com/)
 * Stripped down version of CubicSpline - all data checks have been removed for faster running
 *
 *
 * WRITTEN BY: Dr Michael Thomas Flanagan
 *
 * DATE:	26 December 2009 (Stripped down version of CubicSpline: May 2002 - 31 October 2009)
 * UPDATE: 14  January 2010
 *
 * DOCUMENTATION:
 * See Michael Thomas Flanagan's Java library on-LineWavelet web page:
 * http://www.ee.ucl.ac.uk/~mflanaga/java/CubicSplineFast.html
 * http://www.ee.ucl.ac.uk/~mflanaga/java/
 *
 * Copyright (c) 2002 - 2010  Michael Thomas Flanagan
 *
 * PERMISSION TO COPY:
 *
 * Permission to use, copy and modify this software and its documentation for NON-COMMERCIAL purposes is granted, without fee,
 * provided that an acknowledgement to the author, Dr Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all copies
 * and associated documentation or publications.
 *
 * Redistributions of the source code of this source code, or parts of the source codes, must retain the above copyright notice,
 * this list of conditions and the following disclaimer and requires written permission from the Michael Thomas Flanagan:
 *
 * Redistribution in binary form of all or parts of this class must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution and requires written permission
 * from the Michael Thomas Flanagan:
 *
 * Dr Michael Thomas Flanagan makes no representations about the suitability or fitness of the software for any or for a particular purpose.
 * Dr Michael Thomas Flanagan shall not be liable for any damages suffered as a result of using, modifying or distributing this software
 * or its derivatives.
 *
 */
package be.tarsos.dsp.util

class CubicSplineFast {
    private var nPoints = 0 // no. of tabulated points
    private var y: DoubleArray // y=f(x) tabulated function
    private var x: DoubleArray // x in tabulated function f(x)
    private var d2ydx2: DoubleArray // second derivatives of y

    // Constructors
    // Constructor with data arrays initialised to arrays x and y
    constructor(x: DoubleArray, y: DoubleArray) {
        nPoints = x.size
        this.x = DoubleArray(nPoints)
        this.y = DoubleArray(nPoints)
        d2ydx2 = DoubleArray(nPoints)
        for (i in 0 until nPoints) {
            this.x[i] = x[i]
            this.y[i] = y[i]
        }
        calcDeriv()
    }

    // Constructor with data arrays initialised to zero
    // Primarily for use by BiCubicSplineFast
    constructor(nPoints: Int) {
        this.nPoints = nPoints
        x = DoubleArray(nPoints)
        y = DoubleArray(nPoints)
        d2ydx2 = DoubleArray(nPoints)
    }

    // Resets the x y data arrays - primarily for use in BiCubicSplineFast
    fun resetData(x: DoubleArray, y: DoubleArray) {
        for (i in 0 until nPoints) {
            this.x[i] = x[i]
            this.y[i] = y[i]
        }
    }

    //  Calculates the second derivatives of the tabulated function
    //  for use by the cubic spline interpolation method (.interpolate)
    //  This method follows the procedure in Numerical Methods C language procedure for calculating second derivatives
    fun calcDeriv() {
        var p = 0.0
        var qn = 0.0
        var sig = 0.0
        var un = 0.0
        val u = DoubleArray(nPoints)
        u[0] = 0.0
        d2ydx2[0] = u[0]
        for (i in 1..nPoints - 2) {
            sig = (x[i] - x[i - 1]) / (x[i + 1] - x[i - 1])
            p = sig * d2ydx2[i - 1] + 2.0
            d2ydx2[i] = (sig - 1.0) / p
            u[i] =
                (y[i + 1] - y[i]) / (x[i + 1] - x[i]) - (y[i] - y[i - 1]) / (x[i] - x[i - 1])
            u[i] = (6.0 * u[i] / (x[i + 1] - x[i - 1]) - sig * u[i - 1]) / p
        }
        un = 0.0
        qn = un
        d2ydx2[nPoints - 1] =
            (un - qn * u[nPoints - 2]) / (qn * d2ydx2[nPoints - 2] + 1.0)
        for (k in nPoints - 2 downTo 0) {
            d2ydx2[k] = d2ydx2[k] * d2ydx2[k + 1] + u[k]
        }
    }

    //  INTERPOLATE
    //  Returns an interpolated value of y for a value of x from a tabulated function y=f(x)
    //  after the data has been entered via a constructor.
    //  The derivatives are calculated, bt calcDeriv(), on the first call to this method ands are
    //  then stored for use on all subsequent calls
    fun interpolate(xx: Double): Double {
        var h = 0.0
        var b = 0.0
        var a = 0.0
        var yy = 0.0
        var k = 0
        var klo = 0
        var khi = nPoints - 1
        while (khi - klo > 1) {
            k = khi + klo shr 1
            if (x[k] > xx) {
                khi = k
            } else {
                klo = k
            }
        }
        h = x[khi] - x[klo]
        require(h != 0.0) { "Two values of x are identical: point $klo (" + x[klo] + ") and point " + khi + " (" + x[khi] + ")" }
        a = (x[khi] - xx) / h
        b = (xx - x[klo]) / h
        yy =
            a * y[klo] + b * y[khi] + ((a * a * a - a) * d2ydx2[klo] + (b * b * b - b) * d2ydx2[khi]) * (h * h) / 6.0
        return yy
    }

    companion object {
        // METHODS
        // Returns a new CubicSplineFast setting array lengths to n and all array values to zero with natural spline default
        // Primarily for use in BiCubicSplineFast
        fun zero(n: Int): CubicSplineFast {
            require(n >= 3) { "A minimum of three data points is needed" }
            return CubicSplineFast(n)
        }

        // Create a one dimensional array of cubic spline objects of length n each of array length m
        // Primarily for use in BiCubicSplineFast
        fun oneDarray(n: Int, m: Int): Array<CubicSplineFast?> {
            val a = arrayOfNulls<CubicSplineFast>(n)
            for (i in 0 until n) {
                a[i] = zero(m)
            }
            return a
        }
    }
}