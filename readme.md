# EUC Data

Wear OS app showing data from EUC.

Current status: supports Gotway only.


## WheelLog 

https://github.com/palachzzz/WheelLogAndroid/blob/master/app/src/main/java/com/cooper/wheellog/WheelData.java

## Gotway response structure

https://forum.electricunicycle.org/topic/2850-converting-the-msuper-board-to-bt-le/

```
//    Packet String Type = 00
//    i.e.
//    55 aa 17 1c 00 00 00 00 00 00 00 e0 f2 78 00 01 ff f8 00 18 5a 5a 5a 5a
//
//    55 aa = Header
//    17 1c = Voltage
//    00 00 = Speed (signed short)
//    00 00 00 00 = Trip Odo
//    00 e0 = Current
//    f2 78 = Temperature
//    00 01 ff f8 = UNKNOWN
//    00 = Packet String Type
//    18 = Byte Count excluding the "55 aa"
//    5a 5a 5a 5a = Footer
//
//    Packet String Type = 04
//    i.e.
//    55 aa 00 00 ec f2 00 00 00 00 00 00 00 00 00 00 00 00 04 18 5a 5a 5a 5a
//
//    55 aa = Header
//    00 00 ec f2 = Odometer
//    00 00 00 00 00 00 00 00 00 00 00 00
//    04 = Packet String Type
//    5a 5a 5a 5a = Footer
```

https://forum.electricunicycle.org/topic/870-gotwaykingsong-protocol-reverse-engineering/


## Bluetooth low energy overview

https://developer.android.com/guide/topics/connectivity/bluetooth-le


## Note

https://stackoverflow.com/a/23660414

Here is the general pattern for how things need to work with BLE on Android:

- You try to connect
- You get a callback indicating it is connected
- You discover services
- You are told services are discovered
- You get the characteristics
- For each characteristic you get the descriptors
- For the descriptor you set it to enable notification/indication with BluetoothGattDescriptor.setValue()
- You write the descriptor with BluetoothGatt.writeDescriptor()
- You enable notifications for the characteristic locally with BluetoothGatt.setCharacteristicNotification(). Without this you won't get called back.
- You get notification that the descriptor was written
- Now you can write data to the characteristic. All of the characteristic and descriptor configuration has do be done before anything is written to any characteristic.


## GOTWAY NIKOLA SERVICES: 

00001800-0000-1000-8000-00805f9b34fb
00001801-0000-1000-8000-00805f9b34fb
0000180a-0000-1000-8000-00805f9b34fb
0000ffe0-0000-1000-8000-00805f9b34fb


### 00001800

Characteristic uuids:

00002a00-0000-1000-8000-00805f9b34fb
00002a01-0000-1000-8000-00805f9b34fb
00002a02-0000-1000-8000-00805f9b34fb
00002a03-0000-1000-8000-00805f9b34fb
00002a04-0000-1000-8000-00805f9b34fb


## 00001801

Characteristic: 00002a05-0000-1000-8000-00805f9b34fb
Descriptor: 00002902-0000-1000-8000-00805f9b34fb


### 0000ffe0

Characteristic: 0000ffe1-0000-1000-8000-00805f9b34fb
Descriptor: 00002901-0000-1000-8000-00805f9b34fb
Descriptor: 00002902-0000-1000-8000-00805f9b34fb


### WEAR OS PART

Really USEFUL one:

https://medium.com/mindorks/a-beginner-guide-to-android-watch-app-wear-2-0-71b27a802d11