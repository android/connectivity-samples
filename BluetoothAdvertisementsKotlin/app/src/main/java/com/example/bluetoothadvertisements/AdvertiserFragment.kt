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

import android.bluetooth.le.AdvertiseCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.bluetoothadvertisements.databinding.FragmentAdvertiserBinding
import com.example.bluetoothadvertisements.service.AdvertiserService

private const val TAG = "AdvertiserFragment"

/**
 * Allows user to choose to advertise or stop advertising this device by flipping a switch
 */
class AdvertiserFragment : Fragment() {

    private lateinit var binding: FragmentAdvertiserBinding
    private lateinit var btAdvertisingFailureReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btAdvertisingFailureReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val errorCode = intent?.getIntExtra(BT_ADVERTISING_FAILED_EXTRA_CODE, INVALID_CODE)
                binding.advertiseSwitch.isChecked = false

                var errMsg = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> getString(R.string.already_started)
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> getString(R.string.data_too_large)
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> getString(R.string.not_supported)
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> getString(R.string.inter_err)
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers."
                    ADVERTISING_TIMED_OUT -> "Timed out."
                    else -> "Error unknown."
                }
                errMsg = "Start advertising failed: $errMsg"
                Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdvertiserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.advertiseSwitch.setOnClickListener {
            val view = it as SwitchCompat
            if (view.isChecked) startAdvertising() else stopAdvertising()
            Log.d(TAG, "onViewCreated: switch clicked ")
        }
    }

    override fun onResume() {
        super.onResume()
        binding.advertiseSwitch.isChecked = AdvertiserService.running
        val failureFilter = IntentFilter(ADVERTISING_FAILED)
        requireActivity().registerReceiver(btAdvertisingFailureReceiver, failureFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(btAdvertisingFailureReceiver)
    }

    private fun startAdvertising() = requireContext().startService(createAdvertisingServiceIntent())

    private fun stopAdvertising() {
        requireContext().stopService(createAdvertisingServiceIntent())
        binding.advertiseSwitch.isChecked = false
    }

    private fun createAdvertisingServiceIntent(): Intent =
        Intent(requireContext(), AdvertiserService::class.java)
}


