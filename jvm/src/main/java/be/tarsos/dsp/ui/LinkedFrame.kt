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

import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener
import be.tarsos.dsp.ui.layers.*
import be.tarsos.dsp.ui.layers.pch.PitchClassHistogramLayer
import be.tarsos.dsp.ui.layers.pch.ScaleLayer
import java.awt.Color
import java.awt.Dimension
import java.io.File
import java.util.*
import javax.swing.JFrame
import javax.swing.JSplitPane

class LinkedFrame private constructor() : JFrame(), ViewPortChangedListener {
    protected var lastSplitPane: JSplitPane? = null
        private set
    private var drawing = false

    fun initialise() {
        this.minimumSize = Dimension(800, 400)
        val contentPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        lastSplitPane = contentPane
        this.contentPane = contentPane
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        pack()
        contentPane.dividerLocation = 0
        buildStdSetUp()
        isVisible = true
    }

    fun createNewSplitPane() {
        lastSplitPane!!.dividerSize = 2
        val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        sp.dividerSize = 0
        lastSplitPane!!.add(sp, JSplitPane.BOTTOM)
        lastSplitPane = sp
    }

    override fun viewPortChanged(newViewPort: ViewPort?) {
        if (!drawing) {
            drawing = true
            for (panel in panels.values) {
                panel.repaint()
            }
            drawing = false
        }
    }

    private fun getCoordinateSystem(yUnits: AxisUnit): CoordinateSystem {
        var minValue = -1000f
        var maxValue = 1000f
        if (yUnits === AxisUnit.FREQUENCY) {
            minValue = 200f
            maxValue = 8000f
        }
        return CoordinateSystem(yAxisUnits = yUnits, yMin = minValue, yMax = maxValue)
    }

    private fun buildStdSetUp() {
        var cs = getCoordinateSystem(AxisUnit.AMPLITUDE)
        val audioFile =
            File("/home/joren/Desktop/08._Ladrang_Kandamanyura_10s-20s.wav")
        var panel = LinkedPanel(cs)
        panel.addLayer(ZoomMouseListenerLayer())
        panel.addLayer(DragMouseListenerLayer(cs))
        panel.addLayer(BackgroundLayer(cs))
        panel.addLayer(AmplitudeAxisLayer(cs))
        panel.addLayer(TimeAxisLayer(cs))
        panel.addLayer(WaveFormLayer(cs, audioFile))
        panel.addLayer(BeatLayer(cs, audioFile, true, true))
        panel.addLayer(SelectionLayer(cs))
        val legend = LegendLayer(cs, 50)
        panel.addLayer(legend)
        legend.addEntry("Onsets", Color.BLUE)
        legend.addEntry("Beats", Color.RED)
        panel.viewPort.addViewPortChangedListener(this)
        panels["Waveform"] = panel
        lastSplitPane!!.add(panel, JSplitPane.TOP)
        cs = getCoordinateSystem(AxisUnit.FREQUENCY)
        panel = LinkedPanel(cs)
        panel.addLayer(ZoomMouseListenerLayer())
        panel.addLayer(DragMouseListenerLayer(cs))
        panel.addLayer(BackgroundLayer(cs))
        panel.addLayer(ConstantQLayer(cs, audioFile, 2048, 3600, 10800, 12))
        //	panel.addLayer(new FFTLayer(cs,audioFile,2048,512));
        panel.addLayer(PitchContourLayer(cs, audioFile, Color.red, 2048, 1024))
        panel.addLayer(SelectionLayer(cs))
        panel.addLayer(VerticalFrequencyAxisLayer(cs))
        panel.addLayer(TimeAxisLayer(cs))
        panel.viewPort.addViewPortChangedListener(this)
        val pchCS = CoordinateSystem(
            yAxisUnits = AxisUnit.OCCURRENCES,
            yMin = 0F,
            yMax = 1000F,
            isWrapping = true
        )
        pchCS.setMin(Axis.X, 0f)
        pchCS.setMax(Axis.X, 1200f)
        val pchPanel = LinkedPanel(pchCS)
        pchPanel.addLayer(BackgroundLayer(pchCS))
        pchPanel.addLayer(DragMouseListenerLayer(pchCS))
        pchPanel.addLayer(PitchClassHistogramLayer())
        pchPanel.addLayer(ScaleLayer(pchCS, true))
        pchPanel.addLayer(ScaleLayer(pchCS, false))
        pchPanel.viewPort.addViewPortChangedListener(object : ViewPortChangedListener {
            var painting = false
            override fun viewPortChanged(newViewPort: ViewPort?) {
                if (!painting) {
                    painting = true
                    pchPanel.repaint()
                    painting = false
                }
            }
        })
        lastSplitPane!!.add(pchPanel, JSplitPane.BOTTOM)
        panels["Spectral info"] = panel
        lastSplitPane!!.setDividerLocation(0.7)
    }

    companion object {
        private const val serialVersionUID = 7301610309790983406L
        private var instance: LinkedFrame? = null
        private var panels: HashMap<String, LinkedPanel> = HashMap()

        @JvmStatic
        fun main(strings: Array<String>) {
            getInstance()
        }

        fun getInstance(): LinkedFrame? {
            if (instance == null) {
                instance = LinkedFrame()
                instance!!.initialise()
            }
            return instance
        }
    }
}