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

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.any
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Column
import org.ktorm.schema.varchar

internal open class CompletedMigrations(alias: String?) : MigratableBaseTable<String>("completed_migrations", alias, schema = "ktorm_migration") {
    internal companion object : CompletedMigrations(null)
    override fun aliased(alias: String): CompletedMigrations = CompletedMigrations(alias)

    @Suppress("LeakingThis")
    internal val migrationName: Column<String> = varchar("migrationName").size(256).primaryKey().notNull()

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): String = row[migrationName]!!
}
internal fun Database.markCompleted(migration: Migration){
    this.insert(CompletedMigrations){
        set(it.migrationName, migration.qualifiedName)
    }
}
internal fun Database.markUncomplete(migration: Migration){
    this.delete(CompletedMigrations){
        it.migrationName eq migration.qualifiedName
    }
}

internal fun Database.completedMigrations() = sequenceOf(CompletedMigrations)

internal fun Database.completedMigration(migration: Migration): Boolean {
    return completedMigrations().any {
        it.migrationName eq migration.qualifiedName
    }
}

internal fun Database.enableMigrationTracking(){
    this.executeUpdate(CreateSchemaExpression(CompletedMigrations.schema!!, ifNotExists = true))
    this.executeUpdate(CompletedMigrations.createTable().copy(ifNotExists = true))
}