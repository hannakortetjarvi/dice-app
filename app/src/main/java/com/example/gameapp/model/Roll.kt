package com.example.gameapp.model

data class Roll(
    var time: String? = null,
    var numbers: ArrayList<Int> = ArrayList(),
    var style: String? = null
) {
    companion object {
        const val FIELD_TIME = "time"
        const val FIELD_NUMBERS = "numbers"
        const val FIELD_STYLE = "style"
    }
}