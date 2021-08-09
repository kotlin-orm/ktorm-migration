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
 * Migrates the database forwards to the given migration.
 */
public fun Database.migrate(to: Migration){
    enableMigrationTracking()
    fun internal(to: Migration){
        if(completedMigration(to)) return
        for (dep in to.dependsOn) {
            internal(to)
        }
        useTransaction {
            to.actions.forEach { it.migrate(this) }
            markCompleted(to)
        }
    }
    internal(to)
}

/**
 * Migrates the database back to the given migration.
 * Requires the latest migration as an input because dependant migrations are not tracked.
 */
public fun Database.rollback(to: Migration, latest: Migration){
    enableMigrationTracking()
    val dependees = HashMap<Migration, ArrayList<Migration>>()
    fun traverse(migration: Migration) {
        for(dep in migration.dependsOn){
            dependees.getOrPut(dep) { ArrayList() }.add(migration)
            traverse(dep)
        }
    }
    traverse(latest)

    fun internalRollback(to: Migration) {
        if(!completedMigration(to)) return
        for (dep in dependees[to] ?: listOf()) {
            internalRollback(to)
        }
        useTransaction {
            to.actions.asReversed().forEach { it.undo(this) }
            markUncomplete(to)
        }
    }
    internalRollback(to)
}