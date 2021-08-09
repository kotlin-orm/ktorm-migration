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

import org.ktorm.schema.Column
import org.ktorm.schema.ReferenceBinding

internal fun MigratableTableMixin.createTable(): CreateTableExpression {
    val tableConstraints = HashMap<String, TableConstraintExpression>()
    tableConstraints["${this.self.catalog}_${this.self.schema}_${this.self.tableName}_pk"] = PrimaryKeyTableConstraintExpression(
        across = this.self.primaryKeys.map { it.asReferenceExpression() }
    )
    for ((name, constraint) in constraints) {
        tableConstraints[name] = constraint.asExpression()
    }
    return CreateTableExpression(
        name = this.self.asReferenceExpression(),
        columns = this.self.columns.map { it.asDeclarationExpression() },
        constraints = tableConstraints
    )
}

/**
 * Could be used in the future to automatically enable foreign key constraints from [org.ktorm.schema.Table.references] calls.
 */
@Suppress("unused")
private fun handleReferenceBinding(
    tableConstraints: HashMap<String, TableConstraintExpression>,
    it: Column<*>,
    binding: ReferenceBinding
) {
    tableConstraints["FK_" + it.name] = (ForeignKeyTableConstraintExpression(
        otherTable = binding.referenceTable.asReferenceExpression(),
        correspondence = mapOf(
            it.asReferenceExpression() to (binding.referenceTable.primaryKeys.singleOrNull()
                ?: throw IllegalArgumentException("Foreign key cannot be defined this way if there are multiple primary keys on the other")).asReferenceExpression()
        )
    ))
}