package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScannedProduct
import com.example.ui.BarcodeScannerViewModel
import com.example.ui.CameraPreview
import com.example.ui.ChatMessage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: BarcodeScannerViewModel by viewModels()
    private val hasCameraPermission = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission.value = isGranted
        if (isGranted) {
            triggerVibration()
        } else {
            Toast.makeText(
                this,
                "برای استفاده از اسکنر، دسترسی دوربین الزامی است.",
                Toast.LENGTH_LONG
            ).apply {
                // Support RTL language on toast
                show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync initial permission check
        hasCameraPermission.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MyApplicationTheme {
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                val activeProduct by viewModel.activeProduct.collectAsStateWithLifecycle()
                val isLoadingProduct by viewModel.isLoadingProduct.collectAsStateWithLifecycle()
                val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
                val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
                val filteredHistory by viewModel.filteredHistory.collectAsStateWithLifecycle()
                val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
                val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
                        bottomBar = {
                            NavigationBar(
                                containerColor = SlateDark,
                                tonalElevation = 8.dp,
                                modifier = Modifier.navigationBarsPadding().height(80.dp)
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { viewModel.setSelectedTab(0) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "اسکنر",
                                            tint = if (selectedTab == 0) SlateDark else SoftGray
                                        )
                                    },
                                    label = {
                                        Text(
                                            "اسکنر",
                                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == 0) NeonEmerald else SoftGray,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = NeonEmerald
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { viewModel.setSelectedTab(1) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "تاریخچه",
                                            tint = if (selectedTab == 1) SlateDark else SoftGray
                                        )
                                    },
                                    label = {
                                        Text(
                                            "تاریخچه",
                                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == 1) NeonEmerald else SoftGray,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = NeonEmerald
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { viewModel.setSelectedTab(2) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "دستیار هوشمند",
                                            tint = if (selectedTab == 2) SlateDark else SoftGray
                                        )
                                    },
                                    label = {
                                        Text(
                                            "دستیار هوش‌مصنوعی",
                                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == 2) NeonEmerald else SoftGray,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = NeonEmerald
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SlateDark)
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                0 -> ScannerTabScreen(
                                    hasPermission = hasCameraPermission.value,
                                    onRequestPermission = {
                                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    isScanning = isScanning,
                                    torchEnabled = torchEnabled,
                                    onTorchChange = { viewModel.setTorchEnabled(it) },
                                    onBarcodeDetected = { barcode ->
                                        triggerVibration()
                                        viewModel.processBarcode(barcode)
                                    },
                                    isLoadingProduct = isLoadingProduct,
                                    activeProduct = activeProduct,
                                    onDismissProduct = { viewModel.dismissActiveProduct() },
                                    onToggleFavorite = { viewModel.toggleProductFavorite(it) },
                                    onManualBarcodeSubmit = { barcode ->
                                        triggerVibration()
                                        viewModel.processBarcode(barcode)
                                    },
                                    onAskAiClicked = { product ->
                                        viewModel.setSelectedTab(2)
                                        viewModel.sendChatMessage("در مورد محصول ${product.name} با برند ${product.brand} و رده ${product.category} برام به تفصیل توضیح بده و بگو چقدر سالمه؟")
                                    }
                                )
                                1 -> HistoryTabScreen(
                                    historyList = filteredHistory,
                                    onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                                    onProductClicked = { product ->
                                        viewModel.processBarcode(product.barcode)
                                    },
                                    onToggleFavorite = { viewModel.toggleProductFavorite(it) },
                                    onDeleteProduct = { viewModel.deleteProduct(it) },
                                    onClearAll = { viewModel.clearHistory() }
                                )
                                2 -> AiAssistantTabScreen(
                                    chatHistory = chatHistory,
                                    isChatLoading = isChatLoading,
                                    onSendMessage = { viewModel.sendChatMessage(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(100)
        } catch (e: Exception) {
            // Ignored if device has no vibration support or missing permission context
        }
    }
}

// ======================== TABS IMPLEMENTATION ========================

@Composable
fun ScannerTabScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    isScanning: Boolean,
    torchEnabled: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onBarcodeDetected: (String) -> Unit,
    isLoadingProduct: Boolean,
    activeProduct: ScannedProduct?,
    onDismissProduct: () -> Unit,
    onToggleFavorite: (ScannedProduct) -> Unit,
    onManualBarcodeSubmit: (String) -> Unit,
    onAskAiClicked: (ScannedProduct) -> Unit
) {
    var showManualInput by remember { mutableStateOf(false) }

    if (!hasPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), ShapeDefaults.Medium)
                    .border(2.dp, NeonEmerald, ShapeDefaults.Medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Permission Instruction",
                    modifier = Modifier.size(54.dp),
                    tint = NeonEmerald
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "دسترسی به دوربین دوربین",
                color = SoftWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "برای اسکن زنده بارکد روی کالاها، این برنامه نیاز به دسترسی به دوربین دستگاه شما دارد. لطفاً روی دکمه زیر زده و دسترسی را فعال نمایید.",
                color = SoftGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).testTag("grant_permission_button")
            ) {
                Text("فعال‌سازی دسترسی دوربین", color = SlateDark, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().testTag("scanner_viewport")) {
            // Camera scanner viewfinder
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                isScanning = isScanning && (activeProduct == null) && !isLoadingProduct,
                torchEnabled = torchEnabled,
                onBarcodeScanned = onBarcodeDetected
            )

            // Dynamic Scanning Hologram overlay
            ScannerViewerOverlay()

            // Viewfinder controls overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onTorchChange(!torchEnabled) },
                        modifier = Modifier
                            .background(SlateDark.copy(alpha = 0.6f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "تغییر وضعیت فلاش",
                            tint = if (torchEnabled) NeonAmber else SoftWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(SlateDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "کادر را روبه‌روی بارکد بگیرید",
                            color = SoftWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { showManualInput = true },
                        modifier = Modifier
                            .background(SlateDark.copy(alpha = 0.6f), CircleShape)
                            .size(44.dp).testTag("manual_entry_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "ورود دستی بارکد",
                            tint = NeonTeal
                        )
                    }
                }

                // AI Product Detailed Analysis slides up from bottom
                AnimatedVisibility(
                    visible = activeProduct != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    if (activeProduct != null) {
                        ProductDetailSheet(
                            product = activeProduct,
                            onDismiss = onDismissProduct,
                            onToggleFavorite = onToggleFavorite,
                            onAskAiClicked = onAskAiClicked
                        )
                    }
                }
            }

            // High intelligence scanning loader
            if (isLoadingProduct) {
                InteractiveAnalysisLoader()
            }

            // Keyboard/Manual Entry dialog
            if (showManualInput) {
                ManualBarcodeDialog(
                    onDismiss = { showManualInput = false },
                    onSubmit = { barcode ->
                        showManualInput = false
                        onManualBarcodeSubmit(barcode)
                    }
                )
            }
        }
    }
}

@Composable
fun ScannerViewerOverlay() {
    // Continuous up-down pulsing scanning laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulseLaser")
    val laserOffsetPerc by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPulsing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val boxWidth = width * 0.72f
        val boxHeight = boxWidth * 0.60f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2.3f

        // Draw outside transparent dim overlay
        // 1. Top block
        drawRect(color = Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, 0f), size = Size(width, top))
        // 2. Left block
        drawRect(color = Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, top), size = Size(left, boxHeight))
        // 3. Right block
        drawRect(color = Color.Black.copy(alpha = 0.6f), topLeft = Offset(left + boxWidth, top), size = Size(width - (left + boxWidth), boxHeight))
        // 4. Bottom block
        drawRect(color = Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, top + boxHeight), size = Size(width, height - (top + boxHeight)))

        // Draw camera brackets corners
        val lineLen = 28.dp.toPx()
        val thickness = 4.dp.toPx()
        val cornerRad = CornerRadius(8.dp.toPx(), 8.dp.toPx())

        // Top Left
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left - thickness / 2, top - thickness / 2),
            size = Size(lineLen, thickness),
            cornerRadius = cornerRad
        )
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left - thickness / 2, top - thickness / 2),
            size = Size(thickness, lineLen),
            cornerRadius = cornerRad
        )

        // Top Right
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left + boxWidth - lineLen + thickness / 2, top - thickness / 2),
            size = Size(lineLen, thickness),
            cornerRadius = cornerRad
        )
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left + boxWidth - thickness / 2, top - thickness / 2),
            size = Size(thickness, lineLen),
            cornerRadius = cornerRad
        )

        // Bottom Left
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left - thickness / 2, top + boxHeight - thickness / 2),
            size = Size(lineLen, thickness),
            cornerRadius = cornerRad
        )
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left - thickness / 2, top + boxHeight - lineLen + thickness / 2),
            size = Size(thickness, lineLen),
            cornerRadius = cornerRad
        )

        // Bottom Right
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left + boxWidth - lineLen + thickness / 2, top + boxHeight - thickness / 2),
            size = Size(lineLen, thickness),
            cornerRadius = cornerRad
        )
        drawRoundRect(
            color = NeonEmerald,
            topLeft = Offset(left + boxWidth - thickness / 2, top + boxHeight - lineLen + thickness / 2),
            size = Size(thickness, lineLen),
            cornerRadius = cornerRad
        )

        // Draw pulsing green laser line
        val laserY = top + (boxHeight * laserOffsetPerc)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonEmerald.copy(alpha = 0.2f),
                    NeonEmerald,
                    NeonEmerald.copy(alpha = 0.2f),
                    Color.Transparent
                )
            ),
            start = Offset(left + 6.dp.toPx(), laserY),
            end = Offset(left + boxWidth - 6.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

@Composable
fun ProductDetailSheet(
    product: ScannedProduct,
    onDismiss: () -> Unit,
    onToggleFavorite: (ScannedProduct) -> Unit,
    onAskAiClicked: (ScannedProduct) -> Unit
) {
    var favoriteScale by remember { mutableStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("product_detail_sheet"),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Draggable bar visual
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(SoftGray.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Meta Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(NeonTeal.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (product.category) {
                                    "Food" -> "مواد غذایی"
                                    "Beverage" -> "نوشیدنی"
                                    "Electronics" -> "دیجیتال"
                                    "Cosmetics" -> "بهداشتی"
                                    "Books" -> "کتاب"
                                    "Clothing" -> "پوشاک"
                                    else -> "متفرقه"
                                },
                                color = NeonTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "کد: " + product.barcode,
                            color = SoftGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = product.name,
                        color = SoftWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (product.brand.isNotBlank() && product.brand != "Unknown") {
                        Text(
                            text = "برند: " + product.brand,
                            color = SoftGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Favorite Button with visual feedback scaling
                val animateScale by animateFloatAsState(
                    targetValue = favoriteScale,
                    finishedListener = { favoriteScale = 1f },
                    label = "favScale"
                )

                IconButton(
                    onClick = {
                        favoriteScale = 1.3f
                        onToggleFavorite(product)
                    },
                    modifier = Modifier.graphicsLayer(scaleX = animateScale, scaleY = animateScale).testTag("favorite_toggle_button")
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "افزودن به علاقه‌مندی‌ها",
                        tint = if (product.isFavorite) Color.Red else SoftGray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = SlateBorder)
            Spacer(modifier = Modifier.height(18.dp))

            // Grid Details (Price, Health, Rating)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Approximate Price Card
                CardItemBadge(
                    modifier = Modifier.weight(1f),
                    title = "قیمت تقریبی",
                    value = product.price.ifBlank() { "مشخص نیست" },
                    iconColor = NeonEmerald,
                    icon = Icons.Default.LocalMall
                )

                // Health Grade Badge
                if (product.healthGrade != "N/A" && product.healthGrade.isNotBlank()) {
                    val gradeColor = when (product.healthGrade.uppercase()) {
                        "A" -> Color(0xFF10B981)
                        "B" -> Color(0xFF34D399)
                        "C" -> Color(0xFFFBBF24)
                        "D" -> Color(0xFFF97316)
                        else -> Color(0xFFEF4444)
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("شاخص سلامت", color = SoftGray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(gradeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    product.healthGrade.uppercase(),
                                    color = SlateDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // AI Rating Card
                CardItemBadge(
                    modifier = Modifier.weight(1f),
                    title = "امتیاز کاربران",
                    value = product.rating + " / ۵",
                    iconColor = NeonAmber,
                    icon = Icons.Default.StarRate
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detailed Specifications (Persian)
            if (product.detailsPersian.isNotBlank()) {
                Text(
                    text = "مشخصات و ترکیبات محصول",
                    color = SoftWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.detailsPersian,
                    color = SoftWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Justify
                )
            }

            // Specs in English
            if (product.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Technical / International Specs",
                    color = SoftGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = product.details,
                        color = SoftGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Left
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ask AI Button
                Button(
                    onClick = { onAskAiClicked(product) },
                    modifier = Modifier.weight(1.2f).height(46.dp).testTag("ask_ai_details_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "تحلیل با هوش مصنوعی",
                        tint = SlateDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "بررسی هوشمند با هوش مصنوعی",
                        color = SlateDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Scan Again Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.8f).height(46.dp).testTag("scan_again_button"),
                    border = BorderStroke(1.dp, NeonEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("اسکن مجدد", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CardItemBadge(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    iconColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier.height(72.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = SoftGray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    color = SoftWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun InteractiveAnalysisLoader() {
    // Elegant loading texts loop cycling
    val texts = listOf(
        "در حال خواندن بارکد کالا...",
        "در حال شناسایی برند محصول با AI...",
        "بارگذاری جزئیات در دیتابیس کالاها...",
        "رایزنی با سرویس هوش مصنوعی گمینی...",
        "در حال ترجمه مشخصات کالا به فارسی کادر..."
    )
    var currentTextIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            currentTextIndex = (currentTextIndex + 1) % texts.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = NeonEmerald,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "درحال تحلیل کالا با Gemini",
                    color = NeonEmerald,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = texts[currentTextIndex],
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
                    },
                    label = "loaderText"
                ) { text ->
                    Text(
                        text = text,
                        color = SoftGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ManualBarcodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "ورود دستی بارکد کالا",
                color = SoftWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "اگر به هر دلیلی دوربین کار نمی‌کند، بارکد عددی (مانند بارکد آزمایشی ۶۲۶۰۱۲۳۴۵۶) را در اینجا وارد کنید تا در پایگاه داده AI جستجو شود:",
                    color = SoftGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("مثلاً: 6260123456789", color = SoftGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = SlateBorder,
                        cursorColor = NeonEmerald
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (rawText.isNotBlank()) {
                                onSubmit(rawText)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_barcode_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        onSubmit(rawText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("submit_manual_barcode_button")
            ) {
                Text("تایید و جستجو", color = SlateDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = SoftGray)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

// ======================== TABS 2: HISTORY ========================

@Composable
fun HistoryTabScreen(
    historyList: List<ScannedProduct>,
    onSearchQueryChanged: (String) -> Unit,
    onProductClicked: (ScannedProduct) -> Unit,
    onToggleFavorite: (ScannedProduct) -> Unit,
    onDeleteProduct: (ScannedProduct) -> Unit,
    onClearAll: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var filterFavoriteOnly by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val displayedList = remember(historyList, filterFavoriteOnly) {
        if (filterFavoriteOnly) {
            historyList.filter { it.isFavorite }
        } else {
            historyList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tab Header Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "تاریخچه اسکن کالاها",
                    color = SoftWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "کالاهای گذشته ثبت‌شده با هوش مصنوعی",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            if (historyList.isNotEmpty()) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                        .size(36.dp).testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "پاک کردن کل لیست",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Search Bar & Filters row
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearchQueryChanged(it)
            },
            placeholder = { Text("جستجو در بین کالاها، برند، مشخصات...", color = SoftGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftGray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onSearchQueryChanged("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "پاک کردن", tint = SoftGray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite,
                focusedBorderColor = NeonTeal,
                unfocusedBorderColor = SlateBorder,
                cursorColor = NeonTeal
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("history_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle Favorites Only Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            FilterChip(
                selected = !filterFavoriteOnly,
                onClick = { filterFavoriteOnly = false },
                label = { Text("همه کالاها") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonTeal,
                    selectedLabelColor = SlateDark
                ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = !filterFavoriteOnly)
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
                selected = filterFavoriteOnly,
                onClick = { filterFavoriteOnly = true },
                label = { Text("نشان شده‌ها") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (filterFavoriteOnly) SlateDark else Color.Red
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonAmber,
                    selectedLabelColor = SlateDark
                ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterFavoriteOnly),
                modifier = Modifier.testTag("filter_favorites_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // List View
        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (filterFavoriteOnly) Icons.Default.FavoriteBorder else Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint = SoftGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (filterFavoriteOnly) "هیچ کالای نشان‌شده‌ای وجود ندارد." else "هنوز هیچ محصولی اسکن نکرده‌اید!",
                        color = SoftGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_items_list")
            ) {
                items(displayedList) { product ->
                    HistoryItemCard(
                        product = product,
                        onClick = { onProductClicked(product) },
                        onToggleFavorite = { onToggleFavorite(product) },
                        onDelete = { onDeleteProduct(product) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    product: ScannedProduct,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val categoryIcon = when (product.category) {
        "Food" -> Icons.Default.Restaurant
        "Beverage" -> Icons.Default.LocalBar
        "Electronics" -> Icons.Default.Computer
        "Cosmetics" -> Icons.Default.ContentCut
        "Books" -> Icons.Default.Book
        "Clothing" -> Icons.Default.Checkroom
        else -> Icons.Default.Inventory
    }

    val displayDate = remember(product.timestamp) {
        try {
            val df = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            df.format(Date(product.timestamp))
        } catch (e: Exception) {
            "تاریخ مجهول"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_" + product.barcode),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category circular icon
                Box(
                    modifier = Modifier
                        .background(NeonTeal.copy(alpha = 0.15f), CircleShape)
                        .size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = product.category,
                        tint = NeonTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = product.name,
                        color = SoftWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.brand.ifBlank { "برند متفرقه" },
                            color = SoftGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(3.dp).background(SoftGray, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayDate,
                            color = SoftGray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "بارکد: ${product.barcode}",
                        color = SoftGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                    )
                }
            }

            // Quick Actions icons (Favorite & Trash)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "موردعلاقه",
                        tint = if (product.isFavorite) Color.Red else SoftGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف رکورد",
                        tint = SoftGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ======================== TABS 3: AI CHAT ASSISTANT ========================

@Composable
fun AiAssistantTabScreen(
    chatHistory: List<ChatMessage>,
    isChatLoading: Boolean,
    onSendMessage: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Auto scroll down when new chat bubble arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val suggestionPrompts = listOf(
        "شاخص تغذیه‌ای درجه A چیه؟",
        "آیا نوشابه گازدار Zamzam سالمه؟",
        "کدام کدهای بارکد معتبر هستند؟"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Chat Header Name
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                "دستیار خریدار هوشمند",
                color = SoftWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "دستیار گویای متصل به هوش مصنوعی خانواده گمینی",
                color = SoftGray,
                fontSize = 11.sp
            )
        }

        // Suggestions chips row (Only show if chat catalog is tiny)
        if (chatHistory.size <= 1) {
            Text("سوال‌های نمونه:", color = SoftGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestionPrompts.forEach { promptText ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                onSendMessage(promptText)
                            },
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = promptText,
                            color = NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Messages Flow panel
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat_messages_flow")
        ) {
            items(chatHistory) { message ->
                ChatBubble(message = message)
            }

            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 60.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NeonEmerald,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "دستیار در حال تایپ است...",
                                    color = SoftGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text("سوال یا ویژگی محصول را بنویسید...", color = SoftGray) },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState)
                            textState = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    focusedBorderColor = NeonEmerald,
                    unfocusedBorderColor = SlateBorder,
                    cursorColor = NeonEmerald
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .background(NeonEmerald, CircleShape)
                    .size(44.dp).testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "ارسال پیام",
                    tint = SlateDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"

    val displayTime = remember(message.time) {
        val df = SimpleDateFormat("HH:mm", Locale.getDefault())
        df.format(Date(message.time))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) NeonTeal.copy(alpha = 0.25f) else SlateCard
                ),
                border = BorderStroke(1.dp, if (isUser) NeonTeal.copy(alpha = 0.4f) else SlateBorder),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 0.dp,
                    bottomEnd = if (isUser) 0.dp else 16.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        color = SoftWhite,
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        textAlign = if (isUser) TextAlign.Right else TextAlign.Justify
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayTime,
                        color = SoftGray,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
