package profile.infrastructure.di

import io.ktor.server.config.*
import org.koin.dsl.module
import profile.auth.AuthController
import profile.auth.AuthService
import profile.infrastructure.config.*
import profile.infrastructure.db.DatabaseFactory
import profile.infrastructure.db.UserRepository
import profile.infrastructure.db.VerificationTokenRepository
import profile.infrastructure.db.SessionRepository
import profile.infrastructure.events.EmailEventConsumer
import profile.infrastructure.events.EventPublisher
import profile.infrastructure.jwt.JwtIssuer
import profile.infrastructure.redis.RedisManager
import profile.search.SearchController
import profile.search.SearchService
import profile.sessions.SessionController
import profile.sessions.SessionService
import profile.users.UserController
import profile.users.UserService

fun koinModule(config: ApplicationConfig) = module {
    // 1. Typed Config
    val appConfig = AppConfig(
        jwt = JwtConfig(
            issuer = config.property("identity.jwt.issuer").getString(),
            audience = config.property("identity.jwt.audience").getString(),
            secret = config.property("identity.jwt.secret").getString(),
            accessTokenExpMinutes = config.property("identity.jwt.access_token_exp_minutes").getString().toLong()
        ),
        session = SessionConfig(
            refreshTokenExpDays = config.property("identity.session.refresh_token_exp_days").getString().toLong()
        ),
        postgres = PostgresConfig(
            url = config.property("postgres.url").getString(),
            user = config.property("postgres.user").getString(),
            password = config.property("postgres.password").getString()
        ),
        redis = RedisConfig(
            url = config.property("redis.url").getString()
        ),
        smtp = SmtpConfig(
            host = config.property("smtp.host").getString(),
            port = config.property("smtp.port").getString().toInt()
        ),
        s3 = S3Config(
            endpoint = config.property("s3.endpoint").getString(),
            accessKey = config.property("s3.access_key").getString(),
            secretKey = config.property("s3.secret_key").getString(),
            bucket = config.property("s3.bucket").getString()
        )
    )
    
    single { appConfig }
    single { appConfig.jwt }
    single { appConfig.session }
    single { appConfig.postgres }
    single { appConfig.redis }
    single { appConfig.smtp }
    single { appConfig.s3 }
    single { config }

    // 2. Infrastructure
    single { DatabaseFactory.init(get()) }
    single { RedisManager(config) }
    single { profile.infrastructure.storage.S3Client(config) }
    single { EventPublisher(get()) }
    single { EmailEventConsumer(get()) }
    single { JwtIssuer(get()) }
    single { UserRepository(get()) }
    single { SessionRepository(get()) }
    single { VerificationTokenRepository(get()) }
    single { SearchService(get<UserRepository>(), get<RedisManager>()) }

    // 3. Services
    single { AuthService(get(), get(), get(), get(), get(), get(), get()) }
    single { UserService(get(), get(), get()) }
    single { SessionService(get(), get()) }

    // 4. Controllers
    single { AuthController(get()) }
    single { UserController(get()) }
    single { SearchController(get()) }
    single { SessionController(get()) }
}
