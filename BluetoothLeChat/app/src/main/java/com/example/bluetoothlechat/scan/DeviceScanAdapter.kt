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