package org.ktorm.testmigrations

import org.ktorm.migration.Migration
import org.ktorm.migration.MigrationAction
import org.ktorm.schema.LongSqlType
import org.ktorm.migration.ForeignKeyTableConstraintExpression
import org.ktorm.migration.ColumnDeclarationExpression
import org.ktorm.migration.AlterTableAddConstraintExpression
import org.ktorm.migration.TableReferenceExpression
import org.ktorm.migration.ColumnReferenceExpression
import org.ktorm.schema.IntSqlType
import org.ktorm.expression.ArgumentExpression
import org.ktorm.schema.VarcharSqlType
import org.ktorm.migration.PrimaryKeyTableConstraintExpression
import org.ktorm.schema.LocalDateSqlType
import org.ktorm.migration.UniqueTableConstraintExpression
import org.ktorm.migration.MigrationAction.ReversibleSql
import org.ktorm.migration.CreateSchemaExpression
import org.ktorm.migration.CreateTableExpression

public object Migration0001 : Migration {
    override val dependsOn: List<Migration> = listOf()
    override val actions: List<MigrationAction> = listOf(
        ReversibleSql(
            expression = CreateSchemaExpression(
                name = "company",
                isLeafNode = false,
                extraProperties = mapOf()
            )
        ),
        ReversibleSql(
            expression = CreateTableExpression(
                name = TableReferenceExpression(
                    name = "t_department",
                    catalog = null,
                    schema = null,
                    isLeafNode = true,
                    extraProperties = mapOf()
                ),
                columns = listOf(
                    ColumnDeclarationExpression(
                        name = "id",
                        sqlType = IntSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "name",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "location",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = ArgumentExpression(
                            value = "Unimportant",
                            sqlType = VarcharSqlType,
                            isLeafNode = true,
                            extraProperties = mapOf()
                        ),
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "mixedCase",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    )
                ),
                constraints = mapOf(
                    "null_null_t_department_pk" to PrimaryKeyTableConstraintExpression(
                        across = listOf(
                            ColumnReferenceExpression(
                                name = "id",
                                isLeafNode = true,
                                extraProperties = mapOf()
                            )
                        ), isLeafNode = false, extraProperties = mapOf()
                    )
                ),
                ifNotExists = false,
                isLeafNode = false,
                extraProperties = mapOf()
            )
        ),
        ReversibleSql(
            expression = CreateTableExpression(
                name = TableReferenceExpression(
                    name = "t_employee",
                    catalog = null,
                    schema = null,
                    isLeafNode = true,
                    extraProperties = mapOf()
                ),
                columns = listOf(
                    ColumnDeclarationExpression(
                        name = "id",
                        sqlType = IntSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "name",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "job",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = ArgumentExpression(
                            value = "Minion",
                            sqlType = VarcharSqlType,
                            isLeafNode = true,
                            extraProperties = mapOf()
                        ),
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "manager_id",
                        sqlType = IntSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "hire_date",
                        sqlType = LocalDateSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "salary",
                        sqlType = LongSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "department_id",
                        sqlType = IntSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    )
                ),
                constraints = mapOf(
                    "null_null_t_employee_unique_name" to UniqueTableConstraintExpression(
                        across = listOf(
                            ColumnReferenceExpression(
                                name = "name",
                                isLeafNode = true,
                                extraProperties = mapOf()
                            )
                        ), isLeafNode = false, extraProperties = mapOf()
                    ),
                    "null_null_t_employee_pk" to PrimaryKeyTableConstraintExpression(
                        across = listOf(
                            ColumnReferenceExpression(
                                name = "id",
                                isLeafNode = true,
                                extraProperties = mapOf()
                            )
                        ), isLeafNode = false, extraProperties = mapOf()
                    )
                ),
                ifNotExists = false,
                isLeafNode = false,
                extraProperties = mapOf()
            )
        ),
        ReversibleSql(
            expression = CreateTableExpression(
                name = TableReferenceExpression(
                    name = "t_customer",
                    catalog = null,
                    schema = "company",
                    isLeafNode = true,
                    extraProperties = mapOf()
                ),
                columns = listOf(
                    ColumnDeclarationExpression(
                        name = "id",
                        sqlType = IntSqlType,
                        size = null,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "name",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "email",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "phone_number",
                        sqlType = VarcharSqlType,
                        size = 128,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    ),
                    ColumnDeclarationExpression(
                        name = "address",
                        sqlType = VarcharSqlType,
                        size = 512,
                        notNull = false,
                        default = null,
                        autoIncrement = false,
                        isLeafNode = false,
                        extraProperties = mapOf()
                    )
                ),
                constraints = mapOf(
                    "null_company_t_customer_pk" to PrimaryKeyTableConstraintExpression(
                        across = listOf(
                            ColumnReferenceExpression(
                                name = "id",
                                isLeafNode = true,
                                extraProperties = mapOf()
                            )
                        ), isLeafNode = false, extraProperties = mapOf()
                    )
                ),
                ifNotExists = false,
                isLeafNode = false,
                extraProperties = mapOf()
            )
        ),
        ReversibleSql(
            expression = AlterTableAddConstraintExpression(
                table = TableReferenceExpression(
                    name = "t_employee",
                    catalog = null,
                    schema = null,
                    isLeafNode = true,
                    extraProperties = mapOf()
                ),
                constraintName = "null_null_t_employee_fk_department_id",
                tableConstraint = ForeignKeyTableConstraintExpression(
                    otherTable = TableReferenceExpression(
                        name = "t_department",
                        catalog = null,
                        schema = null,
                        isLeafNode = true,
                        extraProperties = mapOf()
                    ),
                    correspondence = mapOf(
                        ColumnReferenceExpression(
                            name = "department_id",
                            isLeafNode = true,
                            extraProperties = mapOf()
                        ) to ColumnReferenceExpression(name = "id", isLeafNode = true, extraProperties = mapOf())
                    ),
                    isLeafNode = false,
                    extraProperties = mapOf()
                ),
                isLeafNode = false,
                extraProperties = mapOf()
            )
        )
    )
}
