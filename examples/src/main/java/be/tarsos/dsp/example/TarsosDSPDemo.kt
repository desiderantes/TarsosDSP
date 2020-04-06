package be.tarsos.dsp.example

interface TarsosDSPDemo {
    val name: String
    val description: String
    fun start(vararg args: String)
}