package org.ktorm.migration

import org.ktorm.dsl.QueryRowSet
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ReferenceBinding

internal fun MigrateTableMixin.createTable(): CreateTableExpression {
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

public fun <T: Any> Column<T>.asDeclarationExpression(): ColumnDeclarationExpression<T> {
    return ColumnDeclarationExpression(
        name = name,
        sqlType = sqlType,
        notNull = notNull,
        default = default,
        size = size,
        autoIncrement = autoIncrement
    )
}

public fun Constraint.asExpression(): TableConstraintExpression {
    return when (val constraint = this) {
        is UniqueConstraint -> UniqueTableConstraintExpression(
            across = constraint.across.map { it.asReferenceExpression() }
        )
        is ForeignKeyConstraint -> ForeignKeyTableConstraintExpression(
            otherTable = constraint.to.asReferenceExpression(),
            correspondence = constraint.correspondence.entries.associate {
                it.key.asReferenceExpression() to it.value.asReferenceExpression()
            }
        )
        is CheckConstraint -> CheckTableConstraintExpression(
            condition = constraint.condition
        )
        else -> throw IllegalArgumentException()
    }
}

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