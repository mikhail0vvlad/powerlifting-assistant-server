package com.powerlifting.server.db

import com.powerlifting.server.config.DbConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.sql.DriverManager

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init(dbConfig: DbConfig) {
        log.info("Initializing database connection to: ${dbConfig.jdbcUrl}")

        // Create datasource
        val dataSource = hikari(dbConfig)

        // Test connection with simple JDBC first
        try {
            DriverManager.getConnection(dbConfig.jdbcUrl, dbConfig.user, dbConfig.password).use { conn ->
                log.info("Database connection successful: ${conn.metaData.databaseProductName} ${conn.metaData.databaseProductVersion}")
            }
        } catch (e: Exception) {
            log.error("Failed to connect to database: ${e.message}")
            throw e
        }

        // Run migrations with retry logic
        log.info("Running Flyway migrations...")
        runFlywayMigrations(dbConfig)

        Database.connect(dataSource)
        log.info("Database factory initialized successfully")
    }
    
    private fun runFlywayMigrations(dbConfig: DbConfig, maxRetries: Int = 5) {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                log.info("Flyway migration attempt ${attempt + 1}/$maxRetries")
                val flywayDataSource = HikariConfig().apply {
                    jdbcUrl = dbConfig.jdbcUrl
                    username = dbConfig.user
                    password = dbConfig.password
                    // Bigger pool + a warm idle connection so Flyway's two
                    // concurrent borrows (validate + migrate) don't fight over
                    // a single slot while Neon serverless is waking up.
                    maximumPoolSize = 3
                    minimumIdle = 1
                    connectionTimeout = 60000
                    idleTimeout = 300000
                    maxLifetime = 600000
                    validate()
                }.let { HikariDataSource(it) }
                
                try {
                    Flyway.configure()
                        .dataSource(flywayDataSource)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate()
                    log.info("Flyway migrations completed successfully")
                    return
                } finally {
                    flywayDataSource.close()
                }
            } catch (e: Exception) {
                lastException = e
                log.warn("Flyway migration attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < maxRetries - 1) {
                    Thread.sleep(2000L * (attempt + 1))
                }
            }
        }
        
        log.error("All Flyway migration attempts failed. Last error: ${lastException?.message}")
        log.error("Continuing without migrations. You may need to run them manually later.")
        // Не выбрасываем исключение - позволяем серверу запуститься
    }

    private fun hikari(dbConfig: DbConfig): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = dbConfig.jdbcUrl
            username = dbConfig.user
            password = dbConfig.password
            maximumPoolSize = dbConfig.maxPoolSize.coerceAtMost(5)
            minimumIdle = 1
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}
