package org.ktorm.migration

import java.io.File

public fun initializeMigrations(
    folder: File,
    packageName: String
) {
    folder.resolve("LatestMigration.kt").writeText("""
        package $packageName

        import org.ktorm.migration.Migration

        public typealias LatestMigration = Migration.None
    """.trimIndent())

}

public fun makeMigrations(
    folder: File,
    packageName: String,
    latestMigration: Migration = Migration.None,
    migrationName: String = "Migration${(latestMigration.number + 1).toString().padStart(4, '0')}",
    vararg tables: MigrateTableMixin
): Boolean {
    folder.mkdirs()
    val building = BuildingTables()
    val processed = HashSet<Migration>()
    fun process(migration: Migration) {
        if (!processed.add(migration)) return
        for (dep in migration.dependsOn) {
            process(dep)
        }
        migration.migrateTables(building)
    }
    process(latestMigration)

    val updates = building.tables.values.upgradeTo(tables.toList())
    if(updates.isEmpty()) {
        return false
    }

    val out = StringBuilder()
    out.appendLine("package $packageName")
    out.appendLine("import ${latestMigration::class.qualifiedName}")
    updates.generateMigrationSource(
        name = migrationName,
        number = latestMigration.number + 1,
        dependsOn = listOf(latestMigration::class.simpleName!!),
        out = out
    )
    folder.resolve("$migrationName.kt").writeText(out.toString())
    folder.resolve("LatestMigration.kt").writeText("""
        package $packageName
        public typealias LatestMigration = $migrationName
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