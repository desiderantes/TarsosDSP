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

import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import java.awt.GridLayout
import java.awt.event.ActionListener
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.border.TitledBorder

class PitchDetectionPanel(algoChangedListener: ActionListener?) :
    JPanel(GridLayout(0, 1)) {
    private val algo: PitchEstimationAlgorithm

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -5107785666165487335L
    }

    init {
        border = TitledBorder("2. Choose a pitch detection algorithm")
        val group = ButtonGroup()
        algo = PitchEstimationAlgorithm.YIN
        for (value in PitchEstimationAlgorithm.values()) {
            val button = JRadioButton()
            button.text = value.toString()
            add(button)
            group.add(button)
            button.isSelected = value === algo
            button.actionCommand = value.name
            button.addActionListener(algoChangedListener)
        }
    }
}