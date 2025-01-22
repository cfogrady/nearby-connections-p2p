package com.github.cfogrady.nearby.connections.p2p

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NearbyP2PConnection(
    context: Context,
    val serviceId: String,
    val pairingName: String,
    var onReceive: (Payload)->Unit = {},
    var onTransferUpdate: (PayloadTransferUpdate)->Unit = {})
    {

    companion object {
        const val TAG = "NearbyConnections"
        const val PAYLOAD_TYPE_BYTES = Payload.Type.BYTES
        const val PAYLOAD_TYPE_STREAM = Payload.Type.STREAM
        const val PAYLOAD_TYPE_FILE = Payload.Type.FILE
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val internalConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ConnectionStatus> = internalConnectionStatus


    private var selectedEndpointName = CompletableDeferred<String>()
    private val remotePairingNameToEndpointId = mutableMapOf<String, String>()
    private val mutableDiscoveredDevices = MutableSharedFlow<String>(replay = 10)
    val discoveredDevices: Flow<String> = mutableDiscoveredDevices

    fun search() {
        internalConnectionStatus.value = ConnectionStatus.SEARCHING
        startAdvertising()
        startDiscovery()
    }

    fun connect(remotePairingName: String) {
        internalConnectionStatus.value = ConnectionStatus.CONNECTING
        selectedEndpointName.complete(remotePairingName)
        if(pairingName > remotePairingName) {
            val remoteEndpointId = remotePairingNameToEndpointId[remotePairingName]!!
            connectionsClient.requestConnection(pairingName, remoteEndpointId, connectionLifecycleCallback)
        }
    }

    fun sendData(data: ByteArray) {
        if(!selectedEndpointName.isCompleted) {
            throw IllegalStateException("Remote device not yet selected")
        }
        connectionsClient.sendPayload(getSelectedEndpointId(), Payload.fromBytes(data))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun close() {
        connectionsClient.stopAllEndpoints()
        mutableDiscoveredDevices.resetReplayCache()
        internalConnectionStatus.value = ConnectionStatus.DISCONNECTED
        remotePairingNameToEndpointId.clear()
        selectedEndpointName = CompletableDeferred<String>()
    }

    private fun startAdvertising() {
        val advertisingOptions =
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient
            .startAdvertising(
                pairingName, serviceId, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener {
                Log.i(TAG, "Started advertising")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to start listening: $it")
            }
    }

    private fun startDiscovery() {
        val discoveryOptions =
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        connectionsClient
            .startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                Log.i(TAG, "Started Discovery")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to start discovery: $it")
            }
    }

    private val connectionLifecycleCallback = object: ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.i(TAG, "onConnectionInitiated, $endpointId, ${connectionInfo.endpointName}")
            if(!selectedEndpointName.isCompleted) {
                CoroutineScope(Dispatchers.IO).launch {
                    selectedEndpointName.await()
                    checkToAccept(endpointId)
                }
            } else {
                checkToAccept(endpointId)
            }
        }

        override fun onConnectionResult(endpointId: String, connectionResolution: ConnectionResolution) {
            if(connectionResolution.status.isSuccess) {
                internalConnectionStatus.value = ConnectionStatus.CONNECTED
            } else if(connectionResolution.status.statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                internalConnectionStatus.value = ConnectionStatus.REJECTED
            } else {
                Log.i(TAG,"Connection Result: ${connectionResolution.status}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            internalConnectionStatus.value = ConnectionStatus.DISCONNECTED
            Log.i(TAG, "onDisconnected, $endpointId")
        }

    }

    private fun checkToAccept(connectingEndpointId: String) {
        if(getSelectedEndpointId() == connectingEndpointId) {
            connectionsClient.acceptConnection(connectingEndpointId, payloadCallback)
        } else {
            connectionsClient.rejectConnection(connectingEndpointId)
        }
    }

    private val payloadCallback = object: PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            onReceive.invoke(payload)
        }

        override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
            onTransferUpdate.invoke(payloadTransferUpdate)
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getSelectedEndpointId(): String {
        return remotePairingNameToEndpointId[selectedEndpointName.getCompleted()]!!
    }

    private val endpointDiscoveryCallback = object: EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, endpointInfo: DiscoveredEndpointInfo) {
            Log.i(TAG, "Endpoint found: $endpointId, ${endpointInfo.endpointName}")
            if(!remotePairingNameToEndpointId.containsKey(endpointInfo.endpointName)) {
                CoroutineScope(Dispatchers.IO).launch {
                    mutableDiscoveredDevices.emit(endpointInfo.endpointName)
                }
            }
            remotePairingNameToEndpointId[endpointInfo.endpointName] = endpointId
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i(TAG, "Endpoint Lost: $endpointId")
        }

    }
}