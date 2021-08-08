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

import org.ktorm.expression.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import kotlin.math.exp

public interface ReversibleMigrationExpression {
    public fun migrate(): SqlExpression
    public fun undo(): SqlExpression
}
public data class ReversedMigrationExpression(val source: ReversibleMigrationExpression): ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = source.undo()
    override fun undo(): SqlExpression = source.migrate()
}
public operator fun ReversibleMigrationExpression.unaryMinus(): ReversedMigrationExpression = ReversedMigrationExpression(this)

// Schemas

public data class CreateSchemaExpression(
    val name: String,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression(), ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = this
    override fun undo(): DropSchemaExpression = DropSchemaExpression(name)
}

public data class DropSchemaExpression(
    val name: String,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

// Indexes

public data class CreateIndexExpression(
    val name: String,
    val on: TableReferenceExpression,
    val columns: List<ColumnReferenceExpression>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression(), ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = this
    override fun undo(): DropIndexExpression = DropIndexExpression(name, on)
}

public data class DropIndexExpression(
    val name: String,
    val on: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()


// Views

public data class CreateViewExpression(
    val name: TableReferenceExpression,
    val query: SelectExpression,
    val orReplace: Boolean = false,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression(), ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = this
    override fun undo(): DropViewExpression = DropViewExpression(name)
}

public data class DropViewExpression(
    val name: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

// Tables

public data class CreateTableExpression(
    val name: TableReferenceExpression,
    val columns: List<ColumnDeclarationExpression<*>>,
    val constraints: Map<String, TableConstraintExpression> = emptyMap(),
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression(), ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = this
    override fun undo(): DropTableExpression = DropTableExpression(name)
}

public data class DropTableExpression(
    val table: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class TruncateTableExpression(
    val table: TableReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableAddExpression(
    val table: TableReferenceExpression,
    val column: ColumnDeclarationExpression<*>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression(), ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = this
    override fun undo(): AlterTableDropColumnExpression = AlterTableDropColumnExpression(table, ColumnReferenceExpression(column.name))
}

public data class AlterTableDropColumnExpression(
    val table: TableReferenceExpression,
    val column: ColumnReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableModifyColumnReversible(
    val table: TableReferenceExpression,
    val column: ColumnReferenceExpression,
    val oldType: SqlType<*>,
    val newType: SqlType<*>,
    val oldSize: Int? = null,
    val newSize: Int? = null,
    val oldNotNull: Boolean = false,
    val newNotNull: Boolean = false
) : ReversibleMigrationExpression {
    override fun migrate(): AlterTableModifyColumnExpression = AlterTableModifyColumnExpression(table, column, newType, newSize, newNotNull)
    override fun undo(): AlterTableModifyColumnExpression = AlterTableModifyColumnExpression(table, column, oldType, oldSize, oldNotNull)
}

public data class AlterTableModifyColumnExpression(
    val table: TableReferenceExpression,
    val column: ColumnReferenceExpression,
    val newType: SqlType<*>,
    val size: Int? = null,
    val notNull: Boolean = false,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression() {
    internal fun asDeclaration(): ColumnDeclarationExpression<*> = ColumnDeclarationExpression(
        name = column.name,
        sqlType = newType,
        size = size,
        notNull = notNull
    )
}

public data class AlterTableDefaultReversible(
    val table: TableReferenceExpression,
    val column: ColumnReferenceExpression,
    val oldDefault: ScalarExpression<*>? = null,
    val newDefault: ScalarExpression<*>? = null
) : ReversibleMigrationExpression {
    private fun make(expression: ScalarExpression<*>?): SqlExpression {
        return if(expression != null) AlterTableSetDefaultExpression(table, column, expression)
        else AlterTableDropDefaultExpression(table, column)
    }
    override fun migrate(): SqlExpression = make(newDefault)
    override fun undo(): SqlExpression = make(oldDefault)
}

public data class AlterTableSetDefaultExpression(
    val table: TableReferenceExpression,
    val column: ColumnReferenceExpression,
    val default: ScalarExpression<*>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableDropDefaultExpression(
    val table: TableReferenceExpression,
    val column: ColumnReferenceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public data class AlterTableAddConstraintExpression(
    val table: TableReferenceExpression,
    val constraintName: String,
    val tableConstraint: TableConstraintExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression(), ReversibleMigrationExpression {
    override fun migrate(): SqlExpression = this
    override fun undo(): SqlExpression = AlterTableDropConstraintExpression(table, constraintName)
}

public data class AlterTableDropConstraintExpression(
    val table: TableReferenceExpression,
    val constraintName: String,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

// Components

public data class ColumnDeclarationExpression<T : Any>(
    val name: String,
    val sqlType: SqlType<T>,
    val size: Int? = null,
    val notNull: Boolean = false,
    val default: ScalarExpression<out Any>? = null,
    val autoIncrement: Boolean = false,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : SqlExpression()

public abstract class TableConstraintExpression() : SqlExpression()
public data class ForeignKeyTableConstraintExpression(
    val otherTable: TableReferenceExpression,
    val correspondence: Map<ColumnReferenceExpression, ColumnReferenceExpression>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class CheckTableConstraintExpression(
    val condition: ScalarExpression<Boolean>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class UniqueTableConstraintExpression(
    val across: List<ColumnReferenceExpression>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class PrimaryKeyTableConstraintExpression(
    val across: List<ColumnReferenceExpression>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : TableConstraintExpression()

public data class TableReferenceExpression(
    val name: String,
    val catalog: String? = null,
    val schema: String? = null,
    override val isLeafNode: Boolean = true,
    override val extraProperties: Map<String, Any> = mapOf(),
): SqlExpression()
public fun BaseTable<*>.asReferenceExpression(): TableReferenceExpression = TableReferenceExpression(name = tableName, catalog = catalog, schema = schema)

public data class ColumnReferenceExpression(
    val name: String,
    override val isLeafNode: Boolean = true,
    override val extraProperties: Map<String, Any> = mapOf(),
): SqlExpression()
public fun Column<*>.asReferenceExpression(): ColumnReferenceExpression = ColumnReferenceExpression(name)