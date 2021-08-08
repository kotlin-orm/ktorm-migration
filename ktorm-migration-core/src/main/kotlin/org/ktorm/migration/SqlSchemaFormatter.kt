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
import org.ktorm.expression.SqlExpression
import org.ktorm.expression.SqlFormatter
import org.ktorm.expression.TableExpression

/**
 * Subclass of [SqlFormatter] that is able to write information about migrations.
 *
 * @property database the current database object used to obtain metadata such as identifier quote string.
 * @property beautifySql mark if we should output beautiful SQL strings with line-wrapping and indentation.
 * @property indentSize the indent size.
 * @property sql return the executable SQL string after the visit completes.
 * @property parameters return the SQL's execution parameters after the visit completes.
 */
public abstract class SqlSchemaFormatter(
    database: Database,
    beautifySql: Boolean,
    indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when(expr){
            is CreateSchemaExpression -> visitCreateSchema(expr)
            is DropSchemaExpression -> visitDropSchema(expr)
            is TableReferenceExpression -> visitTableReference(expr)
            is CreateTableExpression -> visitCreateTable(expr)
            is DropTableExpression -> visitDropTable(expr)
            is TruncateTableExpression -> visitTruncateTable(expr)
            is AlterTableAddExpression -> visitAlterTableAdd(expr)
            is AlterTableDropColumnExpression -> visitAlterTableDropColumn(expr)
            is AlterTableModifyColumnExpression -> visitAlterTableModifyColumn(expr)
            is AlterTableSetDefaultExpression -> visitAlterTableSetDefault(expr)
            is AlterTableDropDefaultExpression -> visitAlterTableDropDefault(expr)
            is AlterTableAddConstraintExpression -> visitAlterTableAddConstraint(expr)
            is AlterTableDropConstraintExpression -> visitAlterTableDropConstraint(expr)
            is CreateIndexExpression -> visitCreateIndex(expr)
            is DropIndexExpression -> visitDropIndex(expr)
            is CreateViewExpression -> visitCreateView(expr)
            is DropViewExpression -> visitDropView(expr)
            is ColumnDeclarationExpression<*> -> visitColumnDeclaration(expr)
            is TableConstraintExpression -> visitTableConstraint(expr)
            else -> super.visit(expr)
        }
    }

    protected open fun visitTableConstraint(expr: TableConstraintExpression): TableConstraintExpression {
        return when(expr){
            is ForeignKeyTableConstraintExpression -> visitForeignKeyTableConstraint(expr)
            is CheckTableConstraintExpression -> visitCheckTableConstraint(expr)
            is UniqueTableConstraintExpression -> visitUniqueTableConstraint(expr)
            is PrimaryKeyTableConstraintExpression -> visitPrimaryKeyTableConstraint(expr)
            else -> super.visit(expr) as TableConstraintExpression
        }
    }

    protected open fun visitCreateSchema(expr: CreateSchemaExpression): CreateSchemaExpression {
        writeKeyword("create schema ")
        write(expr.name.quoted)
        return expr
    }
    protected open fun visitDropSchema(expr: DropSchemaExpression): DropSchemaExpression {
        writeKeyword("drop schema ")
        write(expr.name.quoted)
        return expr
    }

    protected open fun visitCreateTable(expr: CreateTableExpression): CreateTableExpression {
        writeKeyword("create table ")
        if(expr.ifNotExists) {
            writeKeyword("if not exists")
        }
        visitTableReference(expr.name)

        write("(")
        var first = true
        for(col in expr.columns){
            if(first) first = false
            else write(", ")
            visitColumnDeclaration(col)
        }
        for(constraint in expr.constraints.entries){
            writeKeyword(", constraint ")
            write(constraint.key)
            write(" ")
            visit(constraint.value)
        }
        write(") ")

        return expr
    }

    protected open fun visitDropTable(expr: DropTableExpression): DropTableExpression {
        writeKeyword("drop table ")
        visitTableReference(expr.table)
        return expr
    }

    protected open fun visitTruncateTable(expr: TruncateTableExpression): TruncateTableExpression {
        writeKeyword("truncate table ")
        visitTableReference(expr.table)
        return expr
    }
    protected open fun visitAlterTableAdd(expr: AlterTableAddExpression): AlterTableAddExpression {
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        writeKeyword(" add ")
        visitColumnDeclaration(expr.column)
        return expr
    }
    protected open fun visitAlterTableDropColumn(expr: AlterTableDropColumnExpression): AlterTableDropColumnExpression {
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        writeKeyword(" drop column ")
        write(expr.column.name.quoted)
        return expr
    }
    protected open fun visitAlterTableModifyColumn(expr: AlterTableModifyColumnExpression): AlterTableModifyColumnExpression {
        // TODO: This syntax is basically completely specific to the database in question.
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        writeKeyword(" alter column ")
        write(expr.column.name.quoted)
        write(" ")
        writeKeyword(expr.newType.typeName)
        if(expr.size != null) {
            write("(")
            write(expr.size.toString())
            write(")")
        }
        if(expr.notNull){
            writeKeyword(" not null")
        }
        return expr
    }
    protected open fun visitAlterTableAddConstraint(expr: AlterTableAddConstraintExpression): AlterTableAddConstraintExpression {
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        writeKeyword(" add constraint ")
        write(expr.constraintName)
        write(" ")
        visit(expr.tableConstraint)
        return expr
    }
    protected open fun visitAlterTableDropConstraint(expr: AlterTableDropConstraintExpression): AlterTableDropConstraintExpression {
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        // TODO: MySql has a custom syntax, unfortunately, where instead of using 'constraint', you use the type of the constraint
        writeKeyword(" drop constraint ")
        write(expr.constraintName)
        return expr
    }
    protected open fun visitCreateIndex(expr: CreateIndexExpression): CreateIndexExpression {
        writeKeyword("create index ")
        write(expr.name.quoted)
        writeKeyword(" on ")
        visitTableReference(expr.on)
        writeKeyword(" (")
        var first = true
        for(col in expr.columns){
            if(first) first = false
            else write(", ")
            write(col.name.quoted)
        }
        write(")")
        return expr
    }
    protected open fun visitDropIndex(expr: DropIndexExpression): DropIndexExpression {
        writeKeyword("drop index ")
        write(expr.name.quoted)
        writeKeyword(" on ")
        visitTableReference(expr.on)
        return expr
    }
    protected open fun visitCreateView(expr: CreateViewExpression): CreateViewExpression {
        if(expr.orReplace){
            writeKeyword("create or replace view ")
        } else {
            writeKeyword("create view ")
        }
        visitTableReference(expr.name)
        writeKeyword(" as ")
        visitSelect(expr.query)
        return expr
    }
    protected open fun visitDropView(expr: DropViewExpression): DropViewExpression {
        writeKeyword("drop view ")
        visitTableReference(expr.name)
        return expr
    }

    protected open fun visitColumnDeclaration(expr: ColumnDeclarationExpression<*>): ColumnDeclarationExpression<*> {
        write(expr.name.quoted)
        write(" ")
        writeKeyword(expr.sqlType.typeName)
        if(expr.size != null) {
            write("(")
            write(expr.size.toString())
            write(")")
        }
        if(expr.notNull){
            writeKeyword(" not null")
        }
        if(expr.default != null){
            writeKeyword(" default ")
            visitScalar(expr.default)
        }
        if(expr.autoIncrement) TODO("Auto increment is not supported by the general formatter.")
        return expr
    }
    protected open fun visitForeignKeyTableConstraint(expr: ForeignKeyTableConstraintExpression): ForeignKeyTableConstraintExpression {
        writeKeyword("foreign key (")
        val orderedEntries = expr.correspondence.entries.toList()
        var first = true
        for(col in orderedEntries){
            if(first) first = false
            else write(", ")
            write(col.key.name.quoted)
        }
        writeKeyword(") references ")
        visitTableReference(expr.otherTable)
        write("(")
        first = true
        for(col in orderedEntries){
            if(first) first = false
            else write(", ")
            write(col.value.name.quoted)
        }
        write(")")
        return expr
    }
    protected open fun visitCheckTableConstraint(expr: CheckTableConstraintExpression): CheckTableConstraintExpression {
        writeKeyword("check (")
        visitScalar(expr.condition)
        write(")")
        return expr
    }
    protected open fun visitUniqueTableConstraint(expr: UniqueTableConstraintExpression): UniqueTableConstraintExpression {
        writeKeyword("unique (")
        var first = true
        for(col in expr.across){
            if(first) first = false
            else write(", ")
            write(col.name.quoted)
        }
        write(")")
        return expr
    }
    protected open fun visitPrimaryKeyTableConstraint(expr: PrimaryKeyTableConstraintExpression): PrimaryKeyTableConstraintExpression {
        writeKeyword("primary key (")
        var first = true
        for(col in expr.across){
            if(first) first = false
            else write(", ")
            write(col.name.quoted)
        }
        write(")")
        return expr
    }

    protected open fun visitTableReference(expr: TableReferenceExpression): SqlExpression {
        return visitTable(
            TableExpression(
                name = expr.name,
                tableAlias = null,
                catalog = expr.catalog,
                schema = expr.schema,
                isLeafNode = expr.isLeafNode,
                extraProperties = expr.extraProperties
            )
        )
    }

    protected open fun visitAlterTableSetDefault(expr: AlterTableSetDefaultExpression): SqlExpression {
        // TODO: This is unique across every database, H2 and MS Access use this though - SQLite has zero support
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        writeKeyword(" alter column ")
        write(expr.column.name.quoted)
        writeKeyword(" set default ")
        visitScalar(expr.default)
        return expr
    }
    protected open fun visitAlterTableDropDefault(expr: AlterTableDropDefaultExpression): AlterTableDropDefaultExpression {
        // TODO: Closer to universal, only mysql has this problem - SQLite has zero support
        writeKeyword("alter table ")
        visitTableReference(expr.table)
        writeKeyword(" alter column ")
        write(expr.column.name.quoted)
        writeKeyword(" drop default")
        return expr
    }
}
