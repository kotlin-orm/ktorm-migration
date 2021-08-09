package org.ktorm.migration

import org.ktorm.database.Database
import org.ktorm.expression.ScalarExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.schema.*
import java.lang.Appendable
import java.time.LocalDate

public interface Migration {
    public val dependsOn: List<Migration>
    public val actions: List<MigrationAction>
    public val qualifiedName: String get() = this::class.qualifiedName!!
    public val packageName: String get() = qualifiedName.substringBeforeLast('.')
    public val simpleName: String get() = qualifiedName.substringAfterLast('.')
}

public sealed class MigrationAction {
    public abstract fun migrate(database: Database)
    public abstract fun undo(database: Database)
    public abstract fun migrateTables(tables: BuildingTables)
    public abstract fun undoTables(tables: BuildingTables)

    public abstract class Kotlin : MigrationAction() {
        override fun migrate(database: Database) {}
        override fun undo(database: Database) {}
        override fun migrateTables(tables: BuildingTables) {}
        override fun undoTables(tables: BuildingTables) {}
    }

    public data class ReversibleSql(public val expression: ReversibleMigrationExpression) : MigrationAction() {
        override fun migrate(database: Database) {
            database.executeUpdate(expression.migrate())
        }

        override fun undo(database: Database) {
            database.executeUpdate(expression.undo())
        }

        override fun migrateTables(tables: BuildingTables) {
            tables.apply(expression.migrate())
        }
        override fun undoTables(tables: BuildingTables) {
            tables.apply(expression.undo())
        }
    }
}

public fun List<ReversibleMigrationExpression>.generateMigrationSource(
    name: String,
    packageName: String,
    dependsOn: List<Migration>,
    out: Appendable
) {
    val regen = SourceRegenerator()
    regen.visit(this.map { MigrationAction.ReversibleSql(it) })
    out.appendLine("package $packageName")
    for(dep in dependsOn) {
        out.appendLine("import ${dep.qualifiedName}")
    }
    out.appendLine("import org.ktorm.migration.Migration")
    out.appendLine("import org.ktorm.migration.MigrationAction")
    for(import in regen.imports){
        out.appendLine("import $import")
    }
    out.appendLine()
    out.appendLine("public object $name: Migration {")
    out.appendLine("    override val dependsOn: List<Migration> = listOf(${dependsOn.joinToString(){ it.simpleName }})")
    out.appendLine("    override val actions: List<MigrationAction> = ${regen.builder}")
    out.appendLine("}")
}