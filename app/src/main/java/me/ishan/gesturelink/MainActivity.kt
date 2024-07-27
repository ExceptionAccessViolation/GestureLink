package me.ishan.gesturelink

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.ishan.gesturelink.ui.theme.GestureLinkTheme
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        if (!hasBluetoothPermissions(this)) {
//            requestBluetoothPermissions(this)
//        }

        setContent {
            GestureLinkTheme {
                App()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    GestureLinkTheme {
        App()
    }
}

@Composable
fun App() {

    var text by remember {
        mutableStateOf(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Duis nisl odio, malesuada eget ligula quis, sagittis sollicitudin " +
                    "urna. Donec auctor elit ex, vel rhoncus metus finibus sit amet. " +
                    "Vestibulum faucibus eros eu felis scelerisque, id mattis augue " +
                    "vestibulum. Integer iaculis sem nisi. Nulla cursus pulvinar augue. " +
                    "Integer porta sapien vel leo consectetur, viverra cursus metus " +
                    "convallis. Mauris pretium sollicitudin eleifend. Sed condimentum " +
                    "ex ut velit malesuada, non facilisis velit porta. Suspendisse potenti.",
//                " Nunc tellus metus, fermentum vel cursus vel, porttitor in odio. " +
//                "Ut vel ultricies sem. Aliquam semper orci turpis, eu mollis elit " +
//                "fermentum et. Vestibulum ac venenatis lorem. Mauris nisl neque, " +
//                "luctus ut vulputate vitae, rhoncus sed nisi. Proin porttitor " +
//                "placerat rhoncus. Aliquam volutpat velit at neque auctor tempus. " +
//                "Ut malesuada nunc arcu, ut euismod justo auctor cursus. In sed " +
//                "semper dui. Cras eu rhoncus risus. Etiam rhoncus ipsum elit, " +
//                "sollicitudin semper odio consectetur ut. Cras accumsan feugiat enim, " +
//                "eu mattis mauris convallis ut. Phasellus mauris metus, " +
//                "porta et felis eget, ullamcorper posuere metus."
        )
    }

//    Spacer(Modifier.height(20.dp))
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextContent(text = text)
        Spacer(Modifier.height(30.dp))
        BluetoothConnections(onUpdateText = { newText ->
            text = newText
        })
    }
}

@Composable
fun TextContent(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        textAlign = TextAlign.Justify,
        modifier = Modifier.padding(top = 30.dp),
    )
}

const val MAC = "4142CF5794E8" // Demo MAC of my headphones for now

@Composable
fun BluetoothConnections(modifier: Modifier = Modifier, onUpdateText: (String) -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    var bluetoothStatusString by remember { mutableStateOf("Checking Bluetooth...") }
    var iconId by remember { mutableIntStateOf(R.drawable.bluetooth_error) }

    var deviceFound by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun isArduinoConnected(MAC: String, onResult: (Boolean) -> Unit) {
        assert(hasBluetoothPermissions(context))
//        val connectedDevices = bluetoothAdapter?.bondedDevices

        val connectedDevices = mutableListOf<BluetoothDevice>()
        var str = ""
//        val latch = CountDownLatch(1)

        // Get connected devices for GATT
        connectedDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT))

        // Query connected devices for A2DP and HEADSET profiles
        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                when (profile) {
                    BluetoothProfile.A2DP -> {
                        connectedDevices.addAll(proxy.connectedDevices)
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                    }

                    BluetoothProfile.HEADSET -> {
                        connectedDevices.addAll(proxy.connectedDevices)
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }

                val found = connectedDevices.any { it.address.replace(":", "") == MAC }
                onResult(found)
            }

            override fun onServiceDisconnected(profile: Int) {
                // Handle the disconnection of the profile if needed
            }
        }, BluetoothProfile.A2DP)
    }

    fun updateBluetoothStatus() {
        if (!hasBluetoothPermissions(context)) {
            bluetoothStatusString = "Permissions not granted!"
            iconId = R.drawable.bluetooth_error
        } else if (bluetoothAdapter == null) {
            bluetoothStatusString = "Device does not support Bluetooth!"
            iconId = R.drawable.bluetooth_error
        } else {
            if (bluetoothAdapter.isEnabled) {
                /*bluetoothStatusString = "Not connected"
                iconId = R.drawable.bluetooth_on

                if (isArduinoConnected(MAC)) {
                    Log.d("Device finder", "Updated it to connected!")
                    bluetoothStatusString = "Connected!"
                    iconId = R.drawable.bluetooth_connected
                }*/

                bluetoothStatusString = "Not connected"
                iconId = R.drawable.bluetooth_on

                isArduinoConnected(MAC) { found ->
                    deviceFound = found
                    bluetoothStatusString = if (found) {
                        "Connected!"
                    } else {
                        "Not connected"
                    }
                    iconId = if (found) R.drawable.bluetooth_connected else R.drawable.bluetooth_on
                }
            } else {
                bluetoothStatusString = "Bluetooth is off"
                iconId = R.drawable.bluetooth_disabled
            }
        }
    }

    val bluetoothReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1?.action == BluetoothAdapter.ACTION_STATE_CHANGED)
                    updateBluetoothStatus()
            }
        }
    }

    DisposableEffect(bluetoothAdapter) {
        updateBluetoothStatus()

        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        onDispose {
            context.unregisterReceiver(bluetoothReceiver)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (!hasBluetoothPermissions(context))
                requestBluetoothPermissions(context as ComponentActivity)
            updateBluetoothStatus()
        }) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(text = "  Refresh")
        }

        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(id = iconId), contentDescription = null)

            Text(
                text = "  $bluetoothStatusString"
            )
        }
    }
}

private fun hasBluetoothPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
}

private fun requestBluetoothPermissions(activity: ComponentActivity) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        ),
        1
    )
}