/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.github.cfogrady.nearby.p2p.wear.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.cfogrady.nearby.connections.p2p.ConnectionStatus
import com.github.cfogrady.nearby.connections.p2p.NearbyP2PConnection
import com.github.cfogrady.nearby.connections.p2p.wear.ui.DisplayMatchingDevices
import com.github.cfogrady.nearby.p2p.wear.presentation.theme.NearbyTheme
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    companion object {
        const val TEST_SERVICE_ID = "01948e9b-7815-70a5-b83e-d02b85fbd86c"
    }

    lateinit var nearbyConnections: NearbyP2PConnection
    var needsPermissions: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        nearbyConnections = NearbyP2PConnection(this, TEST_SERVICE_ID, onReceive = {
            if(it.type == NearbyP2PConnection.PAYLOAD_TYPE_BYTES) {
                Toast.makeText(this, "Received Data: ${it.asBytes()?.toString(StandardCharsets.UTF_8)}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        val permissionRequestLauncher = buildPermissionRequestLauncher { grantedPermissions->
            Log.i("MainActivity", "Iterating permissions requested to check grant status")
            for(grantedPermission in grantedPermissions) {
                if(!grantedPermission.value) {
                    Toast.makeText(this, "Nearby Communication requires all requested permissions to be enabled to run", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            needsPermissions = false
        }
        val missingPermissions = NearbyP2PConnection.getMissingPermissions(this)
        if(missingPermissions.isNotEmpty()) {
            needsPermissions = true
            Log.i("MainActivity", "Requesting permissions: $missingPermissions")
            permissionRequestLauncher.launch(missingPermissions.toTypedArray())
        }

        setContent {
            WearApp("Android")
        }
    }

    @Composable
    fun WearApp(greetingName: String) {
        NearbyTheme(this) {
            val connectionState by nearbyConnections.connectionStatus.collectAsState()
            var selectedDevicePairingName by remember { mutableStateOf("") }
            LaunchedEffect(connectionState) {
                if(connectionState == ConnectionStatus.CONNECTED) {
                    if(nearbyConnections.pairingName > selectedDevicePairingName) {
                        nearbyConnections.sendData("Hello World".toByteArray(StandardCharsets.UTF_8))
                    }
                } else if (connectionState == ConnectionStatus.REJECTED) {
                    Toast.makeText(this@MainActivity, "Connection rejected by other party", Toast.LENGTH_SHORT).show()
                    nearbyConnections.close()
                    nearbyConnections.search()
                }
            }
            DisplayMatchingDevices(nearbyConnections.pairingName, nearbyConnections.discoveredDevices, rescan = {
                nearbyConnections.close()
                nearbyConnections.search()
            }, selectDevice = {
                selectedDevicePairingName = it
                nearbyConnections.connect(it)
            })
        }
    }

    private fun buildPermissionRequestLauncher(resultBehavior: (Map<String, Boolean>)->Unit): ActivityResultLauncher<Array<String>> {
        val multiplePermissionsContract = ActivityResultContracts.RequestMultiplePermissions()
        val launcher = registerForActivityResult(multiplePermissionsContract, resultBehavior)
        return launcher
    }

    override fun onResume() {
        super.onResume()
        Log.i("MainActivity", "Resuming")
        if(!needsPermissions) {
            Log.i("MainActivity", "Searching")
            nearbyConnections.search()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i("MainActivity", "Pausing")
        nearbyConnections.close()
    }
}
