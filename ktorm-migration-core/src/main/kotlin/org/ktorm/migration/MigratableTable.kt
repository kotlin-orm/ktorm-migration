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

import org.ktorm.entity.Entity
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table
import kotlin.reflect.KClass

/**
 * A [Table] that supports migrations.
 */
public abstract class MigratableTable<E: Entity<E>>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
): Table<E>(tableName, alias, catalog, schema, entityClass), MigratableTableMixin {
    override val self: BaseTable<*>
        get() = this
    override val columnNotNull: HashSet<String> = HashSet()
    override val columnAutoIncrement: HashSet<String> = HashSet()
    override val columnSize: HashMap<String, Int> = HashMap()
    override val columnDefault: HashMap<String, ScalarExpression<*>> = HashMap()
    override val constraints: HashMap<String, Constraint> = HashMap()
}

/**
 * A [BaseTable] that supports migrations.
 */
public abstract class MigratableBaseTable<E: Any>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
): BaseTable<E>(tableName, alias, catalog, schema, entityClass), MigratableTableMixin {
    override val self: BaseTable<*>
        get() = this
    override val columnNotNull: HashSet<String> = HashSet()
    override val columnAutoIncrement: HashSet<String> = HashSet()
    override val columnSize: HashMap<String, Int> = HashMap()
    override val columnDefault: HashMap<String, ScalarExpression<*>> = HashMap()
    override val constraints: HashMap<String, Constraint> = HashMap()
}
