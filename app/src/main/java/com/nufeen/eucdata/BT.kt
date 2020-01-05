package com.nufeen.eucdata

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult

abstract class BT {
  abstract fun onGotwayFound(device: BluetoothDevice)
  lateinit var btAdapter: BluetoothAdapter

  fun init(mainActivity: MainActivity) {
    btAdapter = BluetoothAdapter.getDefaultAdapter()

    // https://stackoverflow.com/a/36177638/4878481
    // we will get empty search without this manual permission request
    ActivityCompat.requestPermissions(
      mainActivity as Activity,
      arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
      1
    )

    checkBTEnabled(mainActivity)
    startReceiver(mainActivity)
    startDiscovery()
  }

  // https://stackoverflow.com/a/7864148
  private fun startReceiver(mainActivity: Context) {
    val filter = IntentFilter()
    filter.addAction(BluetoothDevice.ACTION_FOUND)
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    with(mainActivity) {
      registerReceiver(receiver, filter)
    }
  }

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        BluetoothAdapter.ACTION_STATE_CHANGED -> {
          if (btAdapter.state == BluetoothAdapter.STATE_ON) {
            // TODO : start discover bug fix
            btAdapter.startDiscovery()
          }
        }

        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//          report("DISCOVERY FINISHED")
        }

        BluetoothDevice.ACTION_FOUND -> {
          val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          if (device?.name != null && device.name.contains("GotWay")) {
            btAdapter.cancelDiscovery()
            onGotwayFound(device)
          }
        }
      }
    }
  }

  private fun checkBTEnabled(mainActivity: Context) {
    if (!btAdapter.isEnabled) {
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(mainActivity as Activity, enableBtIntent, 1, null)
    }
  }

  private fun startDiscovery() {
    if (btAdapter.isDiscovering) {
      btAdapter.cancelDiscovery()
      // TODO : start discover bug fix
    }
    btAdapter.startDiscovery()
  }

  fun onDestroy(mainActivity: Context) {
    Log.d("DEBUG INFO", "DESTROY, UNREGISTER")
    with(mainActivity) {
      unregisterReceiver(receiver)
    }
  }



}

