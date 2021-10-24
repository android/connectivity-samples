/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.example.bluetoothlechat.bluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bluetoothlechat.R
import com.example.bluetoothlechat.databinding.FragmentLocationRequiredBinding

private const val TAG = "LocationRequiredFrag"
private const val LOCATION_REQUEST_CODE = 0

// Fragment responsible for checking if the app has the ACCESS_FINE_LOCATION permission.
// This permission is required when using the BLE APIs so the user must grant permission
// to the app before viewing the BluetoothChatFragment or DeviceListFragment
class LocationRequiredFragment : Fragment() {

    private var _binding: FragmentLocationRequiredBinding? = null
    private val binding: FragmentLocationRequiredBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLocationRequiredBinding.inflate(inflater, container, false)

        // hide the error messages while checking the permissions
        binding.locationErrorMessage.visibility = View.GONE
        binding.grantPermissionButton.visibility = View.GONE
        // setup click listener on grant permission button
        binding.grantPermissionButton.setOnClickListener {
            checkLocationPermission()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // check location permission when the Fragment becomes visible on screen
        checkLocationPermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: ")
        when(requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Navigate to the chat fragment
                    findNavController().navigate(R.id.action_start_chat)
                } else {
                    showError()
                }
            }
        }
    }

    private fun showError() {
        binding.locationErrorMessage.visibility = View.VISIBLE
        binding.grantPermissionButton.visibility = View.VISIBLE
    }

    private fun checkLocationPermission() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            // Navigate to the chat fragment
            findNavController().navigate(R.id.action_start_chat)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }
    }
}