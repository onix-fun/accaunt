package profile.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import java.sql.Connection
import javax.sql.DataSource

object DatabaseFactory {
    private var dataSource: DataSource? = null

    fun init(config: ApplicationConfig) {
        val url = config.propertyOrNull("postgres.url")?.getString() ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        val user = config.propertyOrNull("postgres.user")?.getString() ?: "sa"
        val password = config.propertyOrNull("postgres.password")?.getString() ?: ""

        val hikariConfig = HikariConfig().apply {
            driverClassName = if (url.startsWith("jdbc:h2:")) "org.h2.Driver" else "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val ds = HikariDataSource(hikariConfig)
        dataSource = ds

        runFlyway(ds)
    }

    private fun runFlyway(datasource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(datasource)
            .load()
        flyway.migrate()
    }

    fun getConnection(): Connection = dataSource?.connection ?: throw IllegalStateException("Database not initialized")
}
