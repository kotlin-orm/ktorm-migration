package org.ktorm.migration

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.any
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Column
import org.ktorm.schema.int
import org.ktorm.schema.varchar

internal open class CompletedMigrations(alias: String?) : MigrateBaseTable<String>("completed_migrations", alias, schema = "ktorm_migration") {
    internal companion object : CompletedMigrations(null)
    override fun aliased(alias: String): CompletedMigrations = CompletedMigrations(alias)

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