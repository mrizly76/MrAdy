package com.sitephotos.app

/**
 * EDIT THIS FILE to rename projects / buildings.
 *
 * - `id` is used for the local storage folder and file names: keep it short, no spaces.
 * - `name` is what you see in the app AND the folder name created in Google Drive.
 */
data class Building(val id: String, val name: String)

data class Project(
    val code: String,           // "219" / "220"
    val name: String,           // shown in the app
    val driveFolder: String,    // folder name in Google Drive
    val buildings: List<Building>,
)

object AppConfig {

    /** Top-level folder the app creates in "My Drive". Everything goes inside it. */
    const val DRIVE_ROOT_FOLDER = "Site Photos - Projects 219 & 220"

    val projects: List<Project> = listOf(
        Project(
            code = "219",
            name = "219 – Address Hotel",
            driveFolder = "219 - Address Hotel",
            buildings = listOf(
                Building("B1", "Building 1"),
                Building("B2", "Building 2"),
                Building("B3", "Building 3"),
                Building("B4", "Building 4"),
            ),
        ),
        Project(
            code = "220",
            name = "220 – Ritz-Carlton",
            driveFolder = "220 - Ritz-Carlton",
            buildings = listOf(
                Building("B5", "Building 5"),
                Building("B6", "Building 6"),
                Building("B7", "Building 7"),
                Building("B8", "Building 8"),
            ),
        ),
    )
}
