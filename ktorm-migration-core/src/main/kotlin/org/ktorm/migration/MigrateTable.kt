package org.ktorm.migration

import org.ktorm.dsl.QueryRowSet
import org.ktorm.entity.Entity
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table
import kotlin.reflect.KClass

public abstract class MigrateTable<E: Entity<E>>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
): Table<E>(tableName, alias, catalog, schema, entityClass), MigrateTableMixin {
    override val self: BaseTable<*>
        get() = this
    override val columnNotNull: HashSet<String> = HashSet()
    override val columnAutoIncrement: HashSet<String> = HashSet()
    override val columnSize: HashMap<String, Int> = HashMap()
    override val columnDefault: HashMap<String, ScalarExpression<*>> = HashMap()
    override val constraints: HashMap<String, Constraint> = HashMap()
}

public abstract class MigrateBaseTable<E: Any>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
): BaseTable<E>(tableName, alias, catalog, schema, entityClass), MigrateTableMixin {
    override val self: BaseTable<*>
        get() = this
    override val columnNotNull: HashSet<String> = HashSet()
    override val columnAutoIncrement: HashSet<String> = HashSet()
    override val columnSize: HashMap<String, Int> = HashMap()
    override val columnDefault: HashMap<String, ScalarExpression<*>> = HashMap()
    override val constraints: HashMap<String, Constraint> = HashMap()
}

public open class MigrateMapTable(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null
) : BaseTable<ColumnMap>(tableName, alias, catalog, schema), MigrateTableMixin {
    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): ColumnMap {
        return ColumnMap(columns.associate { it to row[it] })
    }

    override val self: BaseTable<*>
        get() = this
    override val columnNotNull: HashSet<String> = HashSet()
    override val columnAutoIncrement: HashSet<String> = HashSet()
    override val columnSize: HashMap<String, Int> = HashMap()
    override val columnDefault: HashMap<String, ScalarExpression<*>> = HashMap()
    override val constraints: HashMap<String, Constraint> = HashMap()
}

public data class ColumnMap(val data: Map<Column<*>, Any?>) {
    @Suppress("UNCHECKED_CAST")
    public operator fun <T: Any> get(key: Column<T>): T? = data[key] as? T
}