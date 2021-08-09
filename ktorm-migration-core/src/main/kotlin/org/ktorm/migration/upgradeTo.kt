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

import java.lang.Appendable

/**
 * Generates source code for a migration, given some updates.
 */
internal fun List<ReversibleMigrationExpression>.generateMigrationSource(
    name: String,
    packageName: String,
    dependsOn: List<Migration>,
    out: Appendable
) {
    val regen = SourceRegenerator()
    regen.visit(this.map { MigrationAction.ReversibleSql(it) })
    out.appendLine("package $packageName")
    for (dep in dependsOn) {
        out.appendLine("import ${dep.qualifiedName}")
    }
    out.appendLine("import org.ktorm.migration.Migration")
    out.appendLine("import org.ktorm.migration.MigrationAction")
    for (import in regen.imports) {
        out.appendLine("import $import")
    }
    out.appendLine()
    out.appendLine("public object $name: Migration {")
    out.appendLine("    override val dependsOn: List<Migration> = listOf(${dependsOn.joinToString { it.simpleName }})")
    out.appendLine("    override val actions: List<MigrationAction> = ${regen.builder}")
    out.appendLine("}")
}

/**
 * Returns the needed actions to upgrade from one collection of tables to another.
 */
internal fun Collection<MigratableTableMixin>.upgradeTo(target: Collection<MigratableTableMixin>): List<ReversibleMigrationExpression> {

    val changes = ArrayList<ReversibleMigrationExpression>()
    val finalChanges = ArrayList<ReversibleMigrationExpression>()

    diff(
        old = this.mapNotNull { it.self.schema }.toSet(),
        new = target.mapNotNull { it.self.schema }.toSet(),
        compare = { a, b -> a == b },
        add = { changes.add(CreateSchemaExpression(it)) },
        same = { _, _ -> },
        remove = { finalChanges.add(-CreateSchemaExpression(it)) }
    )
    diff(
        old = this,
        new = target,
        compare = { a, b -> a.self.asReferenceExpression() == b.self.asReferenceExpression() },
        add = {
            val w = it.withoutExternalDependencies()
            changes.add(w.createTable())
            finalChanges.addAll(w.upgradeTo(it))
        },
        same = { old, new ->
            for (change in old.upgradeTo(new)) {
                if (change is AlterTableAddConstraintExpression) finalChanges.add(change)
                else changes.add(change)
            }
        },
        remove = {
            changes.add(it.createTable())
        }
    )
    return changes + finalChanges.asReversed()
}

internal fun MigratableTableMixin.withoutExternalDependencies(): MigrationTable {
    return this.asMigrationTable().copy().apply {
        for (entry in this.constraints) {
            if (entry.value is ForeignKeyConstraint) {
                this.constraints.remove(entry.key)
            }
        }
    }
}

/**
 * Returns the needed actions to upgrade from one table to another.
 */
internal fun MigratableTableMixin.upgradeTo(target: MigratableTableMixin): List<ReversibleMigrationExpression> {
    val changes = ArrayList<ReversibleMigrationExpression>()

    diff(
        old = this.self.columns,
        new = target.self.columns,
        compare = { l, r -> l.name == r.name },
        add = {
            changes += AlterTableAddExpression(
                table = this.self.asReferenceExpression(),
                column = it.asDeclarationExpression()
            )
        },
        same = { old, new ->
            if (old.sqlType != new.sqlType
                || old.notNull != new.notNull
                || old.size != new.size
            )
                changes += AlterTableModifyColumnReversible(
                    table = this.self.asReferenceExpression(),
                    column = old.asReferenceExpression(),
                    oldType = old.sqlType,
                    oldSize = old.size,
                    oldNotNull = old.notNull,
                    newType = new.sqlType,
                    newSize = new.size,
                    newNotNull = new.notNull
                )
            val newDefault = new.default
            if (old.default != newDefault) {
                changes += AlterTableDefaultReversible(
                    table = this.self.asReferenceExpression(),
                    column = old.asReferenceExpression(),
                    oldDefault = old.default,
                    newDefault = newDefault,
                )
            }
        },
        remove = {
            changes += -AlterTableAddExpression(
                table = this.self.asReferenceExpression(),
                column = it.asDeclarationExpression()
            )
        }
    )

    diff(
        old = this.constraints.entries,
        new = target.constraints.entries,
        compare = { a, b -> a.key == b.key },
        add = {
            changes.add(
                AlterTableAddConstraintExpression(
                    table = this.self.asReferenceExpression(),
                    constraintName = it.key,
                    tableConstraint = it.value.asExpression()
                )
            )
        },
        same = { a, b ->
            if (!(a.value migrationEqual b.value)) {
                changes.add(
                    -AlterTableAddConstraintExpression(
                        table = this.self.asReferenceExpression(),
                        constraintName = b.key,
                        tableConstraint = b.value.asExpression()
                    )
                )
                changes.add(
                    AlterTableAddConstraintExpression(
                        table = this.self.asReferenceExpression(),
                        constraintName = b.key,
                        tableConstraint = b.value.asExpression()
                    )
                )
            }
        },
        remove = {
            changes.add(
                -AlterTableAddConstraintExpression(
                    table = this.self.asReferenceExpression(),
                    constraintName = it.key,
                    tableConstraint = it.value.asExpression()
                )
            )
        }
    )

    return changes
}

private inline fun <T> diff(
    old: Collection<T>,
    new: Collection<T>,
    compare: (T, T) -> Boolean,
    add: (T) -> Unit,
    same: (T, T) -> Unit,
    remove: (T) -> Unit
) {
    for (o in old) {
        val updated = new.find { n -> compare(o, n) }
        if (updated != null) {
            same(o, updated)
        } else {
            remove(o)
        }
    }
    for (n in new) {
        if (old.any { o -> compare(o, n) }) continue
        add(n)
    }
}