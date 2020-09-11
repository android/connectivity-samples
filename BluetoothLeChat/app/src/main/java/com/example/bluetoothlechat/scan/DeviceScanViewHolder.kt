package com.example.bluetoothlechat.scan

import android.bluetooth.BluetoothDevice
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothlechat.R

class DeviceScanViewHolder(
    view: View,
    val onDeviceSelected: (BluetoothDevice) -> Unit
) : RecyclerView.ViewHolder(view), View.OnClickListener {

    private val name = itemView.findViewById<TextView>(R.id.device_name)
    private val address = itemView.findViewById<TextView>(R.id.device_address)
    private var bluetoothDevice: BluetoothDevice? = null

    init {
        itemView.setOnClickListener(this)
    }

    fun bind(device: BluetoothDevice) {
        bluetoothDevice = device
        name.text = device.name
        address.text = device.address
    }

    override fun onClick(view: View) {
        bluetoothDevice?.let { device ->
            onDeviceSelected(device)
        }
    }
}