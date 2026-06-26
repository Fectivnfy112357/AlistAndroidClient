# HyperOS UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Alist Android client UI into a HyperOS-inspired, high-density, edge-to-edge cloud drive interface while preserving existing business behavior.

**Architecture:** Add a focused Compose design-system layer under `ui/theme` and `ui/components`, then migrate screens to compose those components instead of styling each screen independently. Navigation keeps the current route graph but replaces the default Material bottom navigation with a custom safe-area-aware floating capsule bar.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, AndroidX Activity `enableEdgeToEdge`, Compose `WindowInsets`, Hilt, Navigation Compose, existing ViewModel/Repository layers.

## Global Constraints

- Target visual baseline: Redmi K90 Pro Max and other modern full-screen Android phones.
- Visual direction: login page uses minimal HyperOS system style; main app pages use high-density HyperOS cloud-drive style.
- Do not add new business features, fake storage capacity, fake recent files, AI search, multi-account, admin panel, or external-storage management.
- Do not rewrite ViewModel, Repository, network, transfer, or preview business logic.
- Preserve existing important user-facing copy where tests depend on it, especially `登录 Alist` and `暂无传输任务`.
- Preserve test tags: `upload_button`, `download_button`, and `share_button`.
- Use current Compose guidance: handle edge-to-edge layouts with `WindowInsets`, safe drawing/system bar padding, and semantics/test tags for UI tests; AndroidX docs expose `WindowInsets.systemBars`, `statusBars`, and navigation-bar related insets for system UI avoidance.
- Keep files focused; design-system primitives belong in `ui/components`, palette/typography/shape in `ui/theme`, and screen files should express business screen composition.

---

## File Structure

### New files

- `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt`  
  Defines HyperOS rounded-corner constants used by components.

- `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt`  
  Defines app typography with compact Chinese-friendly hierarchy.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudScaffold.kt`  
  Owns app background, safe-area padding, optional top content, optional bottom content, and page body layout.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudTopBar.kt`  
  Compact title/subtitle/action bar shared by files, transfers, settings, previews, and picker screens.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt`  
  Floating capsule navigation bar for files/transfers/settings.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudSearchBar.kt`  
  Filled compact search field for file search.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudCard.kt`  
  Small reusable card surface and grouped-list container.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudListItem.kt`  
  Compact high-density list row for files, directories, settings, and transfer rows.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudEmptyState.kt`  
  Shared empty/error/loading states.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudStatusBanner.kt`  
  Shared warning/error/info banners.

- `app/src/main/java/com/textvision/alistclient/ui/components/CloudActionButton.kt`  
  Compact round and pill action buttons.

### Modified files

- `app/src/main/java/com/textvision/alistclient/MainActivity.kt`  
  Enable edge-to-edge before `setContent`.

- `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`  
  Replace default `Scaffold`/`NavigationBar` with layout that uses `CloudBottomBar` and passes bottom content padding to `NavHost`.

- `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt`  
  Expand the palette for HyperOS background, cards, text, warnings, and file type chips.

- `app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt`  
  Use expanded color scheme, typography, and shapes.

- `app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt`  
  Add colored rounded tile variant while keeping the existing composable name usable.

- `app/src/main/java/com/textvision/alistclient/ui/components/TransferProgress.kt`  
  Style progress bars with the new palette and rounded track.

- `app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt`  
  Reuse high-density directory list rows and safe-area layout for target selection.

- `app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt`  
  Convert to minimal HyperOS login page.

- `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`  
  Convert to high-density cloud file list while preserving file operations.

- `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`  
  Convert to compact transfer list with styled progress/empty states.

- `app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt`  
  Convert to grouped system settings style.

- `app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt`  
  Wrap preview UI with shared top bar/cards/safe-area behavior.

- `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`  
  Extend launch/navigation smoke checks for the redesigned UI.

---

### Task 1: Theme Palette, Typography, Shapes, and Edge-to-Edge Host

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/MainActivity.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: Existing `AlistClientTheme(content: @Composable () -> Unit)` call in `MainActivity`.
- Produces:
  - Theme constants: `CloudBackground`, `CloudSurface`, `CloudSurfaceStrong`, `CloudPrimary`, `CloudPrimarySoft`, `CloudTextPrimary`, `CloudTextSecondary`, `CloudWarningContainer`, `CloudWarningText`, `CloudErrorContainer`, `FolderTint`, `ImageTint`, `TextTint`, `GenericFileTint`.
  - Shape constants object: `CloudShapes` with `Card`, `Panel`, `Control`, `IconTile`, `BottomBar`.
  - Typography value: `CloudTypography`.
  - `MainActivity` calls `enableEdgeToEdge()` before `setContent`.

- [ ] **Step 1: Extend the launch test so it still protects the login entry point**

Replace `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt` with:

```kotlin
package com.textvision.alistclient

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesToLoginScreen() {
        composeRule.onNodeWithText("登录 Alist").assertIsDisplayed()
        composeRule.onNodeWithText("Alist Cloud").assertIsDisplayed()
        composeRule.onNodeWithText("服务器地址").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the launch test and verify the new branding assertion fails before implementation**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest
```

Expected before implementation: connected test runs if a device/emulator is attached, and `Alist Cloud` assertion fails because the current login page only shows `登录 Alist`. If no device is attached, record that connected tests could not run and continue with unit compile checks in later steps.

- [ ] **Step 3: Replace the color palette**

Replace `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt` with:

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.ui.graphics.Color

val CloudBackground = Color(0xFFF7F5FA)
val CloudBackgroundDeep = Color(0xFFF0EDF6)
val CloudSurface = Color(0xFFFFFFFF)
val CloudSurfaceMuted = Color(0xFFF4F2F8)
val CloudSurfaceStrong = Color(0xFFEDE9F6)
val CloudOutline = Color(0x1F222230)

val CloudPrimary = Color(0xFF5164F6)
val CloudPrimaryDark = Color(0xFF2532A8)
val CloudPrimarySoft = Color(0xFFECE8FF)
val CloudOnPrimary = Color(0xFFFFFFFF)

val CloudTextPrimary = Color(0xFF20212B)
val CloudTextSecondary = Color(0xFF777482)
val CloudTextTertiary = Color(0xFF9B98A4)

val CloudWarningContainer = Color(0xFFFFF4DF)
val CloudWarningText = Color(0xFF8A520D)
val CloudErrorContainer = Color(0xFFFFECEF)
val CloudErrorText = Color(0xFFB4233B)
val CloudSuccessContainer = Color(0xFFEAF8EF)
val CloudSuccessText = Color(0xFF247A3D)

val FolderTint = Color(0xFFEDE8FF)
val FolderIconTint = Color(0xFF2E254A)
val ImageTint = Color(0xFFFFEFEB)
val ImageIconTint = Color(0xFFFF745D)
val TextTint = Color(0xFFE8F8FF)
val TextIconTint = Color(0xFF149BC4)
val GenericFileTint = Color(0xFFEFF2F8)
val GenericFileIconTint = Color(0xFF667085)

// Kept for compatibility with existing code and tests that import AlistBlue.
val AlistBlue = CloudPrimary
```

- [ ] **Step 4: Add shared shape constants**

Create `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt`:

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object CloudShapes {
    val Card = RoundedCornerShape(24.dp)
    val Panel = RoundedCornerShape(28.dp)
    val Control = RoundedCornerShape(18.dp)
    val IconTile = RoundedCornerShape(15.dp)
    val BottomBar = RoundedCornerShape(26.dp)
    val Pill = RoundedCornerShape(999.dp)
}
```

- [ ] **Step 5: Add compact typography**

Create `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt`:

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CloudTypography = Typography().copy(
    headlineLarge = Typography().headlineLarge.copy(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontSize = 21.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = Typography().titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = Typography().labelLarge.copy(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    ),
    labelMedium = Typography().labelMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)
```

- [ ] **Step 6: Update the Material theme**

Replace `app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt` with:

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CloudPrimary,
    onPrimary = CloudOnPrimary,
    primaryContainer = CloudPrimarySoft,
    onPrimaryContainer = CloudPrimaryDark,
    background = CloudBackground,
    onBackground = CloudTextPrimary,
    surface = CloudSurface,
    onSurface = CloudTextPrimary,
    surfaceVariant = CloudSurfaceMuted,
    onSurfaceVariant = CloudTextSecondary,
    outline = CloudOutline,
    error = CloudErrorText,
    errorContainer = CloudErrorContainer,
)

@Composable
fun AlistClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CloudTypography,
        content = content,
    )
}
```

- [ ] **Step 7: Enable edge-to-edge in the activity**

Modify `app/src/main/java/com/textvision/alistclient/MainActivity.kt` so the imports and `onCreate` include `enableEdgeToEdge()`:

```kotlin
package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.TransferNotificationController
import com.textvision.alistclient.ui.theme.AlistClientTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var transferManager: TransferManager
    @Inject lateinit var notificationController: TransferNotificationController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationController.ensureChannels()
        transferManager.initialize()
        setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = false)
            }
        }
    }
}
```

- [ ] **Step 8: Run compile/unit checks**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Run the launch test if a device/emulator is available**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest
```

Expected after Task 3 is implemented later: this may still fail until the login page is redesigned in Task 3. For this task, record the current failure if `Alist Cloud` is still absent, or record that no device/emulator was attached.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt \
        app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt \
        app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt \
        app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt \
        app/src/main/java/com/textvision/alistclient/MainActivity.kt \
        app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: add hyperos theme foundation"
```

---

### Task 2: Shared Cloud UI Components and Floating Bottom Navigation

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudScaffold.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudTopBar.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudSearchBar.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudCard.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudListItem.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudEmptyState.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudStatusBanner.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/CloudActionButton.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: Theme constants from Task 1.
- Produces:
  - `CloudScaffold(showBottomPadding: Boolean = false, bottomBar: @Composable BoxScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit)`.
  - `CloudTopBar(title: String, subtitle: String? = null, action: @Composable RowScope.() -> Unit = {})`.
  - `CloudBottomBar(currentRoute: String?, onNavigate: (String) -> Unit)`.
  - `CloudSearchBar(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier)`.
  - `CloudCard`, `CloudListItem`, `CloudEmptyState`, `CloudStatusBanner`, `CloudRoundIconButton`, `CloudPillButton`.

- [ ] **Step 1: Extend navigation smoke test for bottom tabs**

Update `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt` to include a second test that starts the authenticated route by setting content directly. Keep the existing launch test.

```kotlin
package com.textvision.alistclient

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.ui.theme.AlistClientTheme
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesToLoginScreen() {
        composeRule.onNodeWithText("登录 Alist").assertIsDisplayed()
        composeRule.onNodeWithText("Alist Cloud").assertIsDisplayed()
        composeRule.onNodeWithText("服务器地址").assertIsDisplayed()
    }

    @Test
    fun authenticatedShellShowsBottomNavigation() {
        composeRule.setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = true)
            }
        }

        composeRule.onNodeWithText("文件").assertIsDisplayed()
        composeRule.onNodeWithText("传输").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the focused connected test and verify the bottom nav still uses old/default UI**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellShowsBottomNavigation
```

Expected before implementation: the test can pass because old navigation labels exist, but visual code still uses default `NavigationBar`; proceed because this task is a safe refactor plus visual replacement. If no device is attached, record that connected tests could not run.

- [ ] **Step 3: Create `CloudCard.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudCard.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurface

@Composable
fun CloudCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CloudShapes.Card)
            .background(CloudSurface)
            .padding(contentPadding),
        content = content,
    )
}
```

- [ ] **Step 4: Create `CloudActionButton.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudActionButton.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudOnPrimary
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes

@Composable
fun CloudRoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = CloudPrimarySoft,
    contentColor: Color = CloudPrimary,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CloudShapes.Control)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = contentColor)
    }
}

@Composable
fun CloudPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = CloudPrimary,
    contentColor: Color = CloudOnPrimary,
    leading: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(CloudShapes.Control)
            .background(if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

- [ ] **Step 5: Create `CloudStatusBanner.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudStatusBanner.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudErrorContainer
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudWarningContainer
import com.textvision.alistclient.ui.theme.CloudWarningText

enum class CloudBannerKind { Info, Warning, Error }

@Composable
fun CloudStatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    kind: CloudBannerKind = CloudBannerKind.Info,
) {
    val colors = when (kind) {
        CloudBannerKind.Info -> BannerColors(CloudPrimarySoft, CloudPrimary, Icons.Outlined.Info)
        CloudBannerKind.Warning -> BannerColors(CloudWarningContainer, CloudWarningText, Icons.Outlined.WarningAmber)
        CloudBannerKind.Error -> BannerColors(CloudErrorContainer, CloudErrorText, Icons.Outlined.ErrorOutline)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CloudShapes.Control)
            .background(colors.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(colors.icon, contentDescription = null, tint = colors.content)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = colors.content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class BannerColors(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
)
```

- [ ] **Step 6: Create `CloudEmptyState.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudEmptyState.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun CloudEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        message?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 6.dp),
                color = CloudTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        action?.let {
            Column(Modifier.padding(top = 16.dp)) { it() }
        }
    }
}

@Composable
fun CloudLoadingState(text: String = "加载中") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
            modifier = Modifier.padding(top = 12.dp),
            color = CloudTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
```

- [ ] **Step 7: Create `CloudListItem.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudListItem.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun CloudListItem(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CloudShapes.Control)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            Box(
                modifier = Modifier
                    .padding(end = 11.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                it()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 2.dp),
                    color = CloudTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}
```

- [ ] **Step 8: Create `CloudSearchBar.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudSearchBar.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurfaceMuted
import com.textvision.alistclient.ui.theme.CloudTextTertiary

@Composable
fun CloudSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CloudShapes.Control)
            .background(CloudSurfaceMuted),
        singleLine = true,
        placeholder = { Text(placeholder, color = CloudTextTertiary) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = CloudTextTertiary) },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CloudSurfaceMuted,
            unfocusedContainerColor = CloudSurfaceMuted,
            disabledContainerColor = CloudSurfaceMuted,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}
```

- [ ] **Step 9: Create `CloudTopBar.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudTopBar.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun CloudTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 3.dp),
                    color = CloudTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = action)
    }
}
```

- [ ] **Step 10: Create `CloudBottomBar.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.navigation.AppRoute
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun CloudBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(CloudShapes.BottomBar)
            .background(CloudSurface)
            .height(64.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Files.route,
            icon = Icons.Default.Folder,
            label = "文件",
            onClick = { onNavigate(AppRoute.Files.route) },
        )
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Transfers.route,
            icon = Icons.Default.SyncAlt,
            label = "传输",
            onClick = { onNavigate(AppRoute.Transfers.route) },
        )
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Settings.route,
            icon = Icons.Default.Settings,
            label = "设置",
            onClick = { onNavigate(AppRoute.Settings.route) },
        )
    }
}

@Composable
private fun CloudBottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(CloudShapes.Control)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(CloudShapes.Pill)
                .background(if (selected) CloudPrimarySoft else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) CloudPrimary else CloudTextSecondary,
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 3.dp),
            color = if (selected) CloudTextPrimary else CloudTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
```

- [ ] **Step 11: Create `CloudScaffold.kt`**

Create `app/src/main/java/com/textvision/alistclient/ui/components/CloudScaffold.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudBackground

@Composable
fun CloudScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
    showBottomPadding: Boolean = false,
    bottomBar: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CloudBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(contentPadding)
                .padding(bottom = if (showBottomPadding) 92.dp else 0.dp),
            content = content,
        )
        bottomBar()
    }
}
```

- [ ] **Step 12: Replace AppNavHost bottom navigation**

Replace `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt` with:

```kotlin
package com.textvision.alistclient.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.textvision.alistclient.ui.components.CloudBottomBar
import com.textvision.alistclient.ui.screens.FileScreen
import com.textvision.alistclient.ui.screens.LoginScreen
import com.textvision.alistclient.ui.screens.MoveCopyTargetPickerScreen
import com.textvision.alistclient.ui.screens.PreviewScreen
import com.textvision.alistclient.ui.screens.SettingsScreen
import com.textvision.alistclient.ui.screens.TransferScreen

@Composable
fun AppNavHost(startAuthenticated: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startAuthenticated) AppRoute.Files.route else AppRoute.Login.route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in setOf(AppRoute.Files.route, AppRoute.Transfers.route, AppRoute.Settings.route)

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(AppRoute.Login.route) {
                    LoginScreen(onLoginSuccess = {
                        navController.navigate(AppRoute.Files.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                        }
                    })
                }
                composable(AppRoute.Files.route) { FileScreen() }
                composable(AppRoute.Transfers.route) { TransferScreen() }
                composable(AppRoute.Settings.route) {
                    SettingsScreen(
                        onLoggedOut = {
                            navController.navigate(AppRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(AppRoute.MoveCopyPicker.route) {
                    MoveCopyTargetPickerScreen(onTargetSelected = { navController.popBackStack() })
                }
                composable(
                    route = AppRoute.Preview.route,
                    arguments = listOf(navArgument("filePath") { type = NavType.StringType })
                ) { entry ->
                    val encoded = requireNotNull(entry.arguments?.getString("filePath"))
                    val filePath = Uri.decode(encoded)
                    PreviewScreen(
                        filePath = filePath,
                        onDownload = { navController.popBackStack() },
                        onExternalOpen = { navController.popBackStack() },
                    )
                }
            }
            if (showBottomBar) {
                CloudBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) { launchSingleTop = true }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
```

- [ ] **Step 13: Run compile/unit checks**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 14: Run the bottom navigation smoke test if possible**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellShowsBottomNavigation
```

Expected: `BUILD SUCCESSFUL` when a device/emulator is available. If unavailable, record that connected tests could not run.

- [ ] **Step 15: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/CloudScaffold.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudTopBar.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudSearchBar.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudCard.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudListItem.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudEmptyState.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudStatusBanner.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudActionButton.kt \
        app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt \
        app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: add cloud ui components"
```

---

### Task 3: Minimal HyperOS Login Page

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: `CloudScaffold`, `CloudCard`, `CloudPillButton`, `CloudStatusBanner`, `CloudBannerKind` from Task 2; existing `LoginViewModel` and `LoginUiState`.
- Produces: Login screen with visible `Alist Cloud`, `登录 Alist`, `服务器地址`, `用户名`, `密码`, HTTP warning banner, error banner, and unchanged `viewModel.login(onLoginSuccess)` behavior.

- [ ] **Step 1: Confirm the login launch test currently fails if Task 1 did not already make it pass**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#launchesToLoginScreen
```

Expected before this task: if a device/emulator is attached, failure mentions missing `Alist Cloud`. If no device is attached, record that connected tests could not run.

- [ ] **Step 2: Replace LoginScreen with the minimal HyperOS layout**

Replace `app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt` with:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.auth.LoginViewModel
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudPillButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurfaceMuted
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CloudScaffold(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp)) {
        Spacer(Modifier.height(28.dp))
        Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = null,
            tint = CloudPrimary,
            modifier = Modifier
                .size(58.dp)
                .clip(CloudShapes.Panel)
                .background(CloudPrimarySoft)
                .padding(14.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("Alist Cloud", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "连接你的私人网盘，安全访问所有文件",
            modifier = Modifier.padding(top = 8.dp),
            color = CloudTextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(26.dp))
        CloudCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            LoginField(
                value = state.serverUrl,
                onValueChange = viewModel::updateServerUrl,
                label = "服务器地址",
            )
            if (state.showHttpWarning) {
                Spacer(Modifier.height(8.dp))
                CloudStatusBanner(
                    text = "当前使用 HTTP 明文连接，账号密码可能被窃听",
                    kind = CloudBannerKind.Warning,
                )
            }
            Spacer(Modifier.height(10.dp))
            LoginField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = "用户名",
            )
            Spacer(Modifier.height(10.dp))
            LoginField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = "密码",
                password = true,
            )
            state.errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
            }
            Spacer(Modifier.height(14.dp))
            CloudPillButton(
                text = "登录 Alist",
                onClick = { viewModel.login(onLoginSuccess) },
                enabled = !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "凭据将保存在本机加密存储中",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            color = CloudTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = CloudShapes.Control,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CloudSurfaceMuted,
            unfocusedContainerColor = CloudSurfaceMuted,
            disabledContainerColor = CloudSurfaceMuted,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}
```

- [ ] **Step 3: Run unit compile checks**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run login launch instrumentation test if possible**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#launchesToLoginScreen
```

Expected: `BUILD SUCCESSFUL` when a device/emulator is available. If unavailable, record that connected tests could not run.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt \
        app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: redesign login screen"
```

---

### Task 4: High-Density File Page and File Type Tiles

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: `CloudScaffold`, `CloudTopBar`, `CloudRoundIconButton`, `CloudSearchBar`, `CloudCard`, `CloudListItem`, `CloudStatusBanner`, `CloudEmptyState`, `CloudLoadingState`.
- Produces:
  - `FileTypeIcon(type: FileType, modifier: Modifier = Modifier, tiled: Boolean = true)`.
  - High-density file page preserving `upload_button`, `download_button`, `share_button` and existing ViewModel calls.

- [ ] **Step 1: Add an authenticated file screen smoke assertion**

Append this test to `AppLaunchTest` below existing tests:

```kotlin
@Test
fun authenticatedShellShowsFileUploadEntry() {
    composeRule.setContent {
        AlistClientTheme {
            AppNavHost(startAuthenticated = true)
        }
    }

    composeRule.onNodeWithText("我的文件").assertIsDisplayed()
    composeRule.onNodeWithText("搜索").assertIsDisplayed()
}
```

The full imports must still include `AppNavHost` and `AlistClientTheme` from Task 2.

- [ ] **Step 2: Run the new test and verify it fails before the file page title changes**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellShowsFileUploadEntry
```

Expected before implementation: if a device/emulator is attached, failure mentions missing `我的文件`. If no device is attached, record that connected tests could not run.

- [ ] **Step 3: Replace FileTypeIcon with tiled support**

Replace `app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt` with:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.theme.FolderIconTint
import com.textvision.alistclient.ui.theme.FolderTint
import com.textvision.alistclient.ui.theme.GenericFileIconTint
import com.textvision.alistclient.ui.theme.GenericFileTint
import com.textvision.alistclient.ui.theme.ImageIconTint
import com.textvision.alistclient.ui.theme.ImageTint
import com.textvision.alistclient.ui.theme.TextIconTint
import com.textvision.alistclient.ui.theme.TextTint
import com.textvision.alistclient.ui.theme.CloudShapes

@Composable
fun FileTypeIcon(
    type: FileType,
    modifier: Modifier = Modifier,
    tiled: Boolean = true,
) {
    val style = when (type) {
        FileType.Folder -> FileIconStyle(Icons.Default.Folder, FolderTint, FolderIconTint)
        FileType.Image -> FileIconStyle(Icons.Default.Image, ImageTint, ImageIconTint)
        FileType.Text -> FileIconStyle(Icons.Default.Article, TextTint, TextIconTint)
        else -> FileIconStyle(Icons.Default.InsertDriveFile, GenericFileTint, GenericFileIconTint)
    }
    if (tiled) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(CloudShapes.IconTile)
                .background(style.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(style.icon, contentDescription = null, tint = style.content)
        }
    } else {
        Icon(style.icon, contentDescription = null, tint = style.content, modifier = modifier)
    }
}

private data class FileIconStyle(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)
```

- [ ] **Step 4: Replace FileScreen with the high-density layout**

Replace `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt` with:

```kotlin
package com.textvision.alistclient.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.common.error.ErrorMessageMapper
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudLoadingState
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudSearchBar
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.FileTypeIcon
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun FileScreen(viewModel: FileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load("/") }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.enqueueUpload(uri)
    }

    val currentPath = when (val s = state) {
        is FileUiState.Loading -> s.path
        is FileUiState.Success -> s.path
        is FileUiState.Error -> s.path
    }

    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(
            title = "我的文件",
            subtitle = currentPath,
            action = {
                CloudRoundIconButton(
                    icon = Icons.Default.UploadFile,
                    contentDescription = "上传",
                    onClick = { uploadLauncher.launch("*/*") },
                    modifier = Modifier.testTag("upload_button"),
                )
            },
        )
        CloudSearchBar(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = "搜索",
        )
        Spacer(Modifier.height(10.dp))
        if (!isOnline) {
            CloudStatusBanner(text = "当前无网络", kind = CloudBannerKind.Warning)
            Spacer(Modifier.height(10.dp))
        }
        when (val s = state) {
            is FileUiState.Loading -> CloudCard { CloudLoadingState() }
            is FileUiState.Error -> CloudCard {
                CloudEmptyState(
                    title = ErrorMessageMapper.toUserMessage(s.error),
                    action = {
                        androidx.compose.material3.TextButton(onClick = viewModel::refresh) {
                            Text("重试")
                        }
                    },
                )
            }
            is FileUiState.Success -> {
                if (s.isCurrentDirectoryFilter) {
                    CloudStatusBanner(text = "当前目录搜索结果")
                    Spacer(Modifier.height(10.dp))
                }
                if (s.items.isEmpty()) {
                    CloudCard {
                        CloudEmptyState(
                            title = "这里还没有文件",
                            message = "可以通过右上角上传文件",
                        )
                    }
                } else {
                    CloudCard {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            items(s.items, key = { it.path }) { item ->
                                FileRow(
                                    item = item,
                                    onOpenDir = { viewModel.load(item.path) },
                                    onDownload = { viewModel.enqueueDownload(item) },
                                    onShare = {
                                        val intent = PreviewRouter.shareLinkIntent(item.path)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    onOpenDir: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    CloudListItem(
        title = item.name,
        subtitle = item.subtitleText(),
        onClick = if (item.isDir) onOpenDir else null,
        leading = { FileTypeIcon(item.type) },
        trailing = {
            if (item.isDir) {
                Text("›", color = CloudTextSecondary, style = MaterialTheme.typography.titleLarge)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.testTag("share_button")) {
                        Icon(Icons.Default.Share, contentDescription = "分享链接", tint = CloudPrimary)
                    }
                    IconButton(onClick = onDownload, modifier = Modifier.testTag("download_button")) {
                        Icon(Icons.Default.Download, contentDescription = "下载", tint = CloudPrimary)
                    }
                }
            }
        },
    )
}

private fun FileItem.subtitleText(): String = when {
    isDir -> "文件夹"
    size == 0L -> "未知大小"
    size < 1024L -> "$size B"
    size < 1024L * 1024L -> "${size / 1024L} KB"
    size < 1024L * 1024L * 1024L -> "${size / (1024L * 1024L)} MB"
    else -> "${size / (1024L * 1024L * 1024L)} GB"
}
```

- [ ] **Step 5: Run unit compile checks**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run file screen smoke test if possible**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellShowsFileUploadEntry
```

Expected: `BUILD SUCCESSFUL` when a device/emulator is available. If unavailable, record that connected tests could not run.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt \
        app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt \
        app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: redesign file screen"
```

---

### Task 5: Transfer and Settings Pages

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/TransferProgress.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: Cloud components from Task 2 and theme constants from Task 1.
- Produces: Compact transfer page preserving `暂无传输任务`, cancel/retry behavior; grouped settings page preserving logout/clear preview behavior.

- [ ] **Step 1: Add smoke test for transfer and settings navigation**

Append this test to `AppLaunchTest`:

```kotlin
@Test
fun authenticatedShellNavigatesToTransfersAndSettings() {
    composeRule.setContent {
        AlistClientTheme {
            AppNavHost(startAuthenticated = true)
        }
    }

    composeRule.onNodeWithText("传输").performClick()
    composeRule.onNodeWithText("暂无传输任务").assertIsDisplayed()

    composeRule.onNodeWithText("设置").performClick()
    composeRule.onNodeWithText("清理临时预览文件").assertIsDisplayed()
    composeRule.onNodeWithText("退出登录").assertIsDisplayed()
}
```

Also add this import at the top if absent:

```kotlin
import androidx.compose.ui.test.performClick
```

- [ ] **Step 2: Run the new test before visual rewrite**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellNavigatesToTransfersAndSettings
```

Expected before implementation: it may pass because labels already exist; proceed because this task is visual refactor. If no device is attached, record that connected tests could not run.

- [ ] **Step 3: Style TransferProgress**

Replace `app/src/main/java/com/textvision/alistclient/ui/components/TransferProgress.kt` with:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft

@Composable
fun TransferProgress(bytesDone: Long, totalBytes: Long) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
    if (totalBytes > 0 && totalBytes >= bytesDone) {
        val progress = (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = CloudPrimary,
            trackColor = CloudPrimarySoft,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = CloudPrimary,
            trackColor = CloudPrimarySoft,
        )
    }
}
```

- [ ] **Step 4: Replace TransferScreen layout**

Replace `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt` with:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.TransferProgress
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val EmptyTransferMessage = "暂无传输任务"

data class TransferListUiState(val transfers: List<TransferEntity>) {
    val emptyMessage: String = EmptyTransferMessage
    val shouldShowEmptyState: Boolean = transfers.isEmpty()
}

val TransferEntity.statusText: String
    get() = status.displayName + (failureReason?.let { "：$it" } ?: "")

val TransferEntity.showRetry: Boolean
    get() = status.canRetry

val TransferEntity.retryButtonLabel: String
    get() = status.retryLabel ?: "重试"

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val manager: TransferManager,
) : ViewModel() {
    val transfers = manager.observeTransfers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun cancel(id: String) = manager.cancel(id)
    fun retry(id: String) = manager.retry(id)
}

@Composable
fun TransferScreen(viewModel: TransferViewModel = hiltViewModel()) {
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    TransferScreenContent(
        transfers = transfers,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
    )
}

@Composable
fun TransferScreenContent(
    transfers: List<TransferEntity>,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    val state = TransferListUiState(transfers)
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "传输", subtitle = "上传与下载任务")
        if (state.shouldShowEmptyState) {
            CloudCard {
                CloudEmptyState(title = state.emptyMessage, message = "上传和下载任务会显示在这里")
            }
        } else {
            CloudCard {
                LazyColumn {
                    items(transfers, key = { it.id }) { task ->
                        TransferRow(task, onCancel = { onCancel(task.id) }, onRetry = { onRetry(task.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(task: TransferEntity, onCancel: () -> Unit, onRetry: () -> Unit) {
    CloudListItem(
        title = task.fileName,
        subtitle = task.statusText,
        trailing = {},
    )
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        TransferProgress(task.bytesDone, task.totalBytes)
        Spacer(Modifier.height(6.dp))
        Row {
            if (task.status in setOf(TransferStatus.Waiting, TransferStatus.Uploading, TransferStatus.Downloading)) {
                TextButton(onClick = onCancel) { Text("取消", color = CloudErrorText) }
            }
            if (task.showRetry) {
                TextButton(onClick = onRetry) { Text(task.retryButtonLabel, color = CloudPrimary) }
            }
        }
    }
}
```

- [ ] **Step 5: Replace SettingsScreen layout**

Replace `app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt` with:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val message = remember { mutableStateOf<String?>(null) }
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "设置", subtitle = "账号与本机缓存")
        CloudCard {
            CloudListItem(
                title = "当前服务器",
                subtitle = "已登录的 Alist 服务",
                leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "清理临时预览文件",
                subtitle = "释放本机预览缓存",
                onClick = {
                    val count = viewModel.clearPreviewFiles()
                    message.value = "已清理 $count 个临时文件"
                },
                leading = { Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = CloudPrimary) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
            CloudListItem(
                title = "退出登录",
                subtitle = "清除当前会话并返回登录页",
                onClick = {
                    viewModel.logout()
                    message.value = "已退出登录"
                },
                leading = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = CloudErrorText) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudStatusBanner(
            text = "多账号、管理员、外部网盘管理不在 MVP 范围内",
            kind = CloudBannerKind.Info,
        )
        message.value?.let {
            Spacer(Modifier.height(10.dp))
            CloudStatusBanner(text = it, kind = CloudBannerKind.Info)
        }
    }
}
```

- [ ] **Step 6: Run unit compile checks**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Run transfer/settings navigation smoke test if possible**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellNavigatesToTransfersAndSettings
```

Expected: `BUILD SUCCESSFUL` when a device/emulator is available. If unavailable, record that connected tests could not run.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/TransferProgress.kt \
        app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt \
        app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt \
        app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: redesign transfer and settings screens"
```

---

### Task 6: Preview and Move/Copy Target Picker Unification

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/MoveCopyTargetPickerScreen.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: Cloud components from Task 2 and `FileTypeIcon` tiled variant from Task 4.
- Produces: Safe-area-aware preview and directory picker screens sharing the same HyperOS visual language.

- [ ] **Step 1: Add a preview route smoke test**

Append this test to `AppLaunchTest`:

```kotlin
@Test
fun previewRouteUsesCloudShell() {
    composeRule.setContent {
        AlistClientTheme {
            PreviewScreen(
                filePath = "missing-preview-file.txt",
                onDownload = {},
                onExternalOpen = {},
            )
        }
    }

    composeRule.onNodeWithText("文件预览").assertIsDisplayed()
}
```

Also add import:

```kotlin
import com.textvision.alistclient.ui.screens.PreviewScreen
```

- [ ] **Step 2: Run the preview test and verify it fails before the title exists**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#previewRouteUsesCloudShell
```

Expected before implementation: if a device/emulator is attached, failure mentions missing `文件预览`. If no device is attached, record that connected tests could not run.

- [ ] **Step 3: Replace PreviewScreen layout**

Replace `app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt` with:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.preview.PreviewRouter
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudRoundIconButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudTopBar
import java.io.File

@Composable
fun PreviewScreen(filePath: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    val file = File(filePath)
    CloudScaffold {
        CloudTopBar(
            title = "文件预览",
            subtitle = file.name,
            action = {
                Row {
                    CloudRoundIconButton(Icons.Outlined.Download, "下载", onDownload)
                    Spacer(Modifier.height(0.dp).padding(horizontal = 3.dp))
                    CloudRoundIconButton(Icons.Outlined.OpenInNew, "外部打开", onExternalOpen)
                }
            },
        )
        CloudCard {
            if (file.length() > PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES) {
                CloudEmptyState(
                    title = "文件过大",
                    message = "可以下载或用其他应用打开",
                    action = {
                        Row {
                            TextButton(onClick = onDownload) { Text("下载") }
                            TextButton(onClick = onExternalOpen) { Text("外部打开") }
                        }
                    },
                )
            } else {
                var text by remember(filePath) { mutableStateOf("加载中") }
                LaunchedEffect(filePath) {
                    text = runCatching { file.readText() }.getOrElse { "无法读取文件" }
                }
                Text(
                    text = text,
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
```

- [ ] **Step 4: Replace DirectoryBrowser layout**

Replace `app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt` with:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.file.model.FileItem

@Composable
fun DirectoryBrowser(
    path: String,
    directories: List<FileItem>,
    onOpen: (String) -> Unit,
    onSelectCurrent: (String) -> Unit,
) {
    CloudScaffold {
        CloudTopBar(title = "选择目标目录", subtitle = path)
        CloudCard {
            TextButton(onClick = { onSelectCurrent(path) }) { Text("选择当前目录") }
            if (directories.isEmpty()) {
                CloudEmptyState(title = "没有可选子目录", message = "可以直接选择当前目录")
            } else {
                Spacer(Modifier.height(4.dp))
                LazyColumn {
                    items(directories, key = { it.path }) { dir ->
                        CloudListItem(
                            title = dir.name,
                            subtitle = "文件夹",
                            onClick = { onOpen(dir.path) },
                            leading = { FileTypeIcon(dir.type) },
                            trailing = { Text("›") },
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: Keep MoveCopyTargetPickerScreen behavior unchanged**

Replace `app/src/main/java/com/textvision/alistclient/ui/screens/MoveCopyTargetPickerScreen.kt` with the same behavior and imports normalized:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.ui.components.DirectoryBrowser

@Composable
fun MoveCopyTargetPickerScreen(
    onTargetSelected: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load("/") }
    val success = state as? FileUiState.Success
    DirectoryBrowser(
        path = success?.path ?: "/",
        directories = success?.items.orEmpty().filter { it.isDir },
        onOpen = viewModel::load,
        onSelectCurrent = onTargetSelected,
    )
}
```

- [ ] **Step 6: Run unit compile checks**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Run preview smoke test if possible**

Run:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#previewRouteUsesCloudShell
```

Expected: `BUILD SUCCESSFUL` when a device/emulator is available. If unavailable, record that connected tests could not run.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt \
        app/src/main/java/com/textvision/alistclient/ui/screens/MoveCopyTargetPickerScreen.kt \
        app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: unify preview and picker screens"
```

---

### Task 7: Final Verification, Device Notes, and Visual Polish Pass

**Files:**
- Modify if needed: files changed by Tasks 1-6 only
- Test: Gradle unit and connected instrumentation tests

**Interfaces:**
- Consumes: All previous task outputs.
- Produces: Verified HyperOS redesign with recorded limitations if no device/emulator is available.

- [ ] **Step 1: Run all unit tests**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run all connected Android tests if possible**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected with device/emulator: `BUILD SUCCESSFUL`. If no device/emulator is connected, record the exact Gradle output and state that device verification was skipped because no target was available.

- [ ] **Step 3: Build debug APK**

Run:

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` and APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Manual device inspection on Redmi K90 Pro Max**

Install and inspect the debug APK on the Redmi K90 Pro Max. Verify:

```text
- 登录页顶部内容 starts below the status bar.
- 登录页 bottom content does not collide with the gesture home indicator.
- 文件页 shows compact rows and the floating bottom capsule does not cover the last visible row.
- 底部小白条 remains visually separate from the app bottom bar.
- Transfer and Settings tabs are reachable and visually consistent.
- Text and icons remain readable at the device's current display/font scale.
```

If the agent cannot access the physical device, ask the user to run the APK and provide a screenshot; do not claim device verification passed without evidence.

- [ ] **Step 5: Fix any compile or visual regressions found in verification**

If `./gradlew testDebugUnitTest` fails due to an import or API mismatch, make the minimal targeted fix. Examples:

```kotlin
// Missing Modifier import in a screen file:
import androidx.compose.ui.Modifier

// Missing test import:
import androidx.compose.ui.test.performClick
```

If connected tests fail because a text node is split by semantics, add a stable test tag to the component and update only that assertion. Example:

```kotlin
Modifier.testTag("bottom_tab_files")
```

Then assert with:

```kotlin
composeRule.onNodeWithTag("bottom_tab_files").assertIsDisplayed()
```

- [ ] **Step 6: Re-run verification after fixes**

Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected: both commands report `BUILD SUCCESSFUL`. Run `./gradlew connectedDebugAndroidTest` too if a device/emulator is available.

- [ ] **Step 7: Commit verification polish**

If Step 5 changed code:

```bash
git add app/src/main/java/com/textvision/alistclient app/src/androidTest/java/com/textvision/alistclient
git commit -m "fix: polish hyperos ui verification"
```

If Step 5 did not change code, do not create an empty commit.

---

## Self-Review

### Spec coverage

- HyperOS theme foundation: Task 1.
- Edge-to-edge and system-bar/safe-area handling: Task 1 and Task 2.
- Custom floating bottom navigation: Task 2.
- Minimal HyperOS login page: Task 3.
- High-density file page and file-type tiles: Task 4.
- Transfer page and settings page redesign: Task 5.
- Preview and move/copy target picker visual unification: Task 6.
- Test strategy, connected-test caveat, debug build, and device inspection: Task 7.

### Placeholder scan

The plan contains no `TBD`, `TODO`, `implement later`, or unspecified validation steps. Each task lists exact files, exact code blocks, commands, and expected results.

### Type and signature consistency

The component signatures introduced in Task 2 are used consistently by Tasks 3-6. The plan preserves existing public screen entry points and ViewModel calls, so navigation and Hilt integration remain compatible with the current app structure.
