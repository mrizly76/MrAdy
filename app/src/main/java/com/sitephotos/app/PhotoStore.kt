package com.sitephotos.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingPhoto(val project: Project, val building: Building, val file: File)

/**
 * Photos are written to the app's PRIVATE storage (context.filesDir).
 * They are not visible in the Gallery / Google Photos and are not readable
 * by other apps. They stay here only until they are uploaded by Sync,
 * after which they are deleted.
 */
object PhotoStore {

    private fun root(ctx: Context) = File(ctx.filesDir, "pending")

    fun dir(ctx: Context, project: Project, building: Building): File =
        File(root(ctx), "${project.code}/${building.id}").apply { mkdirs() }

    fun newPhotoFile(ctx: Context, project: Project, building: Building): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(dir(ctx, project, building), "${project.code}_${building.id}_$stamp.jpg")
    }

    fun list(ctx: Context, project: Project, building: Building): List<File> {
        val files = dir(ctx, project, building).listFiles { f -> f.isFile && f.name.endsWith(".jpg") }
            ?: return emptyList()
        // Clean up empty files left by an interrupted capture.
        files.filter { it.length() == 0L && System.currentTimeMillis() - it.lastModified() > 60_000 }
            .forEach { it.delete() }
        return files.filter { it.exists() && it.length() > 0 }.sortedBy { it.name }
    }

    fun allPending(ctx: Context): List<PendingPhoto> =
        AppConfig.projects.flatMap { p ->
            p.buildings.flatMap { b -> list(ctx, p, b).map { PendingPhoto(p, b, it) } }
        }

    fun key(project: Project, building: Building) = "${project.code}/${building.id}"

    fun dateFolder(file: File): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(file.lastModified()))

    /** Small, correctly rotated preview for the review screen. */
    fun loadThumbnail(file: File, maxSize: Int = 360): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSize && bounds.outHeight / (sample * 2) >= maxSize) sample *= 2
        val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        val degrees = when (ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (bmp != null && degrees != 0f) {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        } else bmp
    } catch (e: Exception) {
        null
    }
}
