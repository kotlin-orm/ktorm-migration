package org.ktorm.testmigrations

import org.ktorm.testmigrations.Migration0001
import org.ktorm.migration.Migration
import org.ktorm.migration.MigrationAction
import org.ktorm.schema.VarcharSqlType
import org.ktorm.migration.ColumnDeclarationExpression
import org.ktorm.migration.AlterTableAddExpression
import org.ktorm.migration.TableReferenceExpression
import org.ktorm.migration.MigrationAction.ReversibleSql

public object Migration0002 : Migration {
    override val number: Int = 2
    override val dependsOn: List<Migration> = listOf(Migration0001)
    override val actions: List<MigrationAction> = listOf(
        ReversibleSql(
            expression = AlterTableAddExpression(
                table = TableReferenceExpression(
                    name = "t_customer",
                    catalog = null,
                    schema = "company",
                    isLeafNode = true,
                    extraProperties = mapOf()
                ),
                column = ColumnDeclarationExpression(
                    name = "address",
                    sqlType = VarcharSqlType,
                    size = 512,
                    notNull = false,
                    default = null,
                    autoIncrement = false,
                    isLeafNode = false,
                    extraProperties = mapOf()
                ),
                isLeafNode = false,
                extraProperties = mapOf()
            )
        )
    )
}
