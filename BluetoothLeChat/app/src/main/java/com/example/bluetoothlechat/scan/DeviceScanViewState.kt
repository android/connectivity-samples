package com.example.bluetoothlechat.scan

import android.bluetooth.BluetoothDevice

sealed class DeviceScanViewState {
    object ActiveScan: DeviceScanViewState()
    class ScanResults(val scanResults: Map<String, BluetoothDevice>): DeviceScanViewState()
    class Error(val message: String): DeviceScanViewState()
    object AdvertisementNotSupported: DeviceScanViewState()
}