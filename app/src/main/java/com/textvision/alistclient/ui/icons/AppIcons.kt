package com.textvision.alistclient.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    // — Navigation (Material Icons Extended fallbacks for system / generic icons)
    val home       : ImageVector = Icons.Outlined.Home
    val file       : ImageVector = Icons.Outlined.Description
    val settings   : ImageVector = Icons.Outlined.Settings
    val search     : ImageVector = Icons.Outlined.Search
    val refresh    : ImageVector = Icons.Outlined.Refresh
    val more       : ImageVector = Icons.Outlined.MoreVert
    val back       : ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val chevronRight : ImageVector = Icons.Outlined.ChevronRight

    // — File types (mapped to container colors in FileTypeIcon.kt)
    val folder     : ImageVector = Icons.Outlined.Folder
    val image      : ImageVector = Icons.Outlined.Image
    val video      : ImageVector = Icons.Outlined.Movie
    val audio      : ImageVector = Icons.Outlined.MusicNote
    val doc        : ImageVector = Icons.Outlined.Description
    val archive    : ImageVector = Icons.Outlined.Archive
    val upload     : ImageVector = Icons.Outlined.CloudUpload
    val download   : ImageVector = Icons.Outlined.CloudDownload
    val transfer   : ImageVector = Icons.Outlined.SwapVert

    // — Action
    val share      : ImageVector = Icons.Outlined.Share
    val link       : ImageVector = Icons.Outlined.Link
    val trash      : ImageVector = Icons.Outlined.Delete
    val copy       : ImageVector = Icons.Outlined.ContentCopy
    val external   : ImageVector = Icons.Outlined.OpenInNew
    val cookie     : ImageVector = Icons.Outlined.Cookie

    // — Status
    val alert      : ImageVector = Icons.Outlined.Warning
    val offline    : ImageVector = Icons.Outlined.WifiOff
    val check      : ImageVector = Icons.Outlined.Check
    val sparkle    : ImageVector = Icons.Outlined.AutoAwesome
    val shield     : ImageVector = Icons.Outlined.Shield
    val user       : ImageVector = Icons.Outlined.Person
    val cloud      : ImageVector = Icons.Outlined.Cloud
    val database   : ImageVector = Icons.Outlined.Storage

    // — Theme switcher
    val sun        : ImageVector = Icons.Outlined.LightMode
    val moon       : ImageVector = Icons.Outlined.DarkMode
    val auto       : ImageVector = Icons.Outlined.BrightnessAuto
    val logout     : ImageVector = Icons.Outlined.Logout

    // — Music controls
    val play       : ImageVector = Icons.Outlined.PlayArrow
    val pause      : ImageVector = Icons.Outlined.Pause
    val skipPrev   : ImageVector = Icons.Outlined.SkipPrevious
    val skipNext   : ImageVector = Icons.Outlined.SkipNext
    val heart      : ImageVector = Icons.Outlined.FavoriteBorder
    val heartFill  : ImageVector = Icons.Outlined.Favorite
    val repeat     : ImageVector = Icons.Outlined.Repeat
    val shuffle    : ImageVector = Icons.Outlined.Shuffle
    val queue      : ImageVector = Icons.Outlined.QueueMusic
    val musicNote  : ImageVector = Icons.Outlined.MusicNote
    val filter     : ImageVector = Icons.Outlined.FilterList
    val sort       : ImageVector = Icons.Outlined.Sort
    val mic        : ImageVector = Icons.Outlined.Mic

    // — Decorative / utility (custom path-based, see below)
    val decoNote: ImageVector = ImageVector.Builder(
        name = "decoNote",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).build()
    val decoStar = Icons.Outlined.Star
    val decoHeart = Icons.Outlined.Favorite
    val decoDisc: ImageVector = Icons.Outlined.Album
    val decoFlame: ImageVector = Icons.Outlined.LocalFireDepartment
    val decoWave: ImageVector = Icons.Outlined.GraphicEq
    val decoSpark: ImageVector = Icons.Outlined.AutoAwesome
    val decoHead: ImageVector = Icons.Outlined.Headphones

    // — Tools
    val server: ImageVector = Icons.Outlined.Dns
    val broom: ImageVector = Icons.Outlined.CleaningServices
}
