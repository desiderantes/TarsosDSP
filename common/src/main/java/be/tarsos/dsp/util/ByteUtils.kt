package be.tarsos.dsp.util

inline infix fun Byte.shr(other: Byte): Byte {
    return this.toInt().shr(other.toInt()).toByte()
}

inline infix fun Byte.ushr(other: Byte): Byte {
    return this.toInt().ushr(other.toInt()).toByte()
}

inline infix fun Byte.shl(other: Byte): Byte {
    return this.toInt().shl(other.toInt()).toByte()
}