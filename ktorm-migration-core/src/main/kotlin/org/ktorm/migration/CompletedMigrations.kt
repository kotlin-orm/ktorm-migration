package org.ktorm.migration

import org.ktorm.database.Database
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Column
import org.ktorm.schema.int
import org.ktorm.schema.varchar

public data class CompletedMigration(
    val packageName: String,
    val migrationName: String
)

public open class CompletedMigrations(alias: String?) : MigrateBaseTable<CompletedMigration>("completed_migrations", alias, schema = "ktorm_migration") {
    public companion object : CompletedMigrations(null)
    override fun aliased(alias: String): CompletedMigrations = CompletedMigrations(alias)

    public val packageName: Column<String> = varchar("packageName").size(256).primaryKey().notNull()
    public val migrationName: Column<String> = varchar("migrationName").size(256)

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): CompletedMigration = CompletedMigration(
        packageName = row[packageName]!!,
        migrationName = row[migrationName]!!,
    )
}
public fun Database.markCompleted(migration: Migration){
    this.executeUpdate(CompletedMigrations.createTable().copy(ifNotExists = true))
    val modified = this.update(CompletedMigrations){
        where {
            it.packageName eq migration.packageName
        }
        set(it.migrationName, migration::class.qualifiedName)
    }
    if(modified == 0){
        this.insert(CompletedMigrations){
            set(it.packageName, migration.packageName)
            set(it.migrationName, migration::class.qualifiedName)
        }
    }
}

public fun Database.lastMigrationFor(packageName: String): Migration? {
    val name = this.sequenceOf(CompletedMigrations).find { it.packageName eq packageName }?.packageName ?: return null
    return Class.forName(name).kotlin.objectInstance as Migration
}