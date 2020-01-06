package com.nufeen.eucdata

import android.util.Log
import kotlin.math.*

data class WheelData(
  val speed: Int = 0,
  val temp: Int = 0,
  val battery: Int = 0
)

const val RATIO_GOTWAY = 0.875

object decode {
  fun gotway(d: String): WheelData? {
    val H = d.split(" ") // hex values
    val I = H.map { Integer.parseInt(it, 16) } // int values

    // Guards copied from wheellog code, not sure if all of them needed,
    // needs investigation
    val suspicious =
      H[0] != "55" || d.length < 40 || I[0] != 85 || I[1] != 170 || I[18] != 0

    if (suspicious) {
      return null
    }

    val voltage = I[2] * 256 + (I[3] and 255)
    val battery = (voltage - 5290) / 13

    val rawSpeed = (H[4] + H[5]).toInt(radix = 16).toShort() // signed value
    val speed  = round(abs(rawSpeed * 3.6 * RATIO_GOTWAY / 100)).toInt()

    val rawTemp = (H[12] + H[13]).toInt(radix = 16).toShort() // signed value
    val temp = round(rawTemp / 340.0 + 35).toInt()

    return WheelData(
      speed, temp, battery
    )
  }
}



