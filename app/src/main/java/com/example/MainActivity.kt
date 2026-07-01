package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "drives") {
          composable("drives") { DriveListScreen(navController) }
          composable("terminal/{driveId}") { backStackEntry ->
            val driveId = backStackEntry.arguments?.getString("driveId") ?: "unknown"
            TerminalRecoveryScreen(navController, driveId)
          }
          composable("results") { ResultsScreen(navController) }
        }
      }
    }
  }
}

data class StorageDevice(
  val id: String,
  val name: String,
  val type: String,
  val capacity: String,
  val status: String,
  val driveLetter: String,
  val isCorrupted: Boolean,
  val icon: ImageVector
)

var globalDevices = listOf<StorageDevice>()

fun detectStorageDevices(context: android.content.Context): List<StorageDevice> {
    val devices = mutableListOf<StorageDevice>()
    val storageManager = context.getSystemService(android.content.Context.STORAGE_SERVICE) as android.os.storage.StorageManager
    
    try {
        val volumes = storageManager.storageVolumes
        var driveLetterChar = 'D'
        for (volume in volumes) {
            if (volume.isRemovable) {
                val isUsb = volume.getDescription(context).contains("USB", ignoreCase = true)
                devices.add(
                    StorageDevice(
                        id = volume.uuid ?: java.util.UUID.randomUUID().toString(),
                        name = volume.getDescription(context),
                        type = if (isUsb) "USB Storage" else "SD Card",
                        capacity = "Unknown Capacity",
                        status = volume.state,
                        driveLetter = "${driveLetterChar++}:",
                        isCorrupted = volume.state == android.os.Environment.MEDIA_UNMOUNTED || volume.state == android.os.Environment.MEDIA_UNMOUNTABLE,
                        icon = if (isUsb) Icons.Default.Usb else Icons.Default.SdStorage
                    )
                )
            } else {
                devices.add(
                    StorageDevice(
                        id = volume.uuid ?: "internal",
                        name = "Internal Storage",
                        type = "Internal Flash",
                        capacity = "Unknown Capacity",
                        status = volume.state,
                        driveLetter = "C:",
                        isCorrupted = false,
                        icon = Icons.Default.Smartphone
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    if (devices.none { it.isCorrupted }) {
        devices.add(
            StorageDevice("dev_sdcard1_mock", "Samsung EVO Select", "MicroSD Card", "128.0 GB", "Unallocated / RAW", "E:", true, Icons.Default.SdStorage)
        )
        devices.add(
            StorageDevice("dev_usb1_mock", "SanDisk Cruzer Glide", "USB Flash Drive", "32.0 GB", "Partition Table Missing", "F:", true, Icons.Default.Usb)
        )
    }
    
    globalDevices = devices
    return devices
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveListScreen(navController: NavController) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val devices = remember { detectStorageDevices(context) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("DriveRescue Pro", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = "SELECT TARGET DRIVE",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleSmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 16.dp)
      )
      
      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(devices) { device ->
          DriveItem(device) {
            if (device.isCorrupted) {
              navController.navigate("terminal/${device.id}")
            }
          }
        }
      }
    }
  }
}

@Composable
fun DriveItem(device: StorageDevice, onClick: () -> Unit) {
  val borderColor = if (device.isCorrupted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
  val backgroundColor = MaterialTheme.colorScheme.surface
  
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .border(1.dp, borderColor, RoundedCornerShape(8.dp))
      .background(backgroundColor)
      .clickable(enabled = device.isCorrupted) { onClick() }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = device.icon,
      contentDescription = null,
      tint = if (device.isCorrupted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(40.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = "${device.type} • ${device.capacity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Drive: ${device.driveLetter} | Status: ${device.status}",
        style = MaterialTheme.typography.bodySmall,
        color = if (device.isCorrupted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        fontFamily = FontFamily.Monospace
      )
    }
    if (device.isCorrupted) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Corrupted",
        tint = MaterialTheme.colorScheme.secondary
      )
    } else {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Healthy",
        tint = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalRecoveryScreen(navController: NavController, driveId: String) {
  val device = globalDevices.find { it.id == driveId } ?: return
  var logs by remember { mutableStateOf(listOf<String>()) }
  var progress by remember { mutableFloatStateOf(0f) }
  var isScanning by remember { mutableStateOf(true) }
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    logs = logs + "INITIATING LOW-LEVEL BLOCK SCAN ON [${device.name}]..."
    delay(800)
    logs = logs + "Bypassing OS mount point..."
    delay(500)
    logs = logs + "Connecting to generic mass storage driver..."
    delay(600)
    logs = logs + "WARNING: Partition table (MBR/GPT) missing or corrupt."
    delay(1000)
    logs = logs + "Attempting heuristic block signature matching (Deep Scan)."
    delay(800)

    for (i in 1..100) {
      delay((20..80).random().toLong())
      progress = i / 100f
      
      if (i % 5 == 0) {
        val blockAddr = String.format("0x%08X", (i * 4096) + (0..4000).random())
        logs = logs + "Reading block $blockAddr... OK"
      }
      
      if (i == 23) logs = logs + ">>> FOUND MAGIC HEADER: JPEG Image <<<"
      if (i == 45) logs = logs + ">>> FOUND MAGIC HEADER: PDF Document <<<"
      if (i == 67) logs = logs + ">>> FOUND ORPHANED INODE: /DCIM/Camera <<<"
      if (i == 82) logs = logs + ">>> FOUND MAGIC HEADER: MP4 Video <<<"
      if (i == 95) logs = logs + "Rebuilding virtual file allocation table..."

      scope.launch {
        listState.animateScrollToItem(logs.size - 1)
      }
    }
    
    delay(1000)
    logs = logs + "SCAN COMPLETE. 4 RECOVERABLE ARTIFACTS FOUND."
    isScanning = false
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Deep Scan", fontFamily = FontFamily.Monospace) },
        navigationIcon = {
          IconButton(onClick = { if (!isScanning) navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.primary,
          navigationIconContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF000000))
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        LazyColumn(state = listState) {
          items(logs) { log ->
            Text(
              text = log,
              color = if (log.startsWith(">>>") || log.startsWith("WARNING")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing), label = ""
      )

      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Scan Progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
          Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = { navController.navigate("results") { popUpTo("drives") } },
        enabled = !isScanning,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
      ) {
        Text("VIEW RECOVERED FILES", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(navController: NavController) {
  val files = listOf(
    Pair("IMG_20230514_102230.jpg", "4.2 MB"),
    Pair("Vacation_Video_01.mp4", "312.5 MB"),
    Pair("Financial_Report_Q2.pdf", "1.8 MB"),
    Pair("IMG_20230514_102501.jpg", "3.9 MB")
  )

  var isRecovering by remember { mutableStateOf(false) }
  var recoveryComplete by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Artifacts Found", fontFamily = FontFamily.Monospace) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.primary,
          navigationIconContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
    ) {
      if (recoveryComplete) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            "RECOVERY SUCCESSFUL",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "Files have been restored to /InternalStorage/DriveRescue/",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      } else {
        Text(
          text = "READY FOR EXTRACTION",
          color = MaterialTheme.colorScheme.secondary,
          style = MaterialTheme.typography.titleSmall,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(files) { file ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val icon = when {
                file.first.endsWith(".jpg") -> Icons.Default.Image
                file.first.endsWith(".mp4") -> Icons.Default.Videocam
                else -> Icons.Default.InsertDriveFile
              }
              Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(file.first, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(file.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = { 
            isRecovering = true
            // Simulate recovery write process
          },
          enabled = !isRecovering,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
          if (isRecovering) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Text("EXTRACTING BLOCKS...", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          } else {
            Text("RESTORE PARTITION & FILES", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
        }
        
        LaunchedEffect(isRecovering) {
          if (isRecovering) {
            delay(2500)
            recoveryComplete = true
          }
        }
      }
    }
  }
}
