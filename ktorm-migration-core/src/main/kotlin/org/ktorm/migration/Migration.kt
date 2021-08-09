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

import org.ktorm.database.Database

/**
 * A migration, or change in schema.
 * These are reversible.
 *
 * Migrations are run in a transaction.
 */
public interface Migration {
    /**
     * A list of other migrations that need to be run before this one.
     */
    public val dependsOn: List<Migration>

    /**
     * A list of actions to perform when migrating.
     */
    public val actions: List<MigrationAction>

    /**
     * The qualified name of the migration, automatically populated but overrideable.
     * You probably don't need to override this if you're using migrations as intended.
     */
    public val qualifiedName: String get() = this::class.qualifiedName!!

    /**
     * The simple name of the migration, automatically populated but overrideable.
     * You probably don't need to override this if you're using migrations as intended.
     */
    public val simpleName: String get() = qualifiedName.substringAfterLast('.')
}

/**
 * An action to perform in a migration.
 */
public sealed class MigrationAction {
    /**
     * Perform the action on the given [database].
     */
    public abstract fun migrate(database: Database)

    /**
     * Undo the action on the given [database].
     */
    public abstract fun undo(database: Database)

    /**
     * Modify the tables in the [TableRebuilder] so that the state of the tables may be determined correctly.
     * You don't need to do anything here for any kind of migration that doesn't modify schema.
     */
    public abstract fun migrateTables(tables: TableRebuilder)

    /**
     * Modify the tables in the [TableRebuilder] so that the state of the tables may be determined correctly.
     * You don't need to do anything here for any kind of migration that doesn't modify schema.
     */
    public abstract fun undoTables(tables: TableRebuilder)

    /**
     * A custom, Kotlin-based migration.
     */
    @Suppress("unused")
    public abstract class Kotlin : MigrationAction() {
        override fun migrate(database: Database) {}
        override fun undo(database: Database) {}
        override fun migrateTables(tables: TableRebuilder) {}
        override fun undoTables(tables: TableRebuilder) {}
    }

    /**
     * A reversible SQL action that modifies the schema.
     */
    public data class ReversibleSql(public val expression: ReversibleMigrationExpression) : MigrationAction() {
        override fun migrate(database: Database) {
            database.executeUpdate(expression.migrate())
        }

        override fun undo(database: Database) {
            database.executeUpdate(expression.undo())
        }

        override fun migrateTables(tables: TableRebuilder) {
            tables.apply(expression.migrate())
        }
        override fun undoTables(tables: TableRebuilder) {
            tables.apply(expression.undo())
        }
    }
}
