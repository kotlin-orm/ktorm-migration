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

import java.math.BigDecimal
import java.time.*
import java.sql.*
import java.sql.Date
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

internal class SourceRegenerator {
    val imports = HashSet<String>()
    val builder = StringBuilder()
    fun visit(value: Any?){
        when(value){
            null -> builder.append("null")
            is Boolean,
            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double,
            is UByte,
            is UShort,
            is UInt,
            is ULong -> builder.append(value)
            is BigDecimal -> builder.append("BigDecimal($value)")
            is ByteArray -> if(value.size > 32)
                throw IllegalArgumentException("I'm not gonna reproduce that monster.")
                else {
                builder.append("byteArrayOf(")
                for(byte in value){
                    builder.append(byte)
                }
                builder.append(')')
            }
            is String -> {
                builder.append('"')
                builder.append(value
                    .replace("\t", "\\t")
                    .replace("\b", "\\b")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\"", "\\")
                    .replace("\\", "\\\\")
                    .replace("\$", "\\$")
                )
                builder.append('"')
            }
            is List<*> -> {
                builder.append("listOf(")
                var first = true
                for(child in value) {
                    if(first) first = false
                    else builder.append(", ")
                    visit(child)
                }
                builder.append(")")
            }
            is Map<*, *> -> {
                builder.append("mapOf(")
                var first = true
                for(entry in value) {
                    if(first) first = false
                    else builder.append(", ")
                    visit(entry.key)
                    builder.append(" to ")
                    visit(entry.value)
                }
                builder.append(")")
            }
            is Timestamp -> {
                imports.add("java.sql.Timestamp")
                builder.append("Timestamp(")
                builder.append(value.time)
                builder.append(")")
            }
            is Date -> {
                imports.add("java.sql.Date")
                builder.append("Date(")
                builder.append(value.time)
                builder.append(")")
            }
            is Time -> {
                imports.add("java.sql.Time")
                builder.append("Time(")
                builder.append(value.time)
                builder.append(")")
            }
            is Instant -> {
                imports.add("java.time.Instant")
                builder.append("Instant.ofEpochMilli(")
                builder.append(value.toEpochMilli())
                builder.append(")")
            }
            is LocalDateTime -> {
                imports.add("java.time.LocalDateTime")
                builder.append("LocalDateTime.of(")
                builder.append(value.year)
                builder.append(", ")
                builder.append(value.monthValue)
                builder.append(", ")
                builder.append(value.dayOfMonth)
                builder.append(", ")
                builder.append(value.hour)
                builder.append(", ")
                builder.append(value.minute)
                builder.append(", ")
                builder.append(value.second)
                builder.append(", ")
                builder.append(value.nano)
                builder.append(")")
            }
            is LocalDate -> {
                imports.add("java.time.LocalDate")
                builder.append("LocalDate.of(")
                builder.append(value.year)
                builder.append(", ")
                builder.append(value.monthValue)
                builder.append(", ")
                builder.append(value.dayOfMonth)
                builder.append(")")
            }
            is LocalTime -> {
                imports.add("java.time.LocalTime")
                builder.append("LocalTime.of(")
                builder.append(value.hour)
                builder.append(", ")
                builder.append(value.minute)
                builder.append(", ")
                builder.append(value.second)
                builder.append(", ")
                builder.append(value.nano)
                builder.append(")")
            }
            is MonthDay -> {
                imports.add("java.time.MonthDay")
                builder.append("MonthDay.of(")
                builder.append(value.monthValue)
                builder.append(", ")
                builder.append(value.dayOfMonth)
                builder.append(")")
            }
            is YearMonth -> {
                imports.add("java.time.YearMonth")
                builder.append("YearMonth.of(")
                builder.append(value.year)
                builder.append(", ")
                builder.append(value.monthValue)
                builder.append(")")
            }
            is Year -> {
                imports.add("java.time.Year")
                builder.append("Year.of(")
                builder.append(value.value)
                builder.append(")")
            }
            is UUID -> {
                imports.add("java.util.UUID")
                builder.append("UUID.fromString(\"")
                builder.append(value)
                builder.append("\")")
            }
            else -> {
                val type = value::class
                when {
                    type.objectInstance != null -> {
                        imports.add(type.qualifiedName!!)
                        builder.append(type.simpleName)
                    }
                    type.isData -> {
                        imports.add(type.qualifiedName!!)
                        builder.append(type.simpleName)
                        builder.append('(')
                        var first = true
                        for(prop in type.memberProperties.sortedBy {
                            type.primaryConstructor!!.parameters.indexOfFirst { p -> it.name == p.name }
                        }) {
                            if(first) first = false
                            else builder.append(", ")
                            builder.append(prop.name)
                            builder.append(" = ")
                            @Suppress("UNCHECKED_CAST")
                            visit((prop as KProperty1<Any, Any?>).get(value))
                        }
                        builder.append(')')
                    }
                    else -> builder.append("ERROR")
                }
            }
        }
    }
}