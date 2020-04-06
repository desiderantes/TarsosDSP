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
package be.tarsos.dsp.ui

import be.tarsos.dsp.ui.layers.Layer
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.*
import java.awt.geom.AffineTransform
import java.util.*
import javax.swing.JPanel

class LinkedPanel(coordinateSystem: CoordinateSystem) : JPanel() {
    val viewPort: ViewPort
    private val layers: MutableList<Layer>
    val coordinateSystem: CoordinateSystem

    fun addLayer(l: Layer) {
        layers.add(l)
        if (l is MouseMotionListener) {
            addMouseMotionListener(l as MouseMotionListener)
        }
        if (l is MouseListener) {
            addMouseListener(l as MouseListener)
        }
        if (l is MouseWheelListener) {
            addMouseWheelListener(l as MouseWheelListener)
        }
        if (l is KeyListener) {
            addKeyListener(l as KeyListener)
        }
    }

    val transform: AffineTransform
        get() {
            val xDelta = coordinateSystem.getDelta(Axis.X).toDouble()
            val yDelta = coordinateSystem.getDelta(Axis.Y).toDouble()
            val transform = AffineTransform()
            transform.translate(0.0, height.toDouble())
            transform.scale(width / xDelta, -height / yDelta)
            transform.translate(
                -coordinateSystem.getMin(Axis.X).toDouble(),
                -coordinateSystem.getMin(Axis.Y).toDouble()
            )
            return transform
        }

    fun updateTransform(transform: AffineTransform): AffineTransform {
        val xDelta = coordinateSystem.getDelta(Axis.X).toDouble()
        val yDelta = coordinateSystem.getDelta(Axis.Y).toDouble()
        transform.translate(0.0, height.toDouble())
        transform.scale(width / xDelta, -height / yDelta)
        transform.translate(
            -coordinateSystem.getMin(Axis.X).toDouble(),
            -coordinateSystem.getMin(Axis.Y).toDouble()
        )
        return transform
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val graphics = g.create() as Graphics2D
        graphics.transform = updateTransform(graphics.transform)
        if (layers.isNotEmpty()) {
            for (layer in layers) {
                layer.draw(graphics)
            }
        }
    }

    fun removeLayer(layer: Layer) {
        layers.remove(layer)
        if (layer is MouseMotionListener) {
            removeMouseMotionListener(layer as MouseMotionListener)
        }
        if (layer is MouseListener) {
            removeMouseListener(layer as MouseListener)
        }
        if (layer is MouseWheelListener) {
            removeMouseWheelListener(layer as MouseWheelListener)
        }
        if (layer is KeyListener) {
            removeKeyListener(layer as KeyListener)
        }
    }

    fun removeLayers() {
        while (layers.size > 0) {
            removeLayer(layers[0])
        }
    }

    companion object {
        private const val serialVersionUID = -5055686566048886896L
    }

    init {
        //makes sure key events are registered
        this.isFocusable = true
        layers = ArrayList()
        this.coordinateSystem = coordinateSystem
        viewPort = ViewPort(this.coordinateSystem)
        this.isVisible = true

        //regain focus on mouse enter to get key presses
        addMouseListener(object : MouseListener {
            override fun mouseReleased(e: MouseEvent) {}
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {
                this@LinkedPanel.transferFocusBackward()
            }

            override fun mouseEntered(e: MouseEvent) {
                this@LinkedPanel.requestFocus()
            }

            override fun mouseClicked(e: MouseEvent) {}
        })
    }
}