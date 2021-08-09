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

import org.ktorm.dsl.QueryRowSet
import org.ktorm.expression.ScalarExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.schema.BaseTable

/**
 * Rebuilds the schema from a collection of migrations.
 */
public class TableRebuilder {

    /**
     * The tables this rebuilder knows about.
     */
    public val tables: Map<TableReferenceExpression, MigratableTableMixin> get() = tablesInternal
    internal val tablesInternal: MutableMap<TableReferenceExpression, MigrationTable> = HashMap()

    /**
     * The migrations which have been applied to this builder.
     */
    @Suppress("unused")
    public val appliedMigrations: Set<Migration> get() = _appliedMigrations
    private val _appliedMigrations = HashSet<Migration>()

    /**
     * Applies a migration.
     */
    public fun apply(migration: Migration) {
        if (!_appliedMigrations.add(migration)) return
        for (dep in migration.dependsOn) {
            apply(dep)
        }
        migration.actions.forEach { it.migrateTables(this) }
    }

    /**
     * Applies an individual migration expression.
     */
    public fun apply(sql: SqlExpression) {
        when(sql){
            is CreateTableExpression -> tablesInternal[sql.name] = MigrationTable(
                tableName = sql.name.name,
                alias = null,
                catalog = sql.name.catalog,
                schema = sql.name.schema
            ).apply {
                val pk = sql.constraints.values.mapNotNull { it as? PrimaryKeyTableConstraintExpression }.single()
                for (col in sql.columns) {
                    val recreated = self.registerColumn(col.name, col.sqlType)
                    if (pk.across.any { it.name == col.name }) {
                        recreated.primaryKey()
                    }
                    if (col.notNull) columnNotNull.add(col.name)
                    if (col.autoIncrement) columnAutoIncrement.add(col.name)
                    if (col.default != null) columnDefault[col.name] = col.default
                    if (col.size != null) columnSize[col.name] = col.size
                }
                for (constraint in sql.constraints) {
                    if (constraint.value is PrimaryKeyTableConstraintExpression) continue
                    constraints[constraint.key] = constraint.value.reverse(this) { tablesInternal[it]!! }
                }
            }
            is DropTableExpression -> {
                tablesInternal.remove(sql.table)
            }
            is AlterTableAddExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy().apply {
                    registerColumn(sql.column.name, sql.column.sqlType)
                    val col = sql.column
                    if (col.notNull) columnNotNull.add(col.name)
                    if (col.autoIncrement) columnAutoIncrement.add(col.name)
                    if (col.default != null) columnDefault[col.name] = col.default
                    if (col.size != null) columnSize[col.name] = col.size
                } 
            }
            is AlterTableDropColumnExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy(skipColumns = listOf(sql.column.name)).apply {
                    scrubAnnotations(sql.column.name)
                }
            }
            is AlterTableModifyColumnExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy(skipColumns = listOf(sql.column.name)).apply {
                    registerColumn(sql.column.name, sql.newType)
                    if(sql.size != null) this.columnSize[sql.column.name] = sql.size
                    else this.columnSize.remove(sql.column.name)
                    if(sql.notNull) this.columnNotNull.add(sql.column.name)
                    else this.columnNotNull.remove(sql.column.name)
                }
            }
            is AlterTableSetDefaultExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy().apply {
                    columnDefault[sql.column.name] = sql.default
                }
            }
            is AlterTableDropDefaultExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy().apply {
                    columnDefault.remove(sql.column.name)
                }
            }
            is AlterTableAddConstraintExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy().apply {
                    constraints[sql.constraintName] = sql.tableConstraint.reverse(this) { tablesInternal[it]!! }
                }
            }
            is AlterTableDropConstraintExpression -> {
                tablesInternal[sql.table] = tablesInternal[sql.table]!!.copy().apply {
                    constraints.remove(sql.constraintName)
                }
            }
        }
    }
}

internal class MigrationTable(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null
) : BaseTable<Map<String, Any?>>(tableName, alias, catalog, schema), MigratableTableMixin {
    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): Map<String, Any?> {
        return columns.associate { it.name to row[it] }
    }

    override val self: BaseTable<*>
        get() = this
    override val columnNotNull: HashSet<String> = HashSet()
    override val columnAutoIncrement: HashSet<String> = HashSet()
    override val columnSize: HashMap<String, Int> = HashMap()
    override val columnDefault: HashMap<String, ScalarExpression<*>> = HashMap()
    override val constraints: HashMap<String, Constraint> = HashMap()

    internal fun copy(skipColumns: Collection<String> = emptyList()): MigrationTable {
        val new = MigrationTable(tableName, alias, catalog, schema)
        new.columnNotNull.addAll(columnNotNull)
        new.columnAutoIncrement.addAll(columnAutoIncrement)
        new.columnSize.putAll(columnSize)
        new.columnDefault.putAll(columnDefault)
        new.constraints.putAll(constraints)
        for (col in columns) {
            if (col.name !in skipColumns) {
                val newCol = new.registerColumn(col.name, col.sqlType)
                if(col in self.primaryKeys) {
                    with(new) {
                        newCol.primaryKey()
                    }
                }
            }
        }
        return new
    }

    internal fun scrubAnnotations(forColumn: String) {
        columnNotNull.remove(forColumn)
        columnAutoIncrement.remove(forColumn)
        columnSize.remove(forColumn)
        columnDefault.remove(forColumn)
    }
}

internal fun MigratableTableMixin.asMigrationTable(): MigrationTable {
    val new = MigrationTable(self.tableName, self.alias, self.catalog, self.schema)
    new.columnNotNull.addAll(columnNotNull)
    new.columnAutoIncrement.addAll(columnAutoIncrement)
    new.columnSize.putAll(columnSize)
    new.columnDefault.putAll(columnDefault)
    new.constraints.putAll(constraints)
    for (col in self.columns) {
        val newCol = new.registerColumn(col.name, col.sqlType)
        if(col in self.primaryKeys) {
            with(new) {
                newCol.primaryKey()
            }
        }
    }
    return new
}

private fun TableConstraintExpression.reverse(
    copy: MigrationTable,
    otherTables: (TableReferenceExpression) -> MigrationTable
): Constraint {
    return when (this) {
        is UniqueTableConstraintExpression -> UniqueConstraint(
            across = across.mapNotNull { col -> copy.columns.find { it.name == col.name } }
        )
        is CheckTableConstraintExpression -> CheckConstraint(
            condition = condition
        )
        is ForeignKeyTableConstraintExpression -> {
            val other = otherTables(otherTable)
            ForeignKeyConstraint(
                to = other,
                correspondence = correspondence.entries.associate { entry ->
                    copy.columns.find { it.name == entry.key.name }!! to other.columns.find { it.name == entry.value.name }!!
                }
            )
        }
        else -> throw IllegalArgumentException()
    }
}