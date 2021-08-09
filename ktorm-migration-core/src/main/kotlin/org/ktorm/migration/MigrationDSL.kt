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

import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.SqlType

/**
 * A future feature that will make migrations look nicer.
 * Until we can generate migrations that use it, though, this is hidden.
 */
@Suppress("unused")
internal interface MigrationDSL {

    fun TableReferenceExpression.create(
        setup: CreateTableExpressionBuilder.()->Unit
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(CreateTableExpressionBuilder().apply(setup).build(this))

    fun TableReferenceExpression.drop(
        columns: List<ColumnDeclarationExpression<*>>,
        constraints: Map<String, TableConstraintExpression> = emptyMap(),
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(-CreateTableExpression(this, columns, constraints))

    fun TableReferenceExpression.addColumn(
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

    fun TableReferenceExpression.dropColumn(
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

    fun TableReferenceExpression.alterColumn(
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

    fun TableReferenceExpression.setColumnDefault(
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

    fun <T: Any> TableReferenceExpression.addConstraint(
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

/**
 * A future feature that will make migrations look nicer.
 * Until we can generate migrations that use it, though, this is hidden.
 */
@Suppress("unused", "RedundantVisibilityModifier")
internal class CreateTableExpressionBuilder {
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