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
package com.example.bluetoothlechat.scan

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothlechat.R

class DeviceScanAdapter(
    private val onDeviceSelected: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceScanViewHolder>() {

    private var items = listOf<BluetoothDevice>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceScanViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_device, parent, false)
        return DeviceScanViewHolder(view, onDeviceSelected)
    }

    override fun onBindViewHolder(holder: DeviceScanViewHolder, position: Int) {
        items.getOrNull(position)?.let { result ->
            holder.bind(result)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(results: List<BluetoothDevice>) {
        items = results
        notifyDataSetChanged()
    }
}