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
package be.tarsos.dsp.example

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.border.TitledBorder

class InputPanel : JPanel(BorderLayout()) {
    var mixer: Mixer? = null
    private val setInput =
        ActionListener { arg0: ActionEvent ->
            for (info in Shared.getMixerInfo(false, true)) {
                if (arg0.actionCommand == info.toString()) {
                    val newValue = AudioSystem.getMixer(info)
                    this@InputPanel.firePropertyChange("mixer", mixer, newValue)
                    mixer = newValue
                    break
                }
            }
        }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = 1L
    }

    init {
        border = TitledBorder("1. Choose a microphone input")
        val buttonPanel = JPanel(GridLayout(0, 1))
        val group = ButtonGroup()
        for (info in Shared.getMixerInfo(false, true)) {
            val button = JRadioButton()
            button.text = Shared.toLocalString(info)
            buttonPanel.add(button)
            group.add(button)
            button.actionCommand = info.toString()
            button.addActionListener(setInput)
        }
        this.add(
            JScrollPane(
                buttonPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            ), BorderLayout.CENTER
        )
        this.maximumSize = Dimension(300, 150)
        this.preferredSize = Dimension(300, 150)
    }
}