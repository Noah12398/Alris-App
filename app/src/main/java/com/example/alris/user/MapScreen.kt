package com.example.alris.user

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alris.Constants.logoutAndGoToLogin
import com.example.alris.data.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// --- MAIN MAP SCREEN ---
@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current

    // API client
    val api = remember { ApiClient.createUserApi(context) }

    // ViewModel
    val viewModel: MapViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(api) as T
        }
    })

    val issues = viewModel.issues
    var selectedReport by remember { mutableStateOf<ReportPoint?>(null) }
    var isReportListVisible by remember { mutableStateOf(false) }
    var currentMapType by remember { mutableStateOf(MapType.STANDARD) }
    var showMyLocation by remember { mutableStateOf(true) }

    val mapView = rememberMapViewWithLifecycle(showMyLocation = showMyLocation)

    // Load API data on first load
    LaunchedEffect(Unit) {
        viewModel.resetAndReload(
            userLatitude = 8.8932,
            userLongitude = 76.6141,
            radiusKm = 10
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { mv ->
            // Update tile source if map type changed
            if (mv.tileProvider.tileSource != currentMapType.tileSource) {
                mv.setTileSource(currentMapType.tileSource)
            }

            // Clear previous markers (keep location overlay)
            mv.overlays.removeAll { it is Marker }

            // Add markers for all issues
            issues.forEach { report ->
                val geo = GeoPoint(report.latitude, report.longitude)
                val marker = Marker(mv).apply {
                    position = geo
                    title = report.title
                    snippet = report.description
                    icon = getMarkerIcon(context, report.category, report.status)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        selectedReport = report
                        mv.controller.animateTo(geo)
                        mv.controller.setZoom(18.0)
                        true
                    }
                }
                mv.overlays.add(marker)
            }

            // Center map on first marker
            if (issues.isNotEmpty()) {
                val first = issues[0]
                val firstGeo = GeoPoint(first.latitude, first.longitude)
                mv.controller.setZoom(16.0)
                mv.controller.setCenter(firstGeo)
            }

            mv.invalidate()
        }

        // Top Controls
        MapTopControls(
            currentMapType = currentMapType,
            onMapTypeChange = { currentMapType = it },
            showMyLocation = showMyLocation,
            onLocationToggle = { showMyLocation = it },
            onReportListToggle = { isReportListVisible = !isReportListVisible },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // Logout Button
        FloatingActionButton(
            onClick = { logoutAndGoToLogin(context) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
        }

        // Statistics Card
        MapStatsCard(
            reportPoints = issues,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 140.dp, end = 16.dp)
        )

        // Report Details Bottom Sheet
        selectedReport?.let { report ->
            ReportDetailsCard(
                report = report,
                onDismiss = { selectedReport = null },
                onNavigate = {
                    val geo = GeoPoint(report.latitude, report.longitude)
                    mapView.controller.animateTo(geo)
                    mapView.controller.setZoom(18.0)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Reports List Side Panel
        AnimatedVisibility(
            visible = isReportListVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(320.dp)
        ) {
            ReportsListPanel(
                reports = issues,
                onReportClick = { report ->
                    selectedReport = report
                    val geo = GeoPoint(report.latitude, report.longitude)
                    mapView.controller.animateTo(geo)
                    mapView.controller.setZoom(17.0)
                },
                onClose = { isReportListVisible = false },
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            )
        }

        // FAB for adding new report
        FloatingActionButton(
            onClick = { /* Add new report */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Report", modifier = Modifier.size(24.dp))
        }
    }

    // Auto-pagination
    if (viewModel.hasMore) {
        LaunchedEffect(issues.size) {
            viewModel.loadNearbyIssues()
        }
    }
}
// --- GET MARKER ICON ---
private fun getMarkerIcon(context: Context, category: ReportCategory, status: ReportStatus): Drawable? =
    ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)

// --- TOP CONTROLS ---
@Composable
fun MapTopControls(
    currentMapType: MapType,
    onMapTypeChange: (MapType) -> Unit,
    showMyLocation: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    onReportListToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reports List Button
        FloatingActionButton(
            onClick = onReportListToggle,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = "Reports List",
                modifier = Modifier.size(20.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Map Type Selector
            Card(
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    MapType.values().forEach { type ->
                        val isSelected = currentMapType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { onMapTypeChange(type) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // My Location Button
            FloatingActionButton(
                onClick = { onLocationToggle(!showMyLocation) },
                modifier = Modifier.size(48.dp),
                containerColor = if (showMyLocation) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                contentColor = if (showMyLocation) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- STATS CARD ---
@Composable
fun MapStatsCard(
    reportPoints: List<ReportPoint>,
    modifier: Modifier = Modifier
) {
    val pendingCount = reportPoints.count { it.status == ReportStatus.PENDING }
    val inProgressCount = reportPoints.count { it.status == ReportStatus.IN_PROGRESS }
    val resolvedCount = reportPoints.count { it.status == ReportStatus.RESOLVED }

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            StatItem(pendingCount, "Pending", MaterialTheme.colorScheme.error)
            StatItem(inProgressCount, "In Progress", MaterialTheme.colorScheme.tertiary)
            StatItem(resolvedCount, "Resolved", Color.Green)
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
    }
}

// --- REPORT DETAILS CARD ---
@Composable
fun ReportDetailsCard(
    report: ReportPoint,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        report.category.icon,
                        contentDescription = null,
                        tint = report.category.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            StatusBadge(status = report.status)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reported ${report.created_at}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navigate to Location")
            }
        }
    }
}

// --- REPORTS LIST PANEL ---
@Composable
fun ReportsListPanel(
    reports: List<ReportPoint>,
    onReportClick: (ReportPoint) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportListItem(report = report, onClick = { onReportClick(report) })
                }
            }
        }
    }
}

@Composable
fun ReportListItem(report: ReportPoint, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(report.category.icon, contentDescription = null, tint = report.category.color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(report.created_at, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(status = report.status, compact = true)
        }
    }
}

// --- REMEMBER MAPVIEW WITH LIFECYCLE ---
@Composable
fun rememberMapViewWithLifecycle(showMyLocation: Boolean = false): MapView {
    val context = LocalContext.current

    // Load OsmDroid config
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {

            // Set map source (default: MAPNIK)
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            // FIX: Avoid zoomed-out default view
            controller.setZoom(17.0)  // Good city-level zoom
            controller.setCenter(GeoPoint(8.8932, 76.6141))  // Initial center

            // Optional: My location overlay
            if (showMyLocation) {
                val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                overlay.enableMyLocation()
                overlays.add(overlay)
            }
        }
    }

    // Lifecycle handler
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}

// --- VIEWMODEL ---
class MapViewModel(private val api: UserApi) : ViewModel() {
    var issues by mutableStateOf<List<ReportPoint>>(emptyList())
        private set
    var hasMore by mutableStateOf(false)
        private set
    private var offset = 0
    private val limit = 50
    private var currentRadiusKm: Int = 10
    private var currentUserLatitude: Double? = null
    private var currentUserLongitude: Double? = null

    fun loadNearbyIssues(
        radiusKm: Int = currentRadiusKm,
        userLatitude: Double? = currentUserLatitude,
        userLongitude: Double? = currentUserLongitude
    ) {
        viewModelScope.launch {
            try {
                val response = api.getNearbyIssues(
                    latitude = userLatitude,
                    longitude = userLongitude,
                    radius = radiusKm,
                    limit = limit,
                    offset = offset
                )

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val data = apiResponse?.data ?: return@launch

                    val newIssues = data.issues.map { issue ->
                        ReportPoint(
                            id = issue.issue_id,
                            title = "${issue.category ?: "Issue"} #${issue.issue_id.take(8)}",
                            description = issue.reports?.firstOrNull()?.description ?: "No description",
                            latitude = issue.latitude ?: 0.0,
                            longitude = issue.longitude ?: 0.0,
                            category = when (issue.category?.lowercase()) {
                                "drainage" -> ReportCategory.DRAINAGE
                                "lighting" -> ReportCategory.LIGHTING
                                "waste", "garbage" -> ReportCategory.WASTE
                                "road" -> ReportCategory.ROAD
                                "water" -> ReportCategory.WATER
                                "electricity" -> ReportCategory.ELECTRICITY
                                else -> ReportCategory.OTHER
                            },
                            status = when (issue.status?.lowercase()) {
                                "submitted" -> ReportStatus.PENDING
                                "in_progress" -> ReportStatus.IN_PROGRESS
                                "approved" -> ReportStatus.APPROVED
                                "rejected" -> ReportStatus.REJECTED
                                "resolved" -> ReportStatus.RESOLVED
                                else -> ReportStatus.PENDING
                            },
                            distance_meters = issue.distance_meters ?: 0.0,
                            distance_km = issue.distance_km ?: 0.0,
                            created_at = issue.created_at ?: ""
                        )
                    }

                    issues = issues + newIssues
                    hasMore = data.hasMore
                    offset += limit
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetAndReload(
        radiusKm: Int = 10,
        userLatitude: Double? = null,
        userLongitude: Double? = null
    ) {
        currentRadiusKm = radiusKm
        currentUserLatitude = userLatitude
        currentUserLongitude = userLongitude
        offset = 0
        issues = emptyList()
        loadNearbyIssues(radiusKm, userLatitude, userLongitude)
    }
}

// --- STATUS BADGE ---
@Composable
fun StatusBadge(status: ReportStatus, compact: Boolean = false) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.error to "Pending"
        ReportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary to "In Progress"
        ReportStatus.APPROVED -> Color(0xFF4CAF50) to "Approved"
        ReportStatus.REJECTED -> Color(0xFFF44336) to "Rejected"
        ReportStatus.RESOLVED -> Color(0xFF2196F3) to "Resolved"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.bodySmall, fontSize = if (compact) 10.sp else 12.sp)
    }
}

// --- MAP TYPE ---
enum class MapType(val displayName: String, val tileSource: ITileSource) {
    STANDARD("Map", TileSourceFactory.MAPNIK),
    SATELLITE("Satellite", TileSourceFactory.WIKIMEDIA)
}