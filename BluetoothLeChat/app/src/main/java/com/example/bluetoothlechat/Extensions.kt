package com.example.bluetoothlechat

import android.view.View

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

val <T> T.exhaustive: T
    get() = this