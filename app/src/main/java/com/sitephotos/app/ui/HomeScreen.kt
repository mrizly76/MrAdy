package com.sitephotos.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sitephotos.app.AppConfig
import com.sitephotos.app.Building
import com.sitephotos.app.MainViewModel
import com.sitephotos.app.Project

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenCamera: (Project, Building) -> Unit,
    onReview: (Project, Building) -> Unit,
    onSync: () -> Unit,
    onSecurity: () -> Unit,
) {
    val ctx = LocalContext.current
    var waitingFor by remember { mutableStateOf<Building?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val b = waitingFor
        waitingFor = null
        if (granted && b != null) onOpenCamera(vm.selectedProject, b)
        else if (!granted) vm.showError("Camera permission is needed to take photos.")
    }
    fun openCamera(b: Building) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onOpenCamera(vm.selectedProject, b)
        } else {
            waitingFor = b
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    // Keep the screen awake while uploading.
    val view = LocalView.current
    DisposableEffect(vm.syncing) {
        view.keepScreenOn = vm.syncing
        onDispose { view.keepScreenOn = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Site Photos",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSecurity, enabled = !vm.syncing) {
                    Text("🔒 Security", color = Color.White)
                }
            }
            Text(
                if (vm.driveConnected) "Google Drive connected  •  ${AppConfig.DRIVE_ROOT_FOLDER}"
                else "Google Drive not connected yet – press Sync to connect",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
            )
        }

        // Project selector
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppConfig.projects.forEach { p ->
                val selected = p.code == vm.selectedProject.code
                val waiting = p.buildings.sumOf { vm.countFor(p, it) }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !vm.syncing) { vm.selectProject(p) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondary else Color.White,
                    shadowElevation = if (selected) 4.dp else 1.dp,
                ) {
                    Column(Modifier.padding(vertical = 12.dp, horizontal = 10.dp)) {
                        Text(
                            "Project ${p.code}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            p.name.substringAfter("– ", p.name) + if (waiting > 0) "  •  $waiting waiting" else "",
                            fontSize = 12.sp,
                            color = if (selected) Color.White.copy(alpha = 0.9f) else Color.Gray,
                        )
                    }
                }
            }
        }

        Text(
            "Select a building to take photos",
            Modifier.padding(horizontal = 16.dp),
            color = Color.Gray,
            fontSize = 13.sp,
        )

        // Buildings
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vm.selectedProject.buildings, key = { it.id }) { b ->
                val count = vm.countFor(vm.selectedProject, b)
                Card(
                    onClick = { if (!vm.syncing) openCamera(b) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(
                                        if (count > 0) MaterialTheme.colorScheme.tertiary else Color(0xFFCFD8DC),
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (count == 0) "No photos waiting" else "$count photo(s) waiting",
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Text(
                                "📷 Take photos",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                        if (count > 0) {
                            TextButton(
                                onClick = { onReview(vm.selectedProject, b) },
                                enabled = !vm.syncing,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(30.dp),
                            ) { Text("Review / delete", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        // Sync panel
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(14.dp)
            ) {
                vm.error?.let {
                    Text(it, color = Color(0xFFC62828), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
                vm.message?.let {
                    Text(it, color = Color(0xFF2E7D32), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
                if (vm.syncing) {
                    Text("Uploading ${vm.syncDone} of ${vm.syncTotal} …", fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (vm.syncTotal == 0) 0f else vm.syncDone / vm.syncTotal.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Button(
                    onClick = onSync,
                    enabled = !vm.syncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        if (vm.syncing) "SYNCING…" else "SYNC TO GOOGLE DRIVE  (${vm.totalPending})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
