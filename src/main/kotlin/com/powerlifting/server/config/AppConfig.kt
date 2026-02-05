package com.powerlifting.server.config

import java.io.File
import java.net.URI
import java.util.Properties

data class AppConfig(
    val port: Int,
    val db: DbConfig,
    val firebase: FirebaseConfig,
    val corsAllowAll: Boolean = true,
    val devBypassAuth: Boolean = false,
)

data class DbConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
)

data class FirebaseConfig(
    val projectId: String?,
    /** Either path or base64. If both provided, path wins. */
    val serviceAccountPath: String?,
    val serviceAccountBase64: String?,
)

object ConfigLoader {
    private fun loadLocalConfig(): Properties? {
        val file = File("server-local.properties")
        if (!file.exists()) return null
        return Properties().apply { file.inputStream().use { load(it) } }
    }

    private fun getProp(name: String, default: String? = null): String? {
        // First try env
        System.getenv(name)?.let { return it }
        // Then local file
        loadLocalConfig()?.getProperty(name)?.let { return it }
        return default
    }

    private fun getPropRequired(name: String): String {
        return getProp(name) ?: error("$name env var is required (Neon/Postgres connection string)")
    }

    fun loadFromEnv(): AppConfig {
        val port = getProp("PORT")?.toIntOrNull() ?: 8080

        val devBypassAuth = getProp("DEV_BYPASS_AUTH", "false")?.equals("true", ignoreCase = true) ?: false

        val dbUrlRaw = getProp("DATABASE_URL")
            ?: getProp("JDBC_DATABASE_URL")
            ?: error("DATABASE_URL env var is required (Neon/Postgres connection string)")

        val (jdbcUrl, user, password) = parseDbUrl(dbUrlRaw)

        val db = DbConfig(
            jdbcUrl = jdbcUrl,
            user = getProp("DB_USER") ?: user,
            password = getProp("DB_PASSWORD") ?: password,
            maxPoolSize = getProp("DB_POOL_SIZE")?.toIntOrNull()?.coerceIn(1, 50) ?: 10
        )

        val firebase = FirebaseConfig(
            projectId = getProp("FIREBASE_PROJECT_ID"),
            serviceAccountPath = getProp("FIREBASE_SERVICE_ACCOUNT_PATH"),
            serviceAccountBase64 = getProp("FIREBASE_SERVICE_ACCOUNT_BASE64")
        )

        return AppConfig(
            port = port,
            db = db,
            firebase = firebase,
            corsAllowAll = getProp("CORS_ALLOW_ALL", "true")?.equals("true", ignoreCase = true) ?: true,
            devBypassAuth = devBypassAuth,
        )
    }

    /**
     * Accepts:
     * - jdbc:postgresql://host:5432/db?sslmode=require
     * - postgresql://user:pass@host:5432/db?sslmode=require
     * - postgres://user:pass@host:5432/db
     */
    private fun parseDbUrl(raw: String): Triple<String, String, String> {
        if (raw.startsWith("jdbc:")) {
            // If already JDBC, user/password must be provided via env
            val user = getProp("DB_USER") ?: ""
            val pass = getProp("DB_PASSWORD") ?: ""
            if (user.isBlank() || pass.isBlank()) {
                error("When DATABASE_URL is JDBC, you must also set DB_USER and DB_PASSWORD")
            }
            return Triple(raw, user, pass)
        }

        val uri = URI(raw)
        val userInfo = uri.userInfo ?: ""
        val parts = userInfo.split(":", limit = 2)
        val user = parts.getOrNull(0) ?: ""
        val pass = parts.getOrNull(1) ?: ""
        if (user.isBlank() || pass.isBlank()) {
            error("DATABASE_URL must include user:password (e.g. postgresql://user:pass@host/db)")
        }

        val host = uri.host ?: error("DATABASE_URL has no host")
        val port = if (uri.port == -1) 5432 else uri.port
        val dbName = uri.path?.removePrefix("/")?.takeIf { it.isNotBlank() }
            ?: error("DATABASE_URL has no database name")

        val query = uri.query?.let { "?$it" } ?: ""
        val jdbc = "jdbc:postgresql://$host:$port/$dbName$query"

        return Triple(jdbc, user, pass)
    }
}
