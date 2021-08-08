package org.ktorm.migration

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column

public interface MigrateTableMixin {
    public val self: BaseTable<*>
    public val columnNotNull: HashSet<String>
    public val columnAutoIncrement: HashSet<String>
    public val columnSize: HashMap<String, Int>
    public val columnDefault: HashMap<String, ScalarExpression<*>>
    public val constraints: HashMap<String, Constraint>

//    public fun <C : Any> Column<C>.autoIncrement(autoIncrement: Boolean = true): Column<C> {
//        if(autoIncrement) columnAutoIncrement.add(name) else columnAutoIncrement.remove(name)
//        return this
//    }

    public fun <C : Any> Column<C>.default(value: C): Column<C>
        = default(ArgumentExpression(value, sqlType))
    public fun <C : Any> Column<C>.default(expression: ScalarExpression<C>? = null): Column<C> {
        if(expression != null)
        columnDefault[name] = expression
        else
            columnDefault.remove(name)
        return this
    }

    public fun <C : Any> Column<C>.unique(): Column<C> {
        constraints["${self.catalog}_${self.schema}_${self.tableName}_unique_$name"] = UniqueConstraint(across = listOf(this))
        return this
    }

    public fun <C : Any> Column<C>.foreignKey(
        to: BaseTable<*>,
        @Suppress("UNCHECKED_CAST") on: Column<C> = to.primaryKeys.singleOrNull() as? Column<C>
            ?: throw IllegalArgumentException("Foreign key cannot be defined this way if there are multiple primary keys on the other")
    ): Column<C> {
        constraints["${self.catalog}_${self.schema}_${self.tableName}_fk_$name"] = ForeignKeyConstraint(
            to = to,
            correspondence = mapOf(this to on)
        )
        return this
    }
    public fun <C : Any> Column<C>.notNull(notNull: Boolean = true): Column<C> {
        if(notNull) columnNotNull.add(name) else columnNotNull.remove(name)
        return this
    }
    public fun <C : Any> Column<C>.nullable(): Column<C> {
        columnNotNull.remove(name)
        return this
    }

    public fun <C : Any> Column<C>.size(set: Int): Column<C> {
        columnSize[name] = set
        return this
    }

    public val structuralInfo: Map<String, Any?> get() {
        return mapOf(
            "tableName" to self.tableName,
            "catalog" to self.catalog,
            "schema" to self.schema,
            "primaryKeys" to self.primaryKeys,
            "columns" to self.columns,
            "columnNotNull" to columnNotNull,
            "columnAutoIncrement" to columnAutoIncrement,
            "columnSize" to columnSize,
            "columnDefault" to columnDefault,
            "constraints" to constraints
        )
    }
}

@Suppress("UNCHECKED_CAST")
public val <C : Any> Column<C>.default: ScalarExpression<C>?
    get() = (table as MigrateTableMixin).columnDefault[name] as? ScalarExpression<C>

public val <C : Any> Column<C>.notNull: Boolean get() = this.name in (table as MigrateTableMixin).columnNotNull

public val <C : Any> Column<C>.size: Int?
    get() = (table as MigrateTableMixin).columnSize[name]

public val <C : Any> Column<C>.autoIncrement: Boolean get() = this.name in (table as MigrateTableMixin).columnAutoIncrement