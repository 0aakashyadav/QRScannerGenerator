package com.aakash.qrscanner
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aakash.qrscanner.data.HistoryEntity
import com.aakash.qrscanner.data.HistoryKind
import com.aakash.qrscanner.data.HistoryRepository
import com.aakash.qrscanner.generator.QrGenerator
import com.aakash.qrscanner.scanner.barcodeTitle
import com.aakash.qrscanner.scanner.isSafeWebUrl
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class Screen { HOME, SCAN, GENERATE, HISTORY, SETTINGS }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as QRApplication).repository
        setContent { QRApp(repo) }
    }
}

class QRViewModel(private val repo: HistoryRepository) : ViewModel() {
    val history = repo.items
    suspend fun add(kind: HistoryKind, title: String, content: String) = repo.add(kind, title, content)
    suspend fun delete(id: Long) = repo.delete(id)
    suspend fun clearHistory() = repo.clear()
}

class QRViewModelFactory(private val repo: HistoryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = QRViewModel(repo) as T
}

@Composable
fun QRApp(repo: HistoryRepository) {
    val vm: QRViewModel = viewModel(factory = QRViewModelFactory(repo))
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    val scope = rememberCoroutineScope()
    val storedTheme by AppSettings.themeFlow(context).collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    LaunchedEffect(storedTheme) { themeMode = storedTheme }
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val lightColors = lightColorScheme(
        primary = Color(0xFF4F46E5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8E7FF),
        onPrimaryContainer = Color(0xFF17134A),
        secondary = Color(0xFF0891B2),
        secondaryContainer = Color(0xFFCFFAFE),
        background = Color(0xFFF7F8FC),
        surface = Color.White,
        surfaceVariant = Color(0xFFEFF1F7)
    )
    val darkColors = darkColorScheme(
        primary = Color(0xFFB8B6FF),
        onPrimary = Color(0xFF24205C),
        primaryContainer = Color(0xFF393487),
        onPrimaryContainer = Color(0xFFE9E7FF),
        secondary = Color(0xFF67E8F9),
        secondaryContainer = Color(0xFF164E63),
        background = Color(0xFF0D0F16),
        surface = Color(0xFF151821),
        surfaceVariant = Color(0xFF232735)
    )
    MaterialTheme(colorScheme = if (dark) darkColors else lightColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (screen != Screen.SCAN) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(screen == Screen.HOME, { screen = Screen.HOME }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                        NavigationBarItem(screen == Screen.HISTORY, { screen = Screen.HISTORY }, icon = { Icon(Icons.Default.History, "History") }, label = { Text("History") })
                        NavigationBarItem(screen == Screen.SETTINGS, { screen = Screen.SETTINGS }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Settings") })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen({ screen = Screen.SCAN }, { screen = Screen.GENERATE })
                    Screen.SCAN -> ScanScreen({ screen = Screen.HOME }, vm)
                    Screen.GENERATE -> GenerateScreen({ screen = Screen.HOME }, vm)
                    Screen.HISTORY -> HistoryScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(themeMode) { mode ->
                        themeMode = mode
                        scope.launch { AppSettings.setTheme(context, mode) }
                    }
                }
            }
        }
    }
}


@Composable
fun HomeScreen(onScan: () -> Unit, onGenerate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_qr_logo), "QR Scanner & Generator", Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("QR Scanner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Scan � Create � Share", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Everything QR, in one place", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Scan QR codes and barcodes, or create a QR code in seconds.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(7.dp))
                            Text("Private & on-device", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeActionCard("Scan QR", "QR & barcodes", Icons.Default.QrCodeScanner, onScan, Modifier.weight(1f))
            HomeActionCard("Create QR", "Text, Wi-Fi & more", Icons.Default.QrCode2, onGenerate, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))

        Spacer(Modifier.height(12.dp))
        Text(
            "No account required. Your QR content stays on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, null, Modifier.padding(10.dp).size(26.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
fun BigAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(18.dp)) {
        Icon(icon, null); Spacer(Modifier.width(12.dp)); Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScanScreen(onBack: () -> Unit, vm: QRViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        permissionDenied = !it
    }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("Text") }
    var torch by rememberSaveable { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    fun acceptResult(value: String, type: String) {
        if (result != null || value.isBlank()) return
        result = value
        title = type
        scope.launch { vm.add(HistoryKind.SCANNED, type, value) }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scanImage(context, uri, ::acceptResult)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permission.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(camera, torch) { camera?.cameraControl?.enableTorch(torch) }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Scan QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Point your camera at a code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(enabled = camera != null && hasPermission && result == null, onClick = { torch = !torch }) {
                    Icon(if (torch) Icons.Default.FlashOn else Icons.Default.FlashOff, "Flashlight")
                }
                IconButton(onClick = { gallery.launch("image/*") }) { Icon(Icons.Default.PhotoLibrary, "Scan from image") }
            }
        }
        when {
            !hasPermission -> PermissionState(permissionDenied) {
                permissionDenied = false
                permission.launch(Manifest.permission.CAMERA)
            }
            result == null -> CameraPreview(
                onCamera = { camera = it },
                onCameraUnavailable = { toast(context, "Camera could not be started") },
                onResult = ::acceptResult
            )
            else -> ResultCard(
                title = title,
                value = result!!,
                onClear = { result = null; torch = false },
                onCopy = { copy(context, result!!); toast(context, "Copied") },
                onShare = { shareText(context, result!!) },
                onOpen = if (isSafeWebUrl(result!!)) ({ openUrl(context, result!!) }) else null
            )
        }
    }
}


@Composable
private fun PermissionState(permanentlyDenied: Boolean, request: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CameraAlt, null, Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(if (permanentlyDenied) "Camera permission is needed to scan QR codes." else "Camera permission is required to scan QR codes.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = if (permanentlyDenied) ({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) else request) {
            Text(if (permanentlyDenied) "Open app settings" else "Allow camera")
        }
    }
}

@Composable
fun CameraPreview(
    onCamera: (Camera) -> Unit,
    onCameraUnavailable: () -> Unit,
    onResult: (String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }
    val delivered = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    processCameraFrame(
                        proxy = proxy,
                        scanner = scanner,
                        delivered = delivered,
                        onResult = onResult
                    )
                }
                val cam = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                onCamera(cam)
            } catch (_: Exception) {
                onCameraUnavailable()
            }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            runCatching { scanner.close() }
            runCatching { executor.shutdownNow() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(
    androidx.camera.core.ExperimentalGetImage::class
)
private fun processCameraFrame(
    proxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    delivered: AtomicBoolean,
    onResult: (String, String) -> Unit
) {
    val mediaImage = proxy.image

    if (mediaImage == null || delivered.get()) {
        proxy.close()
        return
    }

    scanner.process(
        InputImage.fromMediaImage(
            mediaImage,
            proxy.imageInfo.rotationDegrees
        )
    ).addOnSuccessListener { codes ->
        val code = codes.firstOrNull { !it.rawValue.isNullOrBlank() }

        if (code != null && delivered.compareAndSet(false, true)) {
            onResult(
                code.rawValue.orEmpty(),
                barcodeTitle(code)
            )
        }
    }.addOnCompleteListener {
        proxy.close()
    }
}

fun scanImage(context: Context, uri: Uri, onResult: (String, String) -> Unit) {
    runCatching { InputImage.fromFilePath(context, uri) }
        .onSuccess { image ->
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { codes ->
                    val code = codes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (code != null) onResult(code.rawValue.orEmpty(), barcodeTitle(code)) else toast(context, "No QR code found in this image")
                }
                .addOnFailureListener { toast(context, "Could not scan this image") }
                .addOnCompleteListener { scanner.close() }
        }
        .onFailure { toast(context, "Could not open this image") }
}

@Composable
fun ResultCard(title: String, value: String, onClear: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit, onOpen: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("$title detected", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Text(value, Modifier.fillMaxWidth().padding(18.dp), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCopy, Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy") }
            OutlinedButton(onClick = onShare, Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
        }
        if (onOpen != null) {
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpen, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(8.dp)); Text("Open website")
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onClear, Modifier.align(Alignment.CenterHorizontally)) { Text("Scan another") }
    }
}


@Composable
fun GenerateScreen(onBack: () -> Unit, vm: QRViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    var type by rememberSaveable { mutableStateOf("Text") }
    var content by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val current = bitmap
        if (uri != null && current != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    check(current.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encoding failed" }
                } ?: error("Unable to open destination")
            }
            saved.onSuccess { toast(context, "QR image saved") }.onFailure { toast(context, "Could not save QR image") }
        }
    }
    val types = listOf("Text", "Website", "Wi-Fi", "Phone", "Email", "SMS", "Contact", "Location")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("Create QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Choose a type and generate instantly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("QR type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            types.forEach { item ->
                FilterChip(
                    selected = type == item,
                    onClick = { type = item; content = ""; bitmap = null; validation = null },
                    label = { Text(item) },
                    leadingIcon = if (type == item) ({ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }) else null
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                GeneratorFields(type) { content = it }
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = content.isNotBlank(),
                    onClick = {
                        validation = validateGenerator(type, content)
                        if (validation == null) {
                            scope.launch {
                                val generated = runCatching {
                                    withContext(Dispatchers.Default) {
                                        QrGenerator.encode(content)
                                    }
                                }

                                generated.onSuccess {
                                    bitmap = it
                                    vm.add(HistoryKind.GENERATED, type, content)
                                }.onFailure {
                                    validation = "This content is too long or cannot be encoded as a QR code."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.QrCode2, null); Spacer(Modifier.width(8.dp)); Text("Generate QR") }
            }
        }
        bitmap?.let { bmp ->
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your QR code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp) {
                        Image(bmp.asImageBitmap(), "Generated QR code", Modifier.padding(14.dp).size(260.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ saveLauncher.launch("QR_${System.currentTimeMillis()}.png") }, Modifier.weight(1f)) { Icon(Icons.Default.SaveAlt, null); Spacer(Modifier.width(5.dp)); Text("Save") }
                        Button({ shareBitmap(context, bmp) }, Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(5.dp)); Text("Share") }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(8.dp))
    }
}


private fun vmScopeAddGenerated(
    scope: kotlinx.coroutines.CoroutineScope,
    vm: QRViewModel,
    type: String,
    content: String
) {
    scope.launch {
        vm.add(HistoryKind.GENERATED, type, content)
    }
}

@Composable
fun GeneratorFields(type: String, onContent: (String) -> Unit) {
    var a by remember(type) { mutableStateOf("") }
    var b by remember(type) { mutableStateOf("") }
    var c by remember(type) { mutableStateOf("") }
    var d by remember(type) { mutableStateOf("") }
    @Composable
    fun field(label: String, value: String, set: (String) -> Unit, single: Boolean = true) {
        OutlinedTextField(value, set, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = single)
    }
    when (type) {
        "Text" -> field("Text", a, { a = it; onContent(a) }, false)
        "Website" -> field("Website URL (https://example.com)", a, { a = it; onContent(a) })
        "Phone" -> field("Phone number", a, { a = it; onContent("tel:${a.trim()}") })
        "Email" -> {
            field("Email address", a, { a = it }); field("Subject (optional)", b, { b = it }); field("Message (optional)", c, { c = it }, false)
            LaunchedEffect(a, b, c) { onContent(if (a.isBlank()) "" else "mailto:${Uri.encode(a.trim())}?subject=${Uri.encode(b)}&body=${Uri.encode(c)}") }
        }
        "SMS" -> {
            field("Phone number", a, { a = it }); field("Message", b, { b = it }, false)
            LaunchedEffect(a, b) { onContent(if (a.isBlank()) "" else "SMSTO:${a.trim()}:${b}") }
        }
        "Wi-Fi" -> {
            field("Network name (SSID)", a, { a = it }); field("Password", b, { b = it })
            Text("Security", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WPA", "WEP", "nopass").forEach { security -> FilterChip(selected = c.ifBlank { "WPA" } == security, onClick = { c = security }, label = { Text(security) }) }
            }
            SwitchRow("Hidden network", d == "true") { d = it.toString() }
            LaunchedEffect(a, b, c, d) { if (a.isNotBlank()) onContent(QrGenerator.wifi(a, b, c.ifBlank { "WPA" }, d == "true")) else onContent("") }
        }
        "Contact" -> {
            field("Name", a, { a = it }); field("Phone (optional)", b, { b = it }); field("Email (optional)", c, { c = it }); field("Organization (optional)", d, { d = it })
            LaunchedEffect(a, b, c, d) { onContent(if (a.isBlank()) "" else QrGenerator.vCard(a, b, c, d)) }
        }
        "Location" -> {
            field("Latitude (-90 to 90)", a, { a = it }); field("Longitude (-180 to 180)", b, { b = it }); field("Label (optional)", c, { c = it })
            LaunchedEffect(a, b, c) { if (a.isNotBlank() && b.isNotBlank()) onContent(QrGenerator.location(a, b, c)) else onContent("") }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onChecked) }
}

@Composable
fun HistoryScreen(vm: QRViewModel) {
    val items by vm.history.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<HistoryEntity?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) TextButton({ confirmClear = true }) { Text("Clear all") }
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No QR history yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items, key = { it.id }) { item ->
                    HistoryRow(item, { selected = item }, { scope.launch { vm.delete(item.id) } }, { copy(context, item.content); toast(context, "Copied") }, { shareText(context, item.content) })
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear history?") },
        text = { Text("This will permanently remove all saved scan and generated QR history from this device.") },
        confirmButton = { TextButton({ confirmClear = false; scope.launch { vm.clearHistory() } }) { Text("Clear") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel") } }
    )
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.title) },
            text = { Text(item.content, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton({ shareText(context, item.content) }) { Text("Share") } },
            dismissButton = { TextButton({ copy(context, item.content); toast(context, "Copied") }) { Text("Copy") } }
        )
    }
}

@Composable
fun HistoryRow(item: HistoryEntity, open: () -> Unit, delete: () -> Unit, copy: () -> Unit, share: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text(item.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(item.timestamp)), style = MaterialTheme.typography.labelSmall)
            }
            Text(item.content, maxLines = 2, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable(onClick = open).padding(top = 6.dp))
            Row {
                TextButton(copy) { Text("Copy") }; TextButton(share) { Text("Share") }; TextButton(delete) { Text("Delete") }
            }
        }
    }
}

@Composable
fun SettingsScreen(mode: ThemeMode, setMode: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { setMode(option) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == option, onClick = { setMode(option) }); Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text("QR contents are processed locally. The app does not have an account system or a backend for QR data.", Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Text("Only camera permission is requested, and only when you choose Scan QR. Gallery selection uses the Android system picker, so storage permission is not required.", Modifier.padding(top = 8.dp))
    }
}

fun validateGenerator(type: String, content: String): String? = when (type) {
    "Website" -> if (!isSafeWebUrl(content.trim())) "Enter a valid http:// or https:// URL." else null
    "Email" -> if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(content.removePrefix("mailto:").substringBefore('?'))) "Enter a valid email address." else null
    "Location" -> runCatching {
        val uri = Uri.parse(content)
        val lat = uri.schemeSpecificPart.substringBefore(',').toDouble()
        val lon = uri.schemeSpecificPart.substringAfter(',').substringBefore('?').toDouble()
        require(lat in -90.0..90.0 && lon in -180.0..180.0)
        null
    }.getOrElse { "Enter valid latitude and longitude values." }
    else -> null
}

fun copy(context: Context, value: String) {
    context.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("QR result", value))
}

fun shareText(context: Context, value: String) {
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, value) }, "Share QR result"))
    }.onFailure { toast(context, "No app is available to share this result") }
}

fun openUrl(context: Context, value: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }
    catch (_: ActivityNotFoundException) { toast(context, "No browser is available") }
}

fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val file = context.cacheDir.resolve("shared_qr_${System.currentTimeMillis()}.png")
    val uri = runCatching {
        file.outputStream().use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encoding failed" } }
        androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }.getOrElse { toast(context, "Could not prepare QR image"); return }
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share QR image"))
    }.onFailure { toast(context, "No app is available to share this image") }
}


















