package com.nufeen.eucdata

import android.bluetooth.BluetoothDevice
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.WindowManager
import androidx.wear.ambient.AmbientModeSupport
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat.SHORT
import java.text.SimpleDateFormat
import java.util.*

const val ACTION_GATT_CONNECTED = "com.nufeen.eucdata.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.nufeen.eucdataACTION_GATT_DISCONNECTED"
const val ACTION_DATA_AVAILABLE = "com.nufeen.eucdata.ACTION_DATA_AVAILABLE"
const val ACTION_GATT_SERVICES_DISCOVERED =  "com.nufeen.eucdata.ACTION_GATT_SERVICES_DISCOVERED"
const val EXTRA_DATA = "com.nufeen.eucdata.EXTRA_DATA"
const val STATE_CONNECTED = 2
const val STATE_DISCONNECTED = 0
const val TAG = "DEBUG"

class MainActivity : WearableActivity() {
  lateinit var device: BluetoothDevice
  private var BLEService: BluetoothLeService? = null
  private lateinit var vibratorService: Vibrator

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Enables Always-on
    // should be managed by https://developer.android.com/training/wearables/apps/always-on
    // but this works: https://developer.android.com/training/scheduling/wakelock.html#screen
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // start searching for wheels around
    bt.init(this)

    // for reporting high speeds
    vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    bindEvents()
  }

  private fun bindEvents() {
    lightswitch.setOnCheckedChangeListener { _, isChecked ->
      updateLightMode(if (isChecked) 1 else 0)
    }
  }

  private val bt = object : BT() {
    override fun onGotwayFound(device: BluetoothDevice) {
      status.text = "Wheel found: ${device.name}"
      status.setTextColor(Color.YELLOW)
      start(device)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    bt.onDestroy(this)
    unregisterReceiver(gattUpdateReceiver)
  }

  override fun onPause() {
    Log.d("DEBUG", "PAUSE")
    super.onPause()
    // unregisterReceiver(gattUpdateReceiver)
  }

  override fun onResume() {
    super.onResume()
    Log.d("DEBUG", "resume")
    // bt.init(this)
  }

  fun start(device: BluetoothDevice) {
    this.device = device
    startReceiver()
    val intent = Intent(this@MainActivity, BluetoothLeService::class.java)
    startService(intent)
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  // Code to manage Service lifecycle.
  // https://github.com/objectsyndicate/Kotlin-BluetoothLeGatt
  val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
      BLEService = (service as BluetoothLeService.LocalBinder).service
      Log.d("DEBUG", "ON SERVICE CONNECT CB")
      BLEService!!.init(this@MainActivity.device)
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
      status.text = "ON SERVICE DISCONNECT CB"
      status.setTextColor(Color.RED)
      if (this@MainActivity.device != null) {
        startReceiver()
      }
    }
  }

  fun startReceiver() {
    val filter = IntentFilter()
    filter.addAction(ACTION_GATT_CONNECTED)
    filter.addAction(ACTION_GATT_DISCONNECTED)
    filter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
    filter.addAction(ACTION_DATA_AVAILABLE)
    registerReceiver(gattUpdateReceiver, filter)
  }

  private val gattUpdateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACTION_GATT_CONNECTED -> {
          status.text = this@MainActivity.device.name
          status.setTextColor(Color.GRAY)
        }

        ACTION_GATT_DISCONNECTED -> {
          status.text = "DISCONNECTED, RECONNECT"
          status.setTextColor(Color.RED)

          if (this@MainActivity.device != null) {
            bt.init(this@MainActivity)
          } else {
            status.text = "CONNECTION LOST"
            status.setTextColor(Color.RED)
          }
        }

        ACTION_GATT_SERVICES_DISCOVERED -> {
          val services = BLEService?.getSupportedGattServices
          for (service in services!!) {
            for (characteristic in service.characteristics) {
              BLEService?.readCharacteristic(characteristic)
            }
          }
        }

        ACTION_DATA_AVAILABLE -> {
          val x = intent.getStringExtra(EXTRA_DATA)
          displayData(x)
        }
      }
    }
  }

  private var t0 = Calendar.getInstance().timeInMillis
  fun displayData(hex: String) {
    val data = decode.gotway(hex)
    if (data != null) {
      val (v, t, b) = data

      battery.text = "$b%"
      speed.text = "$v km/h"
      temperature.text = if (t < 100) "$t°C" else "-"

      speed.setBackgroundColor(if (v > 45) Color.RED else Color.BLACK)
      temperature.setTextColor(if (t < 50) Color.WHITE else Color.RED)
      battery.setTextColor(if (b > 30) Color.WHITE else Color.RED)

      val t1 = Calendar.getInstance().timeInMillis
      if (v > 45 && t1 - t0 > 300) {
        t0 = t1
        val s =
          VibrationEffect.createWaveform(longArrayOf(0, 150), VibrationEffect.DEFAULT_AMPLITUDE)
        vibratorService.vibrate(s)
      }
    }

    val t = Calendar.getInstance().time
    val formatter = SimpleDateFormat.getTimeInstance(SHORT)
    date.text = formatter.format(t)
  }


  fun updateLightMode(lightMode: Int) {
    when (lightMode) {
      0 -> {
        BLEService!!.writeCharacteristic("E".toByteArray())
        BLEService!!.writeCharacteristic("b".toByteArray())
      }
      1 -> {
        BLEService!!.writeCharacteristic("Q".toByteArray())
        BLEService!!.writeCharacteristic("b".toByteArray())
      }
      else -> {
        BLEService!!.writeCharacteristic("T".toByteArray())
        BLEService!!.writeCharacteristic("b".toByteArray())
      }
    }
  }
}
