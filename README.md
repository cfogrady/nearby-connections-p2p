# About
This is meant to act as a helper library for connecting nearby devices in a peer-to-peer fashion.
It builds upon Google's Nearby Connections API, but is built specifically to connect to a peer.

## Modules
1) connections - This is the main library.
2) movile - Sample application for phone/tablet devices
3) ui - Composable to display matching devices and allow selecting one for connecting
4) wear - Sample application for WearOS devices
5) wear-ui Composable to display matching devices and allow selecting one for connecting on WearOS devices.

## Usage
Tha main class is the NearbyP2PConnection class which has a constructor matching:
```
NearbyP2PConnection(
    context: Context, // Android context
    val serviceId: String, // serviceId unique to the application. Best practice is applicationId, but may want a different identifier in cross app communication
    val pairingName: String = generateRandomPairingName(), // pairingName is the name sent to other devices for pairing. By default a random 4-character string is generated 
    var onReceive: (Payload)->Unit = {}, // onReceive is called when receiving a payload after connection has been established.
    var onTransferUpdate: (PayloadTransferUpdate)->Unit = {}) // onTransferUpdate is called to update the status of payload transfers.
```

The main members of this class are:
```
companion object {
  fun generateRandomPairingName(): String // generates a random 4-character string.
  fun getMissingPermissions(activity: Activity): List<String> // gets permissions required, but not yet granted.
}

val connectionStatus: StateFlow<ConnectionStatus> // current connection state. Use this to listen for when the connection is established with ConnectionStatus.CONNECTED
val discoveredDevices: Flow<String> // flow of discovered devices using the same serviceId, the string is the remote device's pairing name.
fun search() // begin advertising and searching for devices on the same service
fun connect(remotePairingName: String) // connects to the remote device with the provided pairing name. Both devices must call connect on each other before a connection is established.
fun sendData(data: ByteArray) // send data once a connection is established
fun close() // close any connections and/or discontinue searching.

```

There is also a helper composable which allows the user to select the device against which to connect:
```
@Composable
fun DisplayMatchingDevices(
    deviceName: String, // local device name (so user can share with peer)
    discoveredDevicesFlow: Flow<String>, // the discoveredDevicesFlow directly from the class to populate discovered devices
    rescan: ()->Unit, // called when the user wishes to clear the lsit and initiate a rescan
    selectDevice: (String)->Unit) // called when the user selects a device
```

### Add As Dependencies
The connections, ui, and wear-ui modules all have a tast to publishToMavenLocal. Execute the task for the ones to be used in the dependent project.

Add mavenLocal to the repositories of the dependent project if not already present.
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal() // add mavenLocal
        ...
    }
}
```

Add the desired dependencies to the dependent project's dependencies
```
dependencies {
    ...
    implementation("com.github.cfogrady.nearby.connections.p2p:connections:0.1.0")
    implementation("com.github.cfogrady.nearby.connections.p2p:ui:0.1.0")
    // implementation("com.github.cfogrady.nearby.connections.p2p:wear-ui:0.1.0") // only for WearOS devices using the helper composable
    ...
}
```
