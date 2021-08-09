/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.migration

import java.io.File

/**
 * Initializes a given [folder] and [packageName] to store migrations.
 */
@Suppress("unused")
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

/**
 * Generates new migrations based on detected changes in the tables.
 * @param folder The folder that holds migrations for this package.
 * @param packageName The name of the package.
 * @param latestMigration The newest migration for this package, usually will be packageName.LatestMigration
 * @param migrationName The name of the new migration.  Defaults to Migration{number}.
 * @param tables The tables this package includes.
 * @return Whether any migrations where created.
 */
public fun makeMigrations(
    folder: File,
    packageName: String,
    latestMigration: Migration? = null,
    migrationName: String = "Migration${((latestMigration?.number ?: 0) + 1).toString().padStart(4, '0')}",
    tables: Collection<MigratableTableMixin>
): Boolean {
    folder.mkdirs()
    val building = TableRebuilder()
    latestMigration?.let { building.apply(it) }

    val updates = building.tables.values.upgradeTo(tables)
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
