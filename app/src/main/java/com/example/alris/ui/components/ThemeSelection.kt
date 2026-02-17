package com.example.alris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alris.data.AppTheme
import com.example.alris.data.ThemeManager
import kotlinx.coroutines.launch

@Composable
fun ThemeSelector() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val currentTheme by themeManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM)
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    title = "System",
                    icon = Icons.Default.BrightnessAuto,
                    selected = currentTheme == AppTheme.SYSTEM,
                    onClick = { scope.launch { themeManager.setTheme(AppTheme.SYSTEM) } },
                    modifier = Modifier.weight(1f)
                )
                
                ThemeOption(
                    title = "Light",
                    icon = Icons.Default.LightMode,
                    selected = currentTheme == AppTheme.LIGHT,
                    onClick = { scope.launch { themeManager.setTheme(AppTheme.LIGHT) } },
                    modifier = Modifier.weight(1f)
                )
                
                ThemeOption(
                    title = "Dark",
                    icon = Icons.Default.DarkMode,
                    selected = currentTheme == AppTheme.DARK,
                    onClick = { scope.launch { themeManager.setTheme(AppTheme.DARK) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
