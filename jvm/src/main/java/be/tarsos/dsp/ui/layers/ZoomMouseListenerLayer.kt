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

import be.tarsos.dsp.ui.LinkedPanel
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

class ZoomMouseListenerLayer : MouseAdapter(), Layer {
    override fun draw(graphics: Graphics2D) {
        //draw nothing, react to mouse events
    }

    override val name: String
        get() = "Zoom mouse listener"

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        val panel = e.component as LinkedPanel
        val amount = e.wheelRotation * e.scrollAmount
        panel.viewPort.zoom(amount, e.point)
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
            val panel = e.component as LinkedPanel
            panel.viewPort.resetZoom()
        }
    }
}