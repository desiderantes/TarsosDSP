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
package be.tarsos.dsp.ui.layers

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.roundToInt

object LayerUtilities {
    /**
     * Transforms pixels to time and frequency.
     *
     * @param g The current graphics, with a meaningful transform applied to it.
     * @param x The x coordinate, in pixels.
     * @param y The y coordinate, in pixels.
     * @return A point with time (in milliseconds) as x coordinate, and frequency (in cents) as y coordinate.
     */
    @JvmStatic
    fun pixelsToUnits(g: Graphics2D, x: Int, y: Int): Point2D? {
        var units: Point2D? = null
        try {
            units = g.transform.inverseTransform(Point2D.Double(x.toDouble(), y.toDouble()), null)
        } catch (e1: NoninvertibleTransformException) {
            e1.printStackTrace()
        }
        return units
    }

    /**
     * Transforms a number of pixels into a corresponding time or frequency span. E.g. 10 horizontal
     * pixels could translate to 320 milliseconds. 10 vertical pixels could translate to 32cents.
     *
     * @param g          The current graphics, with a meaningful transform applied to it.
     * @param pixels     The number of pixels
     * @param horizontal Is it the horizontal or vertical axis?
     * @return A number of cents or milliseconds.
     */
    @JvmStatic
    fun pixelsToUnits(g: Graphics2D, pixels: Int, horizontal: Boolean): Float {
        var numberOfUnits = 0f
        try {
            val originSrc: Point2D = Point2D.Double(0.0, 0.0)
            val originDest: Point2D
            originDest = g.transform.inverseTransform(originSrc, null)
            val destSrc: Point2D = Point2D.Double(pixels.toDouble(), pixels.toDouble())
            val destDest: Point2D
            destDest = g.transform.inverseTransform(destSrc, null)
            numberOfUnits = if (horizontal) {
                (destDest.x - originDest.x).toFloat()
            } else {
                (-destDest.y + originDest.y).toFloat()
            }
        } catch (e: NoninvertibleTransformException) {
            e.printStackTrace()
        }
        return numberOfUnits
    }

    @JvmStatic
    fun unitsToPixels(
        g: Graphics2D,
        units: Float,
        horizontal: Boolean
    ): Float {
        val firstSource: Point2D = Point2D.Float(units, units)
        val firstDest: Point2D = Point2D.Float(0F, 0F)
        val secondSource: Point2D = Point2D.Float(0F, 0F)
        val secondDest: Point2D = Point2D.Float(0F, 0F)
        g.transform.transform(firstSource, firstDest)
        g.transform.transform(secondSource, secondDest)
        return if (horizontal) (firstDest.x - secondDest.x).toFloat() else (firstDest.y - secondDest.y).toFloat()
    }

    @JvmStatic
    @JvmOverloads
    fun drawString(
        graphics: Graphics2D,
        text: String,
        x: Double,
        y: Double,
        centerHorizontal: Boolean,
        centerVertical: Boolean,
        backgroundColor: Color?,
        textColor: Color = Color.BLACK
    ): Rectangle2D {
        val transform = graphics.transform
        val source: Point2D = Point2D.Double(x, y)
        val destination: Point2D = Point2D.Double()
        transform.transform(source, destination)
        try {
            transform.invert()
        } catch (e1: NoninvertibleTransformException) {
            e1.printStackTrace()
        }
        graphics.transform(transform)
        val r = graphics.fontMetrics.getStringBounds(text, graphics)
        val xPosition =
            ((destination.x - if (centerHorizontal) r.width / 2.0 - 1 else 0.0)).roundToInt()
        val yPosition =
            ((destination.y + if (centerVertical) r.height / 2.0 - 1.5 else 0.0)).roundToInt()
        if (backgroundColor != null) {
            graphics.color = backgroundColor
            val width = (r.maxY - r.minY).toInt()
            val height = (r.maxX - r.minX).toInt()
            graphics.fillRect(xPosition, yPosition - width, height, width)
        }
        val boundingRectangle: Rectangle2D =
            Rectangle2D.Double(xPosition.toDouble(), yPosition - r.height, r.width, r.height)
        transform.createTransformedShape(boundingRectangle)
        graphics.color = textColor
        graphics.drawString(text, xPosition, yPosition)
        try {
            transform.invert()
        } catch (e1: NoninvertibleTransformException) {
            e1.printStackTrace()
        }
        graphics.transform(transform)
        return boundingRectangle
    }
}