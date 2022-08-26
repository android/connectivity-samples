package com.google.location.nearby.apps.uwbranging

/**
 * A token class that describes a UWB device.
 *
 * @param id a unique identifier that identifies the UWB device. Unlike UWB address, this identifier
 * is consistent during different UWB sessions.
 */
data class UwbEndpoint(val id: String, val metadata: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UwbEndpoint) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
