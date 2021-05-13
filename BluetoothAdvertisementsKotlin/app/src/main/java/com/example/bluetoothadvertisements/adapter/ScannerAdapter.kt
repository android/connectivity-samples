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

package com.example.bluetoothadvertisements.adapter

import android.bluetooth.le.ScanResult
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothadvertisements.databinding.VhScanResultBinding

private const val TAG = "ScannerAdapter"

/**
 * Adapter for displaying remote Bluetooth devices that are being advertised
 */
class ScannerAdapter : RecyclerView.Adapter<ScannerAdapter.ScanResultVh>() {

    private var itemsList: MutableList<ScanResult> = arrayListOf()

    fun setItems(mutableList: MutableList<ScanResult>) {
        if (mutableList != itemsList) {
            itemsList = mutableList
            notifyDataSetChanged()
        }
    }

    fun addSingleItem(item: ScanResult) {
        /**
         * In this particular case the data coming in may be duplicate. So check that only unique
         * elements are admitted: the device Id + device name should create a unique pair.
         * removeIf requires API level 24, so using removeAll here. But feel free to use any of
         * a number of options. Remove the previous element so to keep the latest timestamp
         */
        itemsList.removeAll {
            it.device.name == item.device.name && it.device.address == item.device.address
        }
        itemsList.add(item)
        notifyDataSetChanged()
    }

    override fun getItemCount() = itemsList.size

    private fun getItem(position: Int): ScanResult? = if (itemsList.isEmpty()) null else itemsList[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultVh {
        val binding =
            VhScanResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanResultVh(binding)
    }

    override fun onBindViewHolder(holder: ScanResultVh, position: Int) {
        Log.d(TAG, "onBindViewHolder: called for position $position")
        holder.bind(getItem(position))
    }

    inner class ScanResultVh(private val binding: VhScanResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScanResult?) {
            item?.let {
                binding.deviceName.text = it.device.name
                binding.deviceAddress.text = it.device.address
                binding.lastSeen.text = it.timestampNanos.toString()
            }
        }
    }
}
