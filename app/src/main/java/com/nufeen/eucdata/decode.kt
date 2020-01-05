package com.nufeen.eucdata

import android.util.Log
import kotlin.math.*

data class WheelData(
  val speed: Short = 0,
  val temp: Short = 0,
  val battery: Int = 0
)

private val RATIO_GW = 0.875


object decode {
  fun bt(x: String): Int {
    return Integer.parseInt(x, 16)
  }


  fun gotway(d: String): WheelData? {
    val H = d.split(" ")
    val I = H.map { bt(it) }

    // Guards just copied from wheellog code, not sure if all of them needed
    val suspicious =
      H[0] != "55" || d.length < 40 || I[0] != 85 || I[1] != 170 || I[18] != 0

    if (suspicious) {
      return null
    }

    val voltage = I[2] * 256 + (I[3] and 255)
    val battery = (voltage - 5290) / 13

    val int = (H[4] + H[5]).toInt(radix = 16).toShort()
    val speed = round(abs(int * 3.6 * RATIO_GW / 100))

    val temp = round(((I[12] * 256 + I[13]) / 340.0 + 35) * 100) / 100

    return WheelData(
      speed.toShort(), temp.toShort(), battery
    )
  }
}



