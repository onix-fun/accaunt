package profile.infrastructure.db

object SqlLoader {
    fun load(path: String): String {
        return this::class.java.getResourceAsStream("/sql/$path")?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("SQL file not found: $path")
    }
}
