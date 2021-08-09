package org.ktorm.migration

import java.io.File

public fun initializeMigrations(
    folder: File,
    packageName: String
) {
    folder.resolve("LatestMigration.kt").writeText("""
        package $packageName

        import org.ktorm.migration.Migration

        public val LatestMigration: Migration? = null
    """.trimIndent())

}

private val Migration.number: Int get() = (this::class.simpleName ?: "").substringAfter("Migration").takeWhile { it.isDigit() }.toIntOrNull() ?: 0

public fun makeMigrations(
    folder: File,
    packageName: String,
    latestMigration: Migration? = null,
    migrationName: String = "Migration${((latestMigration?.number ?: 0) + 1).toString().padStart(4, '0')}",
    vararg tables: MigrateTableMixin
): Boolean {
    folder.mkdirs()
    val building = BuildingTables()
    latestMigration?.let { building.apply(it) }

    val updates = building._tables.values.upgradeTo(tables.toList())
    if(updates.isEmpty()) {
        return false
    }

    val out = StringBuilder()
    updates.generateMigrationSource(
        name = migrationName,
        packageName = packageName,
        dependsOn = listOfNotNull(latestMigration),
        out = out
    )
    folder.resolve("$migrationName.kt").writeText(out.toString())
    folder.resolve("LatestMigration.kt").writeText("""
        package $packageName
        
        import org.ktorm.migration.Migration
        
        public val LatestMigration: Migration = $migrationName
    """.trimIndent())

    return true
}

/*

Folder structure

0001_nameOfMigration.kt
0002_nameOfMigration.kt
0003_nameOfMigration.kt
0004_nameOfMigration.kt
0005_nameOfMigration.kt
Migrations.kt


Migrations.kt:

val includedTables = listOf(
    Employees,
    Departments
)
fun main() = 0005_nameOfMigration(databaseFromEnv, includedTables)


0005_nameOfMigration.kt

fun 0005_nameOfMigration(database: Database, tables) {

}

 */