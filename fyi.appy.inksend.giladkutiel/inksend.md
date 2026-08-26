# Build Specification: Text-to-Image Overlay for Android (Kotlin)

## 1. Project Overview & Architecture

### 1.1 Summary
This specification defines the architecture, design, and implementation details for a native Android application built in Kotlin. The app runs a background `AccessibilityService` combined with a floating `System Alert Window` overlay (`TYPE_APPLICATION_OVERLAY`). 

Whenever a user types text into an input field (such as WhatsApp, Messages, or any third-party app):
1. The `AccessibilityService` reads the active field's text content in real time.
2. If the text length falls within user-defined bounds (minimum/maximum length), a floating overlay button appears unobtrusively near or above the soft keyboard.
3. Tapping the overlay triggers an immediate workflow:
   - Renders the typed text into a styled image (`Bitmap`) based on customizable design templates (fonts, background colors, gradients, padding, text alignment).
   - Writes the rendered image file URI directly to the Android System **Clipboard** (`ClipData` with `image/png` MIME type).
   - Clears the original text field in the targeted application using `ACTION_SET_TEXT`.
   - Displays a floating preview toast or system hint so the user can immediately tap **Paste** (or select the clipboard suggestion chip on their soft keyboard).
4. The main application provides a modern Jetpack Compose UI for configuring design templates, text length constraints, and enabling required system permissions.

### 1.2 Target Platform & Technical Stack
- **Language:** Kotlin 2.0+
- **Min SDK:** API Level 26 (Android 8.0 Oreo)
- **Target SDK:** API Level 34+ (Android 14)
- **UI Framework (Main App):** Jetpack Compose with Material 3
- **Dependency Injection:** Hilt / Dagger
- **Data Storage:** DataStore (Preferences) for settings persistence
- **Concurrency:** Kotlin Coroutines & Flow
- **Image Processing:** Native Android `Canvas`, `StaticLayout`, `Paint`, and `FileProvider`

---

## 2. Permissions & System Services

The application relies on two high-privilege Android system permissions. The main app UI must guide the user through granting both before activating the service.

### 2.1 Manifest Declarations (`AndroidManifest.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.texttoimageoverlay">

    <!-- Permission to draw floating overlay UI -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    
    <!-- Permission to read/write external app cache for file sharing -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:name=".TextToImageApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.TextToImageOverlay">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.TextToImageOverlay">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Accessibility Service Configuration -->
        <service
            android:name=".service.TextMonitorAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- FileProvider for Sharing Clipboard Image URIs -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

### 2.2 Accessibility Configuration (`xml/accessibility_service_config.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewTextChanged|typeViewFocused"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

### 2.3 File Provider Paths (`xml/file_paths.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="styled_images" path="images/" />
</paths>
```

---

## 3. App Architecture & Module Breakdown

```
com.example.texttoimageoverlay/
│
├── data/
│   ├── model/
│   │   ├── TextStyleConfig.kt
│   │   ├── ColorPalette.kt
│   │   └── TextLengthBounds.kt
│   └── repository/
│       └── SettingsRepository.kt
│
├── engine/
│   ├── ImageRenderer.kt
│   └── ClipboardManagerHelper.kt
│
├── service/
│   ├── TextMonitorAccessibilityService.kt
│   └── OverlayWindowManager.kt
│
├── ui/
│   ├── MainActivity.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/
│       └── Theme.kt
│
└── TextToImageApp.kt
```

---

## 4. Component Deep Dive & Code Specifications

### 4.1 Data Models & Settings Persistence

#### `TextStyleConfig.kt`
```kotlin
package com.example.texttoimageoverlay.data.model

import androidx.annotation.Keep

@Keep
data class TextStyleConfig(
    val fontFileName: String = "sans-serif", // Options: sans-serif, serif, monospace, custom_script.ttf
    val fontSizeSp: Float = 28f,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#1E1E2E",
    val isGradientEnabled: Boolean = true,
    val gradientEndColorHex: String = "#89B4FA",
    val paddingDp: Int = 32,
    val cornerRadiusDp: Float = 24f,
    val minTextLength: Int = 3,
    val maxTextLength: Int = 280
)
```

#### `SettingsRepository.kt`
Uses AndroidX DataStore for persistent state management.

```kotlin
package com.example.texttoimageoverlay.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.texttoimageoverlay.data.model.TextStyleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsRepository @Inject constructor(private val context: Context) {

    private object PreferencesKeys {
        val FONT_FILE = stringPreferencesKey("font_file")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val TEXT_COLOR = stringPreferencesKey("text_color")
        val BG_COLOR = stringPreferencesKey("bg_color")
        val GRADIENT_ENABLED = booleanPreferencesKey("gradient_enabled")
        val GRADIENT_END_COLOR = stringPreferencesKey("gradient_end_color")
        val PADDING = intPreferencesKey("padding")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val MIN_LENGTH = intPreferencesKey("min_length")
        val MAX_LENGTH = intPreferencesKey("max_length")
    }

    val styleConfigFlow: Flow<TextStyleConfig> = context.dataStore.data.map { prefs ->
        TextStyleConfig(
            fontFileName = prefs[PreferencesKeys.FONT_FILE] ?: "sans-serif",
            fontSizeSp = prefs[PreferencesKeys.FONT_SIZE] ?: 28f,
            textColorHex = prefs[PreferencesKeys.TEXT_COLOR] ?: "#FFFFFF",
            backgroundColorHex = prefs[PreferencesKeys.BG_COLOR] ?: "#1E1E2E",
            isGradientEnabled = prefs[PreferencesKeys.GRADIENT_ENABLED] ?: true,
            gradientEndColorHex = prefs[PreferencesKeys.GRADIENT_END_COLOR] ?: "#89B4FA",
            paddingDp = prefs[PreferencesKeys.PADDING] ?: 32,
            cornerRadiusDp = prefs[PreferencesKeys.CORNER_RADIUS] ?: 24f,
            minTextLength = prefs[PreferencesKeys.MIN_LENGTH] ?: 3,
            maxTextLength = prefs[PreferencesKeys.MAX_LENGTH] ?: 280
        )
    }

    async fun updateConfig(config: TextStyleConfig) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FONT_FILE] = config.fontFileName
            prefs[PreferencesKeys.FONT_SIZE] = config.fontSizeSp
            prefs[PreferencesKeys.TEXT_COLOR] = config.textColorHex
            prefs[PreferencesKeys.BG_COLOR] = config.backgroundColorHex
            prefs[PreferencesKeys.GRADIENT_ENABLED] = config.isGradientEnabled
            prefs[PreferencesKeys.GRADIENT_END_COLOR] = config.gradientEndColorHex
            prefs[PreferencesKeys.PADDING] = config.paddingDp
            prefs[PreferencesKeys.CORNER_RADIUS] = config.cornerRadiusDp
            prefs[PreferencesKeys.MIN_LENGTH] = config.minTextLength
            prefs[PreferencesKeys.MAX_LENGTH] = config.maxTextLength
        }
    }
}
```

---

### 4.2 Image Rendering Engine (`ImageRenderer.kt`)

Converts raw text strings into clean styled PNG image bitmaps using native `Canvas` and `StaticLayout`.

```kotlin
package com.example.texttoimageoverlay.engine

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.texttoimageoverlay.data.model.TextStyleConfig
import java.io.File
import java.io.FileOutputStream

object ImageRenderer {

    fun generateStyledImageUri(context: Context, text: String, config: TextStyleConfig): Uri {
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt()
        val cornerRadiusPx = config.cornerRadiusDp * density

        // Configure TextPaint
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.textColorHex)
            textSize = config.fontSizeSp * density
            typeface = Typeface.create(config.fontFileName, Typeface.BOLD)
        }

        val maxCanvasWidth = (800 * density).toInt()
        val contentWidth = maxCanvasWidth - (paddingPx * 2)

        // Build StaticLayout for multi-line text measurement
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)

        val staticLayout = builder.build()
        val textWidth = (0 until staticLayout.lineCount).maxOfOrNull { staticLayout.getLineWidth(it) }?.toInt() ?: contentWidth
        
        val finalWidth = (textWidth + (paddingPx * 2)).coerceAtLeast((250 * density).toInt())
        val finalHeight = staticLayout.height + (paddingPx * 2)

        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Render Background (Solid or Gradient)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (config.isGradientEnabled) {
                shader = LinearGradient(
                    0f, 0f, finalWidth.toFloat(), finalHeight.toFloat(),
                    Color.parseColor(config.backgroundColorHex),
                    Color.parseColor(config.gradientEndColorHex),
                    Shader.TileMode.CLAMP
                )
            } else {
                color = Color.parseColor(config.backgroundColorHex)
            }
        }

        val rect = RectF(0f, 0f, finalWidth.toFloat(), finalHeight.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)

        // Draw Text centered inside padding
        canvas.save()
        val xOffset = (finalWidth - contentWidth) / 2f
        canvas.translate(xOffset, paddingPx.toFloat())
        staticLayout.draw(canvas)
        canvas.restore()

        // Cache file write
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File(imagesDir, "styled_text_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
}
```

---

### 4.3 Clipboard Helper (`ClipboardManagerHelper.kt`)

Handles writing image URIs to the System Clipboard with proper `ClipData` MIME declarations.

```kotlin
package com.example.texttoimageoverlay.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

object ClipboardManagerHelper {

    fun copyImageToClipboard(context: Context, imageUri: Uri) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newUri(
            context.contentResolver,
            "Styled Text Image",
            imageUri
        )
        clipboard.setPrimaryClip(clipData)
    }
}
```

---

### 4.4 Overlay Window Manager (`OverlayWindowManager.kt`)

Manages inflating, showing, positioning, and destroying the floating system window (`WindowManager`).

```kotlin
package com.example.texttoimageoverlay.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.texttoimageoverlay.R

class OverlayWindowManager(
    private val context: Context,
    private val onOverlayClicked: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isShowing = false

    @SuppressLint("InflateParams")
    fun showOverlay() {
        if (isShowing) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 48 // margin end px
            y = 220 // elevated above soft keyboard
        }

        val container = FrameLayout(context).apply {
            val button = ImageView(context).apply {
                setImageResource(R.drawable.ic_style_convert)
                setBackgroundResource(R.drawable.bg_overlay_button)
                setPadding(24, 24, 24, 24)
                setOnClickListener { onOverlayClicked() }
            }
            addView(button)
        }

        overlayView = container
        windowManager.addView(overlayView, params)
        isShowing = true
    }

    fun hideOverlay() {
        if (!isShowing || overlayView == null) return
        try {
            windowManager.removeView(overlayView)
        } catch (_: Exception) {}
        overlayView = null
        isShowing = false
    }
}
```

---

### 4.5 Accessibility Service (`TextMonitorAccessibilityService.kt`)

Coordinates reading text from active fields, checking bounds, rendering images, clearing input nodes, and writing to the clipboard.

```kotlin
package com.example.texttoimageoverlay.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.texttoimageoverlay.data.model.TextStyleConfig
import com.example.texttoimageoverlay.data.repository.SettingsRepository
import com.example.texttoimageoverlay.engine.ClipboardManagerHelper
import com.example.texttoimageoverlay.engine.ImageRenderer
import kotlinx.coroutines.*
import javax.inject.Inject

class TextMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var overlayManager: OverlayWindowManager? = null
    
    private var currentText: String = ""
    private var activeNode: AccessibilityNodeInfo? = null
    
    private var currentConfig = TextStyleConfig()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayWindowManager(this) {
            handleOverlayPressed()
        }
        
        // Collect current settings
        serviceScope.launch {
            settingsRepository.styleConfigFlow.collect { config ->
                currentConfig = config
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val node = event.source ?: return
                if (node.isEditable) {
                    activeNode = node
                    val text = node.text?.toString() ?: ""
                    currentText = text
                    evaluateOverlayVisibility(text)
                }
            }
        }
    }

    private fun evaluateOverlayVisibility(text: String) {
        val len = text.trim().length
        if (len >= currentConfig.minTextLength && len <= currentConfig.maxTextLength) {
            overlayManager?.showOverlay()
        } else {
            overlayManager?.hideOverlay()
        }
    }

    private fun handleOverlayPressed() {
        if (currentText.isBlank()) return

        // 1. Generate Styled Image
        val imageUri = ImageRenderer.generateStyledImageUri(this, currentText, currentConfig)

        // 2. Put Image onto Clipboard
        ClipboardManagerHelper.copyImageToClipboard(this, imageUri)

        // 3. Clear target text field
        activeNode?.let { node ->
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // 4. Hide Overlay & Notify User
        overlayManager?.hideOverlay()
        Toast.makeText(this, "Styled Image copied! Press Paste.", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {
        overlayManager?.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayManager?.hideOverlay()
    }
}
```

---

### 4.6 Main Settings UI (Jetpack Compose)

Provides control over text bounds, font selections, previewing the output image, and setting up permissions.

#### `SettingsViewModel.kt`
```kotlin
package com.example.texttoimageoverlay.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texttoimageoverlay.data.model.TextStyleConfig
import com.example.texttoimageoverlay.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<TextStyleConfig> = repository.styleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TextStyleConfig())

    fun updateConfig(newConfig: TextStyleConfig) {
        viewModelScope.launch {
            repository.updateConfig(newConfig)
        }
    }
}
```

#### `SettingsScreen.kt`
```kotlin
package com.example.texttoimageoverlay.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.texttoimageoverlay.data.model.TextStyleConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
Composable fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val config by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Text-to-Image Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Status Cards
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Required System Permissions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("1. Grant Overlay Permission")
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("2. Enable Accessibility Service")
                    }
                }
            }

            // Text Length Constraints
            Text("Text Length Triggers", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = config.minTextLength.toString(),
                    onValueChange = { val valInt = it.toIntOrNull() ?: 1; viewModel.updateConfig(config.copy(minTextLength = valInt)) },
                    label = { Text("Min Length") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = config.maxTextLength.toString(),
                    onValueChange = { val valInt = it.toIntOrNull() ?: 280; viewModel.updateConfig(config.copy(maxTextLength = valInt)) },
                    label = { Text("Max Length") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Design Controls
            Text("Styling & Colors", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = config.backgroundColorHex,
                onValueChange = { viewModel.updateConfig(config.copy(backgroundColorHex = it)) },
                label = { Text("Background Color (Hex)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.textColorHex,
                onValueChange = { viewModel.updateConfig(config.copy(textColorHex = it)) },
                label = { Text("Text Color (Hex)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Gradient Background")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = config.isGradientEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(isGradientEnabled = it)) }
                )
            }

            if (config.isGradientEnabled) {
                OutlinedTextField(
                    value = config.gradientEndColorHex,
                    onValueChange = { viewModel.updateConfig(config.copy(gradientEndColorHex = it)) },
                    label = { Text("Gradient End Color (Hex)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

---

## 5. End-to-End User Experience Flow

```
[ User types text in WhatsApp / App Input ]
                       │
                       ▼
[ AccessibilityService detects TYPE_VIEW_TEXT_CHANGED ]
                       │
                       ▼
[ Text length between Min and Max settings? ]
        ├── NO  ► Hide Floating Overlay
        └── YES ► Show Floating Overlay above soft keyboard
                       │
                       ▼
[ User presses Floating Overlay Button ]
                       │
                       ▼
   ┌───────────────────┴───────────────────┐
   │ 1. Render styled Bitmap via Canvas    │
   │ 2. Save PNG to cache FileProvider     │
   │ 3. Push Image Uri to Clipboard        │
   │ 4. Clear input via ACTION_SET_TEXT    │
   └───────────────────┬───────────────────┘
                       │
                       ▼
[ User long-presses input field & taps Paste (or keyboard chip) ]
```

---

## 6. Testing & Validation Checklist

1. **Permission Handling:** Verify that the app handles cases where either System Overlay or Accessibility permissions are revoked dynamically.
2. **Clipboard Paste Integrity:** Test pasting into WhatsApp, Telegram, and standard Android `EditText` controls to ensure the `ClipData` URI format is accepted.
3. **Multi-line Wrapping:** Confirm that long text strings correctly wrap into multiple lines without overflowing the rendered bitmap boundaries.
4. **Memory Leak Prevention:** Verify that the overlay view is removed cleanly from `WindowManager` when the text field loses focus or clears.
