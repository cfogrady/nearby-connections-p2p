package com.github.cfogrady.nearby.connections.p2p.wear.ui

import android.util.Log
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.material3.CompactButton
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Composable
fun DisplayMatchingDevices(deviceName: String, discoveredDevicesFlow: Flow<String>, rescan: ()->Unit, selectDevice: (String)->Unit) {
    KeepScreenOn()
    val devicePairingNames = remember { mutableStateListOf<String>() }
    LaunchedEffect(true) {
        discoveredDevicesFlow.collect {
            devicePairingNames.add(it)
        }
    }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
        Text(text = "Device Id: $deviceName", fontWeight = FontWeight.Bold, fontSize = 2.em, modifier = Modifier.padding(5.dp, 10.dp, 5.dp, 0.dp))
        CompactButton(onClick = {
            rescan.invoke()
            devicePairingNames.clear()
        }) {
            Text("Rescan")
        }
        Text(text="Select Other Device", fontSize = 2.em, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(devicePairingNames) { devicePairingName ->
                CompactButton(onClick = { selectDevice.invoke(devicePairingName) }) {
                    Text(devicePairingName)
                }
            }

        }
    }
}

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
private fun DisplayMatchingDevicesPreview() {
    val testFlow = remember { MutableSharedFlow<String>(replay = 4) }
    var targetValue by remember { mutableStateOf(0) }
    val addId by animateIntAsState(targetValue = targetValue,
        animationSpec = tween(durationMillis = 20000),
        label = "DeviceAdditions")
    LaunchedEffect(true) {
        targetValue = subSequentDevices.size-1
    }
    Log.i("Preview", "AddId: $addId")
    LaunchedEffect(addId) {
        Log.i("Preview", "Emit AddId: $addId")
        testFlow.emit(subSequentDevices[addId])
    }
    DisplayMatchingDevices("ABCD", testFlow, rescan = {
        if(targetValue == subSequentDevices.size-1) {
            CoroutineScope(Dispatchers.Default).launch {
                testFlow.emit(subSequentDevices[subSequentDevices.size-1])
            }
            targetValue = 0
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                testFlow.emit(subSequentDevices[0])
            }
            targetValue = subSequentDevices.size-1
        }
    }, selectDevice = {})
}

private val subSequentDevices = arrayOf("DCBA", "MNOP", "FGSD", "FEFF", "ZFER", "FESF", "DOIQ", "DOIN", "BLEF", "QPKD", "ZXCL", "ANOT", "WEOQ", "QWOP", "DTYU", "ZXCV")
