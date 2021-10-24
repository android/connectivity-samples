/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetoothadvertisements

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothadvertisements.databinding.ActivityMainBinding

private const val TAG = "MainActivity"

/**
 * Demos how to advertise a bluetooth device and also how to scan for remote nearby bluetooth
 * devices.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            verifyBluetoothCapabilities()
        }
    }

    private fun verifyBluetoothCapabilities() {
        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        when {
            bluetoothAdapter == null ->
                // Bluetooth is not supported on this hardware platform
                showErrorText("onCreate: bluetooth not supported")
            !bluetoothAdapter.isEnabled -> // Bluetooth is OFF, user should turn it ON
                // Prompt the use to allow the app to turn on Bluetooth
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT
                )
            bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported ->
                showErrorText("Bluetooth Advertisements are not supported.")
            bluetoothAdapter.isEnabled && bluetoothAdapter.isMultipleAdvertisementSupported ->
                setupFragments()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "onActivityResult: REQUEST_ENABLE_BT")
            verifyBluetoothCapabilities()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Setup two Fragments in the Activity: one shows the list of nearby devices; one shows the
     * switch for advertising to nearby devices.
     */
    private fun setupFragments() {
        val fragTransaction = supportFragmentManager.beginTransaction()
        fragTransaction.replace(R.id.scanner_fragment_container, ScannerFragment())
        fragTransaction.replace(R.id.advertiser_fragment_container, AdvertiserFragment())
        fragTransaction.commit()
    }

    private fun showErrorText(msg: String) {
        binding.errorText.text = msg
        Log.d(TAG, "showErrorText: $msg")
    }
}
