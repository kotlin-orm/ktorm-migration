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
import org.ktorm.schema.*

/**
 * Represents a constraint on an SQL Table.
 */
public abstract class Constraint() {
    /**
     * Checks if the constraints are equivalent in terms of migrations.
     */
    public abstract infix fun migrationEqual(other: Constraint): Boolean

    /**
     * Returns a [TableConstraintExpression] representing the definition of this constraint.
     */
    public abstract fun asExpression(): TableConstraintExpression
}

/**
 * A unique constraint, which ensures that every row has a different set of values for the given columns.
 */
public data class UniqueConstraint(val across: List<Column<*>>) : Constraint() {
    override fun migrationEqual(other: Constraint): Boolean =
        other is UniqueConstraint && this.across.zip(other.across) { a, b -> a.name == b.name }.all { it }

    override fun asExpression(): UniqueTableConstraintExpression = UniqueTableConstraintExpression(
        across = across.map { it.asReferenceExpression() }
    )
}

/**
 * A check constraint, which ensures that inserted or updated rows adhere to the given [condition].
 */
public data class CheckConstraint(val condition: ScalarExpression<Boolean>) : Constraint() {
    override fun migrationEqual(other: Constraint): Boolean =
        other is CheckConstraint && this.condition == other.condition

    override fun asExpression(): CheckTableConstraintExpression = CheckTableConstraintExpression(condition)
}

/**
 * A foreign key constraint, which ensures that the columns in [correspondence] map to the target table.
 * If the target row is modified, [onUpdate] is triggered.
 * If the target row is deleted, [onDelete] is triggered.
 */
public data class ForeignKeyConstraint(
    val to: BaseTable<*>,
    val correspondence: Map<Column<*>, Column<*>>,
    val onUpdate: OnModification = OnModification.Cascade,
    val onDelete: OnModification = OnModification.Error,
) : Constraint() {

    /**
     * Represents the different ways of handling a target row modification.
     */
    public enum class OnModification {
        /**
         * Throw an error, do not proceed
         */
        Error,

        /**
         * Corresponding rows are updated or deleted in the referencing table when that row is updated or deleted in the parent table.
         */
        Cascade,

        /**
         * References to the target row are set to null.
         */
        SetNull,

        /**
         * References to the target row are set to the default value.
         */
        SetDefault
    }

    override fun migrationEqual(other: Constraint): Boolean = other is ForeignKeyConstraint
            && this.correspondence.entries.zip(other.correspondence.entries) { a, b ->
        a.key.name == b.key.name && a.value.name == b.value.name
    }.all { it }

    override fun asExpression(): ForeignKeyTableConstraintExpression = ForeignKeyTableConstraintExpression(
        otherTable = to.asReferenceExpression(),
        correspondence = correspondence.entries.associate {
            it.key.asReferenceExpression() to it.value.asReferenceExpression()
        },
        onUpdate = onUpdate,
        onDelete = onDelete,
    )
}
