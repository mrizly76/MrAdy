package com.sitephotos.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("site_photos", Context.MODE_PRIVATE)

    var selectedProject by mutableStateOf(
        AppConfig.projects.firstOrNull { it.code == prefs.getString("project", null) }
            ?: AppConfig.projects.first()
    )
        private set

    /** Pending photo count per building (key = PhotoStore.key). */
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var totalPending by mutableIntStateOf(0)
        private set

    var driveConnected by mutableStateOf(prefs.getBoolean("drive_connected", false))
        private set

    var syncing by mutableStateOf(false)
        private set
    var syncDone by mutableIntStateOf(0)
        private set
    var syncTotal by mutableIntStateOf(0)
        private set

    /** App lock: locked on every fresh start (the PIN screen is shown first). */
    var locked by mutableStateOf(true)
    var needsPinSetup by mutableStateOf(false)
    var backgroundSince = 0L

    var message by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    init {
        refresh()
    }

    fun selectProject(p: Project) {
        selectedProject = p
        prefs.edit().putString("project", p.code).apply()
    }

    fun refresh() {
        val all = PhotoStore.allPending(getApplication())
        counts = all.groupingBy { PhotoStore.key(it.project, it.building) }.eachCount()
        totalPending = all.size
    }

    fun countFor(p: Project, b: Building) = counts[PhotoStore.key(p, b)] ?: 0

    fun markConnected() {
        driveConnected = true
        prefs.edit().putBoolean("drive_connected", true).apply()
    }

    fun showError(msg: String) {
        error = msg
        message = null
    }

    /** Upload every pending photo, then delete it from the phone. */
    fun sync(accessToken: String) {
        if (syncing) return
        markConnected()
        val items = PhotoStore.allPending(getApplication())
        if (items.isEmpty()) {
            message = "Nothing to sync – all photos are already in Google Drive."
            error = null
            return
        }
        syncing = true
        syncTotal = items.size
        syncDone = 0
        error = null
        message = null

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val drive = DriveUploader(accessToken)
                    val rootId = drive.ensureFolder(AppConfig.DRIVE_ROOT_FOLDER, "root")
                    for (item in items) {
                        if (!item.file.exists()) continue
                        val projectId = drive.ensureFolder(item.project.driveFolder, rootId)
                        val buildingId = drive.ensureFolder(item.building.name, projectId)
                        val dateId = drive.ensureFolder(PhotoStore.dateFolder(item.file), buildingId)
                        drive.uploadPhoto(item.file, dateId)
                        item.file.delete()   // uploaded OK -> remove from phone
                        withContext(Dispatchers.Main) {
                            syncDone++
                            refresh()
                        }
                    }
                }
                message = "Done: $syncTotal photo(s) uploaded to Google Drive › ${AppConfig.DRIVE_ROOT_FOLDER}"
            } catch (e: DriveUploader.AuthExpiredException) {
                error = "Google sign-in expired. Press Sync again. ($syncDone of $syncTotal uploaded; the rest are safe in the app.)"
            } catch (e: Exception) {
                error = "Sync stopped: ${e.message ?: e.javaClass.simpleName}\n" +
                    "$syncDone of $syncTotal uploaded. The rest are kept safely in the app – press Sync again when you have internet."
            } finally {
                syncing = false
                refresh()
            }
        }
    }
}
