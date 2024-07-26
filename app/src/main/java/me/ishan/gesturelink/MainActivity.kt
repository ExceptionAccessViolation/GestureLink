package me.ishan.gesturelink

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
//    Spacer(Modifier.height(20.dp))
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextContent() // and here
        Spacer(Modifier.height(30.dp))
        BluetoothConnections()
    }
}

@Composable
fun TextContent(modifier: Modifier = Modifier) {
    Text(
        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
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
        textAlign = TextAlign.Justify,
        modifier = Modifier.padding(top = 30.dp), // here
    )
}

@Composable
fun BluetoothConnections(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    var bluetoothStatusString by remember { mutableStateOf("Checking Bluetooth...") }
    var iconId by remember { mutableIntStateOf(R.drawable.bluetooth_error) }

    fun bluetoothStatus() {
        if (!hasBluetoothPermissions(context)) {
            bluetoothStatusString = "Permissions not granted!"
            iconId = R.drawable.bluetooth_error
        } else if (bluetoothAdapter == null) {
            bluetoothStatusString = "Device does not support Bluetooth!"
            iconId = R.drawable.bluetooth_error
        } else {
            if (bluetoothAdapter.isEnabled) {
                bluetoothStatusString = "Not connected"
                iconId = R.drawable.bluetooth_on
            } else {
                bluetoothStatusString = "Bluetooth is off"
                iconId = R.drawable.bluetooth_disabled
            }
        }
    }

    fun isArduinoConnected(MAC: String) {
        if (!hasBluetoothPermissions(context)) return
        val connectedDevices = bluetoothAdapter?.bondedDevices
    }

    val bluetoothReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1?.action == BluetoothAdapter.ACTION_STATE_CHANGED)
                    bluetoothStatus()
            }
        }
    }

    DisposableEffect(bluetoothAdapter) {
        bluetoothStatus()

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
            bluetoothStatus()
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
//            && (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
}

// Request Bluetooth permissions
private fun requestBluetoothPermissions(activity: ComponentActivity) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH_CONNECT
        ),
        1 // Request code
    )
}
