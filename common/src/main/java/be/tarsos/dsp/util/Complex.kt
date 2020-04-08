package be.tarsos.dsp.util

import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.pow

/**
 * Complex implements a complex number and defines complex
 * arithmetic and mathematical functions
 * Last Updated February 27, 2001
 * Copyright 1997-2001
 *
 * @author Andrew G. Bennett
 * @author joren
 * @version 1.0
 */
data class Complex
/**
 * Constructs the complex number z = u + i*v
 *
 * @param real Real part
 * @param imag Imaginary part
 */(val real: Double, val imag: Double) {

    /**
     * Modulus of this Complex number
     * (the distance from the origin in polar coordinates).
     *
     * @return |z| where z is this Complex number.
     */
    fun mod(): Double {
        return if (real != 0.0 || imag != 0.0) {
            kotlin.math.sqrt(real * real + imag * imag)
        } else {
            0.0
        }
    }

    /**
     * Argument of this Complex number
     * (the angle in radians with the x-axis in polar coordinates).
     *
     * @return arg(z) where z is this Complex number.
     */
    fun arg(): Double {
        return atan2(imag, real)
    }

    /**
     * Complex conjugate of this Complex number
     * (the conjugate of x+i*y is x-i*y).
     *
     * @return z-bar where z is this Complex number.
     */
    fun conj(): Complex {
        return Complex(real, -imag)
    }

    /**
     * Addition of Complex numbers (doesn't change this Complex number).
     * <br></br>(x+i*y) + (s+i*t) = (x+s)+i*(y+t).
     *
     * @param w is the number to add.
     * @return z+w where z is this Complex number.
     */
    operator fun plus(w: Complex): Complex {
        return Complex(real + w.real, imag + w.imag)
    }

    /**
     * Subtraction of Complex numbers (doesn't change this Complex number).
     * <br></br>(x+i*y) - (s+i*t) = (x-s)+i*(y-t).
     *
     * @param w is the number to subtract.
     * @return z-w where z is this Complex number.
     */
    operator fun minus(w: Complex): Complex {
        return Complex(real - w.real, imag - w.imag)
    }

    /**
     * Complex multiplication (doesn't change this Complex number).
     *
     * @param w is the number to multiply by.
     * @return z*w where z is this Complex number.
     */
    operator fun times(w: Complex): Complex {
        return Complex(real * w.real - imag * w.imag, real * w.imag + imag * w.real)
    }

    /**
     * Division of Complex numbers (doesn't change this Complex number).
     * <br></br>(x+i*y)/(s+i*t) = ((x*s+y*t) + i*(y*s-y*t)) / (s^2+t^2)
     *
     * @param w is the number to divide by
     * @return new Complex number z/w where z is this Complex number
     */
    operator fun div(w: Complex): Complex {
        val den = w.mod().pow(2.0)
        return Complex((real * w.real + imag * w.imag) / den, (imag * w.real - real * w.imag) / den)
    }

    /**
     * Complex exponential (doesn't change this Complex number).
     *
     * @return exp(z) where z is this Complex number.
     */
    fun exp(): Complex {
        return Complex(kotlin.math.exp(real) * kotlin.math.cos(imag), kotlin.math.exp(real) * kotlin.math.sin(imag))
    }

    /**
     * Principal branch of the Complex logarithm of this Complex number.
     * (doesn't change this Complex number).
     * The principal branch is the branch with -pi < arg <= pi.
     *
     * @return log(z) where z is this Complex number.
     */
    fun log(): Complex {
        return Complex(ln(this.mod()), arg())
    }

    /**
     * Complex square root (doesn't change this complex number).
     * Computes the principal branch of the square root, which
     * is the value with 0 <= arg < pi.
     *
     * @return sqrt(z) where z is this Complex number.
     */
    fun sqrt(): Complex {
        val r = kotlin.math.sqrt(this.mod())
        val theta = arg() / 2
        return Complex(r * kotlin.math.cos(theta), r * kotlin.math.sin(theta))
    }

    // Real cosh function (used to compute complex trig functions)
    private fun cosh(theta: Double): Double {
        return (kotlin.math.exp(theta) + kotlin.math.exp(-theta)) / 2
    }

    // Real sinh function (used to compute complex trig functions)
    private fun sinh(theta: Double): Double {
        return (kotlin.math.exp(theta) - kotlin.math.exp(-theta)) / 2
    }

    /**
     * Sine of this Complex number (doesn't change this Complex number).
     * <br></br>sin(z) = (exp(i*z)-exp(-i*z))/(2*i).
     *
     * @return sin(z) where z is this Complex number.
     */
    fun sin(): Complex {
        return Complex(cosh(imag) * kotlin.math.sin(real), sinh(imag) * kotlin.math.cos(real))
    }

    /**
     * Cosine of this Complex number (doesn't change this Complex number).
     * <br></br>cos(z) = (exp(i*z)+exp(-i*z))/ 2.
     *
     * @return cos(z) where z is this Complex number.
     */
    fun cos(): Complex {
        return Complex(cosh(imag) * kotlin.math.cos(real), -sinh(imag) * kotlin.math.sin(real))
    }

    /**
     * Hyperbolic sine of this Complex number
     * (doesn't change this Complex number).
     * <br></br>sinh(z) = (exp(z)-exp(-z))/2.
     *
     * @return sinh(z) where z is this Complex number.
     */
    fun sinh(): Complex {
        return Complex(sinh(real) * kotlin.math.cos(imag), cosh(real) * kotlin.math.sin(imag))
    }

    /**
     * Hyperbolic cosine of this Complex number
     * (doesn't change this Complex number).
     * <br></br>cosh(z) = (exp(z) + exp(-z)) / 2.
     *
     * @return cosh(z) where z is this Complex number.
     */
    fun cosh(): Complex {
        return Complex(cosh(real) * kotlin.math.cos(imag), sinh(real) * kotlin.math.sin(imag))
    }

    /**
     * Tangent of this Complex number (doesn't change this Complex number).
     * <br></br>tan(z) = sin(z)/cos(z).
     *
     * @return tan(z) where z is this Complex number.
     */
    fun tan(): Complex {
        return sin().div(cos())
    }

    /**
     * Negative of this complex number (chs stands for change sign).
     * This produces a new Complex number and doesn't change
     * this Complex number.
     * <br></br>-(x+i*y) = -x-i*y.
     *
     * @return -z where z is this Complex number.
     */
    fun chs(): Complex {
        return Complex(-real, -imag)
    }

    /**
     * String representation of this Complex number.
     *
     * @return x+i*y, x-i*y, x, or i*y as appropriate.
     */
    override fun toString(): String {
        if (real != 0.0 && imag > 0) {
            return real.toString() + " + " + imag + "i"
        }
        if (real != 0.0 && imag < 0) {
            return real.toString() + " - " + -imag + "i"
        }
        if (imag == 0.0) {
            return real.toString()
        }
        return if (real == 0.0) {
            imag.toString() + "i"
        } else "$real + i*$imag"
        // shouldn't get here (unless Inf or NaN)
    }

}