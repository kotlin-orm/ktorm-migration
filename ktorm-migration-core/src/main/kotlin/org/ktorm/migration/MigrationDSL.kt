package org.ktorm.migration

import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.SqlType

public interface MigrationDSL {

    public fun TableReferenceExpression.create(
        setup: CreateTableExpressionBuilder.()->Unit
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(CreateTableExpressionBuilder().apply(setup).build(this))

    public fun TableReferenceExpression.drop(
        columns: List<ColumnDeclarationExpression<*>>,
        constraints: Map<String, TableConstraintExpression> = emptyMap(),
    ): MigrationAction.ReversibleSql = MigrationAction.ReversibleSql(-CreateTableExpression(this, columns, constraints))

    public fun TableReferenceExpression.addColumn(
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

    public fun TableReferenceExpression.dropColumn(
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

    public fun TableReferenceExpression.alterColumn(
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

    public fun TableReferenceExpression.setColumnDefault(
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

    public fun <T: Any> TableReferenceExpression.addConstraint(
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

public class CreateTableExpressionBuilder {
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