package org.ktorm.migration

import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.*

public abstract class Constraint() {
    public abstract infix fun migrationEqual(other: Constraint): Boolean
}

public data class UniqueConstraint(val across: List<Column<*>>) : Constraint() {
    override fun migrationEqual(other: Constraint): Boolean =
        other is UniqueConstraint && this.across.zip(other.across) { a, b -> a.name == b.name }.all { it }
}

public data class CheckConstraint(val condition: ScalarExpression<Boolean>) : Constraint() {
    override fun migrationEqual(other: Constraint): Boolean =
        other is CheckConstraint && this.condition == other.condition
}

public data class ForeignKeyConstraint(
    val to: BaseTable<*>,
    val correspondence: Map<Column<*>, Column<*>>
) : Constraint() {
    override fun migrationEqual(other: Constraint): Boolean = other is ForeignKeyConstraint
            && this.correspondence.entries.zip(other.correspondence.entries) { a, b ->
        a.key.name == b.key.name && a.value.name == b.value.name
    }.all { it }
}

