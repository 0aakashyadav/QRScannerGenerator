from pathlib import Path
import re
root=Path.cwd()
main=root/'app/src/main/java/com/aakash/qrscanner/MainActivity.kt'
s=main.read_text()

# Imports: add modern UI imports and replace deprecated ArrowBack import usage safely.
s=s.replace('import androidx.compose.ui.graphics.asImageBitmap\n', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.asImageBitmap\n')
s=s.replace('import androidx.compose.ui.platform.LocalContext\n', 'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.painterResource\n')
s=s.replace('import androidx.compose.material.icons.filled.*\n', 'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.automirrored.filled.ArrowBack\n')
s=s.replace('Icons.Default.ArrowBack', 'Icons.AutoMirrored.Filled.ArrowBack')

# Theme/app shell.
new_qrapp='''@Composable
fun QRApp(repo: HistoryRepository, adsReady: Boolean) {
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
                    Screen.HOME -> HomeScreen({ screen = Screen.SCAN }, { screen = Screen.GENERATE }, adsReady)
                    Screen.SCAN -> ScanScreen({ screen = Screen.HOME }, vm)
                    Screen.GENERATE -> GenerateScreen({ screen = Screen.HOME }, vm, adsReady)
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
'''

def replace_fun(text,name,new):
    pat=re.compile(r'(?ms)^@Composable\n(?:private )?fun '+re.escape(name)+r'\b.*?(?=^@Composable\n(?:private )?fun |^fun |\Z)')
    m=pat.search(text)
    if not m:
        raise SystemExit(f'function {name} not found')
    return text[:m.start()]+new+'\n\n'+text[m.end():]

home='''@Composable
fun HomeScreen(onScan: () -> Unit, onGenerate: () -> Unit, adsReady: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_qr_logo), "QR Scanner & Generator", Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("QR Scanner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Scan • Create • Share", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (adsReady) AdBanner(Modifier.fillMaxWidth())
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
'''

scan='''@Composable
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
'''

result='''@Composable
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
'''

generate='''@Composable
fun GenerateScreen(onBack: () -> Unit, vm: QRViewModel, adsReady: Boolean) {
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
                            val generated = runCatching { QrGenerator.encode(content) }
                            generated.onSuccess {
                                bitmap = it
                                scope.launch { vm.add(HistoryKind.GENERATED, type, content) }
                            }.onFailure { validation = "This content is too long or cannot be encoded as a QR code." }
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
        if (adsReady) AdBanner(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}
'''

s=replace_fun(s,'QRApp',new_qrapp)
s=replace_fun(s,'HomeScreen',home)
s=replace_fun(s,'ScanScreen',scan)
s=replace_fun(s,'ResultCard',result)
s=replace_fun(s,'GenerateScreen',generate)

# Clean imports and add a simple scanner overlay without changing camera lifecycle code.
s=s.replace('import androidx.compose.foundation.background\n','')
s=s.replace('import com.google.mlkit.vision.barcode.BarcodeScanner\n','')
old_camera = '    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().weight(1f))\n'
new_camera = '''    Box(Modifier.fillMaxWidth().weight(1f)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).size(250.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.9f))
            ) {}
            Text(
                "Align a QR or barcode inside the frame",
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 48.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
'''
if old_camera in s:
    s=s.replace(old_camera,new_camera)

# Advanced scanner formats retained.
s=s.replace('.setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)', '.setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)')

main.write_text(s)

# AdMob adaptive API used by current source.
ad=root/'app/src/main/java/com/aakash/qrscanner/ads/AdBanner.kt'
a=ad.read_text().replace('getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)', 'getLargeAnchoredAdaptiveBannerAdSize(context, width)')
ad.write_text(a)

# Logo resources.
res=root/'app/src/main/res'
(res/'drawable').mkdir(parents=True,exist_ok=True)
(res/'values').mkdir(parents=True,exist_ok=True)
logo='''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#4F46E5" android:pathData="M18,4h72a14,14 0,0 1,14 14v72a14,14 0,0 1,-14 14h-72a14,14 0,0 1,-14 -14v-72a14,14 0,0 1,14 -14z"/>
    <path android:fillColor="#7C3AED" android:fillAlpha="0.65" android:pathData="M74,4h16a14,14 0,0 1,14 14v72a14,14 0,0 1,-14 14h-16z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M20,20h25v25h-25zM63,20h25v25h-25zM20,63h25v25h-25z"/>
    <path android:fillColor="#4F46E5" android:pathData="M26,26h13v13h-13zM69,26h13v13h-13zM26,69h13v13h-13z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M52,52h9v9h-9zM65,52h9v9h-9zM78,52h10v9h-10zM52,65h9v9h-9zM65,65h9v23h-9zM78,66h10v9h-10zM52,78h9v10h-9zM78,78h10v10h-10z"/>
    <path android:fillColor="#67E8F9" android:pathData="M12,27h5v-9h9v-5h-14zM81,13h9v5h5v9h-5v-5h-9zM12,81h5v9h9v5h-14zM95,81v14h-14v-5h9v-9z"/>
</vector>
'''
(res/'drawable/ic_qr_logo.xml').write_text(logo)
(res/'drawable/ic_launcher.xml').write_text(logo)
# Keep foreground for compatibility but use the polished logo for the launcher.
strings=(res/'values/strings.xml')
ss=strings.read_text()
ss=ss.replace('<string name="app_name">QR Scanner &amp; Generator</string>', '<string name="app_name">QR Scanner &amp; Generator</string>')
strings.write_text(ss)
manifest=root/'app/src/main/AndroidManifest.xml'
m=manifest.read_text().replace('android:icon="@drawable/ic_launcher_foreground"','android:icon="@drawable/ic_launcher"').replace('android:roundIcon="@drawable/ic_launcher_foreground"','android:roundIcon="@drawable/ic_launcher"')
manifest.write_text(m)

# Update README/store docs minimally to mention polished UI/icon.
print('Updated:', main)
