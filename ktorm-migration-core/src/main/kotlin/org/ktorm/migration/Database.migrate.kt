package org.ktorm.migration

import org.ktorm.database.Database
import org.ktorm.entity.find

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