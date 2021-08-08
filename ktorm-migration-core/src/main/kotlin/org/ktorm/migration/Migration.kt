package org.ktorm.migration

import org.ktorm.database.Database
import org.ktorm.expression.ScalarExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.schema.*
import java.lang.Appendable
import java.time.LocalDate

public interface Migration : MigrationDSL {
    public val dependsOn: List<Migration>
    public val actions: List<MigrationAction>
}
public fun Migration.migrate(database: Database) {
    val currentNumber = database.migrationNumberFor(this.packageName)
    val processed = HashSet<Migration>()
    fun process(migration: Migration) {
        if (!processed.add(migration)) return
        if (migration.number <= currentNumber) return
        for (dep in migration.dependsOn) {
            process(dep)
        }
        database.useTransaction {
            migration.migrateThisOnly(database)
            database.markCompleted(migration)
        }
    }
    process(this)
}
public fun Migration.undo(database: Database, backTo: Migration) {

    val processed = HashSet<Migration>()
    fun process(migration: Migration) {
        if (!processed.add(migration)) return
        for (dep in migration.dependsOn) {
            process(dep)
        }
        migration.migrateTables(building)
    }
    process(latestMigration)
}

private fun Migration.migrateThisOnly(database: Database) {
    actions.forEach { it.migrate(database) }
}
private fun Migration.undoThisOnly(database: Database) {
    actions.asReversed().forEach { it.undo(database) }
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

public interface MigrationDSL {

    public fun TableReferenceExpression.create(
        setup: CreateTableExpressionBuilder.()->Unit
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(CreateTableExpressionBuilder().apply(setup).build(this))

    public fun TableReferenceExpression.drop(
        columns: List<ColumnDeclarationExpression<*>>,
        constraints: Map<String, TableConstraintExpression> = emptyMap(),
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(-CreateTableExpression(this, columns, constraints))

    public fun TableReferenceExpression.addColumn(
        name: String,
        sqlType: SqlType<*>,
        size: Int? = null,
        notNull: Boolean = false,
        default: ScalarExpression<out Any>? = null,
        autoIncrement: Boolean = false,
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(
        AlterTableAddExpression(
            this, ColumnDeclarationExpression(
                name, sqlType, size, notNull, default, autoIncrement
            )
        )
    )

    public fun TableReferenceExpression.dropColumn(
        name: String,
        sqlType: SqlType<*>,
        size: Int? = null,
        notNull: Boolean = false,
        default: ScalarExpression<out Any>? = null,
        autoIncrement: Boolean = false,
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(
        -AlterTableAddExpression(
            this, ColumnDeclarationExpression(
                name, sqlType, size, notNull, default, autoIncrement
            )
        )
    )

    public fun TableReferenceExpression.alterColumn(
        name: String,
        oldType: SqlType<*>,
        newType: SqlType<*>,
        oldSize: Int? = null,
        newSize: Int? = null,
        oldNotNull: Boolean = false,
        newNotNull: Boolean = false
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(
        AlterTableModifyColumnReversible(
            this,
            ColumnReferenceExpression(name),
            oldType,
            newType,
            oldSize,
            newSize,
            oldNotNull,
            newNotNull
        )
    )

    public fun TableReferenceExpression.setColumnDefault(
        name: String,
        oldDefault: ScalarExpression<*>? = null,
        newDefault: ScalarExpression<*>? = null
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(
        AlterTableDefaultReversible(
            this,
            ColumnReferenceExpression(name),
            oldDefault,
            newDefault
        )
    )

    public fun <T: Any> TableReferenceExpression.addConstraint(
        constraintName: String,
        tableConstraint: TableConstraintExpression,
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(
        AlterTableAddConstraintExpression(
            this,
            constraintName,
            tableConstraint
        )
    )
}

public class CreateTableExpressionBuilder {
    private val columns = ArrayList<ColumnDeclarationExpression<*>>()
    private val constraints = HashMap<String, TableConstraintExpression>()

    public fun column(
        name: String,
        sqlType: SqlType<*>,
        size: Int? = null,
        notNull: Boolean = false,
        default: ScalarExpression<out Any>? = null,
        autoIncrement: Boolean = false,
    ) {
        columns.add(ColumnDeclarationExpression(
            name, sqlType, size, notNull, default, autoIncrement
        ))
    }

    public fun build(name: TableReferenceExpression): CreateTableExpression = CreateTableExpression(
        name = name,
        columns = columns,
        constraints = constraints
    )
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
        out.appendLine("import ${dep::class.qualifiedName}")
    }
    out.appendLine("import org.ktorm.migration.Migration")
    out.appendLine("import org.ktorm.migration.MigrationAction")
    for(import in regen.imports){
        out.appendLine("import $import")
    }
    out.appendLine()
    out.appendLine("public object $name: Migration {")
    out.appendLine("    override val dependsOn: List<Migration> = listOf(${dependsOn.joinToString(){ it::class.simpleName!! }})")
    out.appendLine("    override val actions: List<MigrationAction> = ${regen.builder}")
    out.appendLine("}")
}