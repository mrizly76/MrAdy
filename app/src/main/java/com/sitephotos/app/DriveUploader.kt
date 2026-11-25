package com.sitephotos.app

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal Google Drive v3 REST client (folder find/create + file upload).
 * Uses the "drive.file" scope: the app can only see files/folders it created itself.
 * Must be called from a background thread.
 */
class DriveUploader(private val accessToken: String) {

    class AuthExpiredException : IOException("Google sign-in expired")

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    private val folderCache = HashMap<String, String>()
    private val json = "application/json; charset=UTF-8".toMediaType()

    /** Returns the id of folder [name] inside [parentId] ("root" = My Drive), creating it if needed. */
    fun ensureFolder(name: String, parentId: String): String {
        val cacheKey = "$parentId/$name"
        folderCache[cacheKey]?.let { return it }

        val q = "mimeType='application/vnd.google-apps.folder' and trashed=false " +
            "and name='${escape(name)}' and '${escape(parentId)}' in parents"
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("fields", "files(id,name)")
            .addQueryParameter("spaces", "drive")
            .build()
        val found = execute(Request.Builder().url(url).get())
            .getJSONArray("files")
        val id = if (found.length() > 0) {
            found.getJSONObject(0).getString("id")
        } else {
            val meta = JSONObject()
                .put("name", name)
                .put("mimeType", "application/vnd.google-apps.folder")
                .put("parents", JSONArray().put(parentId))
            execute(
                Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files?fields=id")
                    .post(meta.toString().toRequestBody(json))
            ).getString("id")
        }
        folderCache[cacheKey] = id
        return id
    }

    /** Uploads [file] as a JPEG into folder [parentId]. Returns the new Drive file id. */
    fun uploadPhoto(file: File, parentId: String): String {
        val meta = JSONObject()
            .put("name", file.name)
            .put("parents", JSONArray().put(parentId))
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody(json))
            .addPart(file.asRequestBody("image/jpeg".toMediaType()))
            .build()
        return execute(
            Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
                .post(body)
        ).getString("id")
    }

    private fun execute(builder: Request.Builder): JSONObject {
        val request = builder.header("Authorization", "Bearer $accessToken").build()
        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) throw AuthExpiredException()
            if (!resp.isSuccessful) {
                val reason = runCatching {
                    JSONObject(text).getJSONObject("error").getString("message")
                }.getOrDefault(text.take(200))
                throw IOException("Google Drive error ${resp.code}: $reason")
            }
            return JSONObject(text)
        }
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")
}
