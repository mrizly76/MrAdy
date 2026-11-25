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
            name = "219 – Address Hotel & SA",
            driveFolder = "219 - Address Hotel & SA",
            buildings = listOf(
                Building("H01", "H01"),
                Building("H02", "H02"),
                Building("R03", "R03"),
                Building("R04", "R04"),
            ),
        ),
        Project(
            code = "220",
            name = "220 – Ritz Hotel & BR",
            driveFolder = "220 - Ritz Hotel & BR",
            buildings = listOf(
                Building("H04", "H04"),
                Building("H05", "H05"),
                Building("R05", "R05"),
                Building("R09", "R09"),
            ),
        ),
    )
}
