package com.nufeen.eucdata

import android.bluetooth.BluetoothDevice
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.support.wearable.activity.WearableActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat.SHORT
import java.text.SimpleDateFormat
import java.util.*

const val TAG = "UINFO"
const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val STATE_CONNECTED = 2
const val STATE_DISCONNECTED = 0
const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
const val ACTION_GATT_SERVICES_DISCOVERED =
  "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

class MainActivity : WearableActivity() {
  private var BLEService: BluetoothLeService? = null
  lateinit var mdevice: BluetoothDevice
  lateinit var deviceName: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setAmbientEnabled() // Enables Always-on
    bt.init(this)
    bindEvents()
  }

  private fun bindEvents() {
    lightswitch.setOnCheckedChangeListener { _, isChecked ->
      updateLightMode(if (isChecked) 1 else 0)
    }
  }

  private val bt = object : BT() {
    override fun onGotwayFound(device: BluetoothDevice) {
      deviceName = device.name
      status.text = "Gotway found: $device"
      status.setTextColor(Color.YELLOW)
      start(device)
    }
  }

  override fun onDestroy() {
    Log.d(TAG, "DESTROY")
    super.onDestroy()
    bt.onDestroy(this)
    unregisterReceiver(gattUpdateReceiver)
  }

  override fun onPause() {
    Log.d(TAG, "PAUSE")
    super.onPause()
    // unregisterReceiver(gattUpdateReceiver)
  }


  override fun onResume() {
    super.onResume()
    Log.d(TAG, "resume")
    // start(mdevice)
  }

  fun start(device: BluetoothDevice) {
    Log.d(TAG, "START")
    Log.d(TAG, device.toString())

    mdevice = device
    startReceiver()
    val intent = Intent(this@MainActivity, BluetoothLeService::class.java)

    Log.d(TAG, "SERVICE BIND ATTEMPT")
    startService(intent)
    bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
  }

  // Code to manage Service lifecycle.
  // https://github.com/objectsyndicate/Kotlin-BluetoothLeGatt
  val mServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
      BLEService = (service as BluetoothLeService.LocalBinder).service
      Log.d(TAG, "ON SERVICE CONNECT")
      BLEService!!.init(mdevice)
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
      // TODO: Stable reconnection
      status.text = "DISCONNECTED"
      status.setTextColor(Color.RED)

      if (mdevice != null) {
        startReceiver()
      }
    }
  }

  fun startReceiver() {
    Log.d(TAG, "START RECIEVER")
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
          status.text = mdevice.name
          status.setTextColor(Color.GRAY)
        }

        ACTION_GATT_DISCONNECTED -> {
          // TODO: Stable reconnection
          Log.d(TAG, "RECIEVER  ACTION_GATT_DISCONNECTED")

          status.text = "DISCONNECTED, RECONNECT"
          status.setTextColor(Color.RED)

          if (mdevice != null) {
            // start(mdevice)
            startReceiver()
          } else {
            status.text = "CONNECTION LOST"
            status.setTextColor(Color.RED)
          }
        }

        ACTION_GATT_SERVICES_DISCOVERED -> {
          Log.d(TAG, "RECIEVER  ACTION_GATT_DISCOVERED")
          val SS = BLEService?.getSupportedGattServices
          for (service in SS!!) {
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
