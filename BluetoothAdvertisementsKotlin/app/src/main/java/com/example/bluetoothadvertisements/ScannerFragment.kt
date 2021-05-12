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

import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothadvertisements.adapter.ScannerAdapter
import com.example.bluetoothadvertisements.databinding.FragmentScannerBinding

private const val TAG = "ScannerFragment"
private const val SCAN_PERIOD_IN_MILLIS: Long = 90_000

/**
 * Displays remote nearby bluetooth devices
 */
class ScannerFragment : Fragment() {

    private lateinit var binding: FragmentScannerBinding
    private lateinit var scannerAdapter: ScannerAdapter
    private var scanCallback: ScanCallback? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        handler = Handler(Looper.myLooper()!!)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()
        setupRecyclerViewAdapter()

        /**
         * Before Android N, Bluetooth didn't need location permission in order to start scanning.
         * But between Android N and R location permission is required for Bluetooth scanning.
         * The code below requests location access if it's required and has not yet been granted.
         */
        val isLocationPermissionRequired =
                Build.VERSION.SDK_INT in Build.VERSION_CODES.N..Build.VERSION_CODES.R
        val isLocationAccessNotGranted =
                (checkSelfPermission(requireContext(), LOCATION_FINE_PERM)
                        != PackageManager.PERMISSION_GRANTED)

        if (isLocationPermissionRequired && isLocationAccessNotGranted) {
            requestLocationPermission()
        } else {
            startScanning()
        }
    }

    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(LOCATION_FINE_PERM)) {
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            with(alertDialogBuilder) {
                setTitle(getString(R.string.loc_req_title))
                setMessage(getString(R.string.loc_req_msg))
                setPositiveButton(getString(R.string.okay)) { _, _ -> makeLocationRequest() }
            }
            alertDialogBuilder.create().show()
        } else {
            makeLocationRequest()
        }
    }

    private fun makeLocationRequest() = requestPermissions(
            arrayOf(LOCATION_FINE_PERM),
            PERMISSION_REQUEST_LOCATION
    )

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScanning()
                }
            }
            else -> Toast.makeText(
                    requireContext(),
                    getString(R.string.loc_req_denied_msg),
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerViewAdapter() {
        scannerAdapter = ScannerAdapter()
        with(binding.scanRecyclerview) {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = scannerAdapter
        }
    }

    private fun initialize() {
        if (bluetoothLeScanner == null) {
            val manager =
                    requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = manager.adapter
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        }
    }

    private fun startScanning() {
        Log.d(TAG, "startScanning")

        if (scanCallback != null) {
            Log.d(TAG, "startScanning: already scanning")
            Toast.makeText(requireContext(), getString(R.string.bt_scanning), Toast.LENGTH_LONG).show()
            return
        }
        handler?.postDelayed({ stopScanning() }, SCAN_PERIOD_IN_MILLIS)
        scanCallback = SampleScanCallback()
        bluetoothLeScanner?.startScan(buildScanFilters(), buildScanSettings(), scanCallback)
    }

    private fun stopScanning() {
        Log.d(TAG, "stopScanning")
        bluetoothLeScanner?.stopScan(scanCallback)
        scanCallback = null
        // update 'last seen' times even though there are no new results
        scannerAdapter.notifyDataSetChanged()
    }

    private fun buildScanFilters(): List<ScanFilter> {
        val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ScanFilterService_UUID)
                .build()
        Log.d(TAG, "buildScanFilters")
        return listOf(scanFilter)
    }

    private fun buildScanSettings() = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.scanner_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                startScanning()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class SampleScanCallback : ScanCallback() {

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults size: ${results?.size}")
            results?.let { scannerAdapter.setItems(it) }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onScanResult: single")
            scannerAdapter.addSingleItem(result)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: errorCode $errorCode")
            Toast.makeText(
                    requireContext(),
                    "Scan failed with error code $errorCode",
                    Toast.LENGTH_LONG
            ).show()
        }
    }

}
