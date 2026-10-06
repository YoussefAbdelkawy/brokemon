package com.joecode.brokemon.battle

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.joecode.brokemon.domain.battle.BattleCodec
import com.joecode.brokemon.domain.battle.BattleMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface LinkEvent {
    data object Connected : LinkEvent
    data object Disconnected : LinkEvent
    data class Message(val message: BattleMessage) : LinkEvent
    data class Failed(val reason: String) : LinkEvent
}

/**
 * A one-to-one battle link over Google Nearby Connections: Bluetooth and
 * Wi-Fi straight between two phones, no server and no internet. The host
 * advertises its 6-character room code; the guest looks for that code.
 */
class NearbyLink(context: Context) {
    private val client = Nearby.getConnectionsClient(context.applicationContext)
    private val _events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<LinkEvent> = _events.asSharedFlow()

    private var endpointId: String? = null
    private var code: String = ""
    private var name: String = ""
    private var hosting = false

    val connected: Boolean get() = endpointId != null

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            BattleCodec.decode(bytes)?.let { _events.tryEmit(LinkEvent.Message(it)) }
        }

        override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            // Only accept phones that know this room's code.
            val ok = info.endpointName.startsWith("$PREFIX$code:")
            if (ok && endpointId == null) client.acceptConnection(id, payloads) else client.rejectConnection(id)
        }

        override fun onConnectionResult(id: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                endpointId = id
                client.stopDiscovery()
                if (hosting) client.stopAdvertising()
                _events.tryEmit(LinkEvent.Connected)
            }
        }

        override fun onDisconnected(id: String) {
            if (id == endpointId) {
                endpointId = null
                _events.tryEmit(LinkEvent.Disconnected)
            }
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
            if (endpointId == null && info.endpointName.startsWith("$PREFIX$code:")) {
                client.requestConnection(endpointName(), id, lifecycle)
                    .addOnFailureListener { _events.tryEmit(LinkEvent.Failed("Couldn't connect. Try again.")) }
            }
        }

        override fun onEndpointLost(id: String) = Unit
    }

    private fun endpointName() = "$PREFIX$code:${name.take(16)}"

    fun host(code: String, trainerName: String) {
        this.code = code
        name = trainerName
        hosting = true
        client.startAdvertising(endpointName(), SERVICE_ID, lifecycle, AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
            .addOnFailureListener { _events.tryEmit(LinkEvent.Failed("Couldn't open the room: ${it.message ?: "Bluetooth/Wi-Fi off?"}")) }
    }

    fun join(code: String, trainerName: String) {
        this.code = code
        name = trainerName
        hosting = false
        client.startDiscovery(SERVICE_ID, discovery, DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
            .addOnFailureListener { _events.tryEmit(LinkEvent.Failed("Couldn't search: ${it.message ?: "Bluetooth/Wi-Fi off?"}")) }
    }

    /** After a drop: the host opens the room again, the guest searches again. */
    fun reconnect() {
        if (hosting) host(code, name) else join(code, name)
    }

    fun send(message: BattleMessage) {
        val id = endpointId ?: return
        client.sendPayload(id, Payload.fromBytes(BattleCodec.encode(message)))
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        endpointId = null
    }

    companion object {
        /** Same for every build so both app styles can battle each other. */
        const val SERVICE_ID = "com.joecode.brokemon.battle"
        private const val PREFIX = "BRK:"
        private val STRATEGY = Strategy.P2P_POINT_TO_POINT

        /** Runtime permissions Nearby needs on this Android version. */
        val requiredPermissions: Array<String>
            get() = when {
                Build.VERSION.SDK_INT >= 33 -> arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                )
                Build.VERSION.SDK_INT >= 31 -> arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
                else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}
