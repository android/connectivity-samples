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
import android.view.Menu
import android.view.MenuItem
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
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        binding = ActivityMainBinding.inflate(layoutInflater)

        if (savedInstanceState == null) {
            verifyBluetooth()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ENABLE_BT -> {
                    Log.d(TAG, "onActivityResult: REQUEST_ENABLE_BT")
                    verifyBluetooth()
                }
                PERMISSION_REQUEST_LOCATION -> {
                    Log.d(TAG, "onActivityResult: PERMISSION_REQUEST_COARSE_LOCATION")
                    verifyBluetooth()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupFragments() {
        val fragTransaction = supportFragmentManager.beginTransaction()
        fragTransaction.replace(R.id.scanner_fragment_container, ScannerFragment())
        fragTransaction.replace(R.id.advertiser_fragment_container, AdvertiserFragment())
        fragTransaction.commit()
    }

    private fun verifyBluetooth() {
        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        when {
            bluetoothAdapter == null ->
                showErrorText("onCreate: bluetooth not supported")
            bluetoothAdapter.isEnabled && bluetoothAdapter.isMultipleAdvertisementSupported ->
                setupFragments()
            bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported ->
                showErrorText("Bluetooth Advertisements are not supported.")
            !bluetoothAdapter.isEnabled ->
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT
                )
        }
    }

    private fun showErrorText(msg: String) {
        binding.errorText.text = msg
        Log.d(TAG, "showErrorText: $msg")
    }
}
