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

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column

/**
 * A mixin that enables migrations for a table.
 */
public interface MigratableTableMixin {
    /**
     * A reference to a [BaseTable].  Used because an interface can't require the inheritance of a super class.
     */
    public val self: BaseTable<*>

    /**
     * Stores which columns have a "not null" restriction.
     */
    public val columnNotNull: HashSet<String>

    /**
     * Stores which columns have auto increment enabled.
     */
    public val columnAutoIncrement: HashSet<String>

    /**
     * Stores the sizes of each column for SQL types like VARCHAR.
     */
    public val columnSize: HashMap<String, Int>

    /**
     * Stores the default value expression for each column.
     */
    public val columnDefault: HashMap<String, ScalarExpression<*>>

    /**
     * Stores the related constraints by name.
     */
    public val constraints: HashMap<String, Constraint>

    /**
     * Enables auto increment on a column.
     */
    public fun <C : Number> Column<C>.autoIncrement(autoIncrement: Boolean = true): Column<C> {
        if(autoIncrement) columnAutoIncrement.add(name) else columnAutoIncrement.remove(name)
        return this
    }

    /**
     * Sets the default value for a column.
     */
    public fun <C : Any> Column<C>.default(value: C): Column<C>
        = default(ArgumentExpression(value, sqlType))

    /**
     * Sets the default value for a column as an expression.
     */
    public fun <C : Any> Column<C>.default(expression: ScalarExpression<C>? = null): Column<C> {
        if(expression != null)
        columnDefault[name] = expression
        else
            columnDefault.remove(name)
        return this
    }

    /**
     * Adds a constraint that this column must be unique across rows.
     */
    public fun <C : Any> Column<C>.unique(): Column<C> {
        constraints["${self.catalog}_${self.schema}_${self.tableName}_unique_$name"] = UniqueConstraint(across = listOf(this))
        return this
    }

    /**
     * Adds a foreign key constraint to this column.
     */
    public fun <C : Any> Column<C>.foreignKey(
        to: BaseTable<*>,
        @Suppress("UNCHECKED_CAST") on: Column<C> = to.primaryKeys.singleOrNull() as? Column<C>
            ?: throw IllegalArgumentException("Foreign key cannot be defined this way if there are multiple primary keys on the other")
    ): Column<C> {
        constraints["${self.catalog}_${self.schema}_${self.tableName}_fk_$name"] = ForeignKeyConstraint(
            to = to,
            correspondence = mapOf(this to on)
        )
        return this
    }

    /**
     * Restricts a column from being set to null.
     */
    public fun <C : Any> Column<C>.notNull(notNull: Boolean = true): Column<C> {
        if(notNull) columnNotNull.add(name) else columnNotNull.remove(name)
        return this
    }

    /**
     * Unrestricts a column from being set to null.
     */
    public fun <C : Any> Column<C>.nullable(): Column<C> {
        columnNotNull.remove(name)
        return this
    }

    /**
     * Sets the size of a column.
     */
    public fun Column<String>.size(set: Int): Column<String> {
        columnSize[name] = set
        return this
    }
}

internal val MigratableTableMixin.structuralInfo: Map<String, Any?> get() {
    return mapOf(
        "tableName" to self.tableName,
        "catalog" to self.catalog,
        "schema" to self.schema,
        "primaryKeys" to self.primaryKeys,
        "columns" to self.columns,
        "columnNotNull" to columnNotNull,
        "columnAutoIncrement" to columnAutoIncrement,
        "columnSize" to columnSize,
        "columnDefault" to columnDefault,
        "constraints" to constraints
    )
}

/**
 * Returns the default value expression for a column.
 */
@Suppress("UNCHECKED_CAST")
public val <C : Any> Column<C>.default: ScalarExpression<C>?
    get() = (table as MigratableTableMixin).columnDefault[name] as? ScalarExpression<C>

/**
 * Returns whether a column is restricted from being set to NULL.
 */
public val <C : Any> Column<C>.notNull: Boolean get() = this.name in (table as MigratableTableMixin).columnNotNull

/**
 * Returns the size of a column.
 */
public val <C : Any> Column<C>.size: Int?
    get() = (table as MigratableTableMixin).columnSize[name]

/**
 * Returns whether a column has auto increment enabled.
 */
public val <C : Any> Column<C>.autoIncrement: Boolean get() = this.name in (table as MigratableTableMixin).columnAutoIncrement