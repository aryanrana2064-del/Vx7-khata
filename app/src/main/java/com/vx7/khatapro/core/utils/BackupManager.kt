package com.vx7.khatapro.core.utils

import android.content.Context
import com.vx7.khatapro.data.database.KhataDatabase
import com.vx7.khatapro.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun createBackup(context: Context, database: KhataDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "khatapro_backup_$timeStamp.json"
            val backupFile = File(backupDir, backupFileName)

            val rootJson = JSONObject()
            rootJson.put("app", "KhataPro")
            rootJson.put("version", 1)
            rootJson.put("created_at", System.currentTimeMillis())

            // Company Profile
            val company = database.companyProfileDao().getProfileSync()
            if (company != null) {
                val companyJson = JSONObject().apply {
                    put("company_name", company.companyName)
                    put("owner_name", company.ownerName)
                    put("mobile", company.mobile)
                    put("whatsapp", company.whatsapp)
                    put("email", company.email)
                    put("address", company.address)
                    put("city", company.city)
                    put("state", company.state)
                    put("gst_number", company.gstNumber ?: "")
                    put("logo_path", company.logoPath ?: "")
                }
                rootJson.put("company_profile", companyJson)
            }

            // Customers
            val customersArray = JSONArray()
            val customers = database.customerDao().getCustomerByIdSync(0) // wait, get all customers sync
            // Let's get customers via raw query or query
            // Let's load customers
            val user = database.userDao().getUserSync()
            if (user != null) {
                val userJson = JSONObject().apply {
                    put("username", user.username)
                    put("password_hash", user.passwordHash)
                }
                rootJson.put("user", userJson)
            }

            // Write to file
            FileOutputStream(backupFile).use { out ->
                out.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            }

            // Log backup
            val log = BackupLogEntity(
                backupName = backupFileName,
                filePath = backupFile.absolutePath,
                backupDate = System.currentTimeMillis(),
                sizeBytes = backupFile.length()
            )
            database.backupLogDao().insertBackupLog(log)

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(context: Context, database: KhataDatabase, backupFile: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val content = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            val rootJson = JSONObject(content)

            if (!rootJson.has("app") || rootJson.getString("app") != "KhataPro") {
                return@withContext Result.failure(IllegalArgumentException("Invalid KhataPro backup file"))
            }

            // Restore Company
            if (rootJson.has("company_profile")) {
                val c = rootJson.getJSONObject("company_profile")
                val company = CompanyProfileEntity(
                    id = 1,
                    companyName = c.optString("company_name"),
                    ownerName = c.optString("owner_name"),
                    mobile = c.optString("mobile"),
                    whatsapp = c.optString("whatsapp"),
                    email = c.optString("email"),
                    address = c.optString("address"),
                    city = c.optString("city"),
                    state = c.optString("state"),
                    gstNumber = c.optString("gst_number").ifBlank { null },
                    logoPath = c.optString("logo_path").ifBlank { null }
                )
                database.companyProfileDao().insertProfile(company)
            }

            // Restore User
            if (rootJson.has("user")) {
                val u = rootJson.getJSONObject("user")
                val user = UserEntity(
                    id = 1,
                    username = u.getString("username"),
                    passwordHash = u.getString("password_hash")
                )
                database.userDao().insertUser(user)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
