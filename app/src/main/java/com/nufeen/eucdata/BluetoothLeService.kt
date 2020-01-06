package com.nufeen.eucdata

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

const val GOTWAY_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
const val GOTWAY_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"

class BluetoothLeService() : Service() {
  val getSupportedGattServices: List<BluetoothGattService>?
    get() {
      if (bluetoothGatt == null) return null
      return bluetoothGatt!!.services
    }

  private val mBinder = LocalBinder()

  // https://github.com/objectsyndicate/Kotlin-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/BluetoothLeService.kt
  inner class LocalBinder : Binder() {
    internal val service: BluetoothLeService
      get() = this@BluetoothLeService
  }

  override fun onBind(p0: Intent?): IBinder? {
    Log.d(TAG, "BIND SERVICE")
    return mBinder
  }

  private var connectionState = STATE_DISCONNECTED
  var bluetoothGatt: BluetoothGatt? = null

  fun init(device: BluetoothDevice) {
    Log.d(TAG, "INIT BLE SERVICE")
    bluetoothGatt = device.connectGatt(this, false, gattCallback)
  }

  fun writeCharacteristic(b: ByteArray) {
    val service: BluetoothGattService =
      bluetoothGatt!!.getService(UUID.fromString(GOTWAY_SERVICE_UUID))

    val characteristic: BluetoothGattCharacteristic =
      service.getCharacteristic(UUID.fromString(GOTWAY_READ_CHARACTER_UUID))

    characteristic.setValue(b);
    bluetoothGatt!!.writeCharacteristic(characteristic);
  }

  fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
    bluetoothGatt?.readCharacteristic(characteristic)

    if (characteristic.uuid.toString() == "0000ffe1-0000-1000-8000-00805f9b34fb") {
      bluetoothGatt?.setCharacteristicNotification(characteristic, true);
      val uuid: UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")
      val descriptor = characteristic.getDescriptor(uuid).apply {
        value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
      }
      bluetoothGatt?.writeDescriptor(descriptor)
    }
  }

  private val gattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(
      gatt: BluetoothGatt,
      status: Int,
      newState: Int
    ) {
      val intentAction: String
      when (newState) {
        BluetoothProfile.STATE_CONNECTED -> {
          intentAction = ACTION_GATT_CONNECTED
          connectionState = STATE_CONNECTED
          broadcastUpdate(intentAction)
          Log.i(TAG, "SERVICE Connected to GATT server.")
          Log.i(
            TAG, "SERVICE Attempting to start service discovery: " +
                bluetoothGatt?.discoverServices()
          )
        }
        BluetoothProfile.STATE_DISCONNECTED -> {
          intentAction = ACTION_GATT_DISCONNECTED
          connectionState = STATE_DISCONNECTED
          broadcastUpdate(intentAction)
        }
      }
    }

    override fun onCharacteristicChanged(
      gatt: BluetoothGatt,
      characteristic: BluetoothGattCharacteristic
    ) {
      broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
      when (status) {
        BluetoothGatt.GATT_SUCCESS -> broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
        else -> Log.w(TAG, "SERVICE onServicesDiscovered received: $status")
      }
    }

    override fun onCharacteristicRead(
      gatt: BluetoothGatt,
      characteristic: BluetoothGattCharacteristic,
      status: Int
    ) {
      when (status) {
        BluetoothGatt.GATT_SUCCESS -> {
          broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
      }
    }
  }

  private fun broadcastUpdate(action: String) {
    val intent = Intent(action)
    sendBroadcast(intent)
  }

  private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
    val intent = Intent(action)
    val data: ByteArray? = characteristic.value
    if (data?.isNotEmpty() == true) {
      val hexString: String = data.joinToString(separator = " ") {
        String.format("%02X", it)
      }
      intent.putExtra(EXTRA_DATA, hexString)
    }
    sendBroadcast(intent)
  }
}