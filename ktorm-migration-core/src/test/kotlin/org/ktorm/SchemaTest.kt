package org.ktorm

import org.junit.Before
import org.junit.Test
import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.ktorm.dsl.insert
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.expression.*
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.migration.*
import org.ktorm.schema.*
import org.ktorm.testmigrations.LatestMigration
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * Created by vince on Dec 07, 2018.
 */
open class SchemaTest {
    lateinit var database: Database

    @Before
    open fun init() {
        println("initialize database")
        database = Database.connect(
            dialect = object : SqlDialect {
                override fun createSqlFormatter(
                    database: Database,
                    beautifySql: Boolean,
                    indentSize: Int
                ): SqlFormatter {
                    return TestFormatter(database, beautifySql, indentSize)
                }
            },
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )
    }

    @Test
    fun create() {
        database.executeUpdate(Departments.createTable())
        database.executeUpdate(Employees.createTable())
        database.insert(Departments) {
            set(it.id, 0)
            set(it.name, "Test Department")
            set(it.location, "Some Location")
            set(it.mixedCase, "WhEeEeEe")
        }
        database.insert(Employees) {
            set(it.id, 0)
            set(it.name, "Bob Boberson")
            set(it.departmentId, 0)
        }
        database.executeUpdate(DropTableExpression(Employees.asReferenceExpression()))
        database.executeUpdate(DropTableExpression(Departments.asReferenceExpression()))
    }

    @Test
    fun reverse() {
        val tables = BuildingTables()
        tables._tables[Departments.asReferenceExpression()] = Departments.asMigrationTable()
        tables.apply(Employees.createTable())
        val recreated = tables._tables[Employees.asReferenceExpression()]!!
        println("Original  : ${Employees.structuralInfo}")
        println("Reproduced: ${recreated.structuralInfo}")
        assertEquals(recreated.structuralInfo.toString(), Employees.structuralInfo.toString())
    }

    @Test
    fun noUpgradeNeeded() {
        val tables = BuildingTables()
        tables._tables[Departments.asReferenceExpression()] = Departments.asMigrationTable()
        tables.apply(Employees.createTable())
        val recreated = tables._tables[Employees.asReferenceExpression()]!!
        val ops = recreated.upgradeTo(Employees)
        assertEquals(listOf(), ops)
    }

    @Test
    fun forwards() {
        val currentState = CreateTableExpression(
            name = TableReferenceExpression(
                name = "t_employee",
                catalog = null,
                schema = null
            ),
            columns = listOf(
                ColumnDeclarationExpression(
                    name = "id",
                    sqlType = IntSqlType,
                    size = null,
                    notNull = false,
                    default = null,
                    autoIncrement = false
                ), ColumnDeclarationExpression(
                    name = "name",
                    sqlType = VarcharSqlType,
                    size = null,
                    notNull = false,
                    default = null,
                    autoIncrement = false
                ), ColumnDeclarationExpression(
                    name = "job",
                    sqlType = VarcharSqlType,
                    size = null,
                    notNull = false,
                    default = ArgumentExpression(
                        value = "Minion",
                        sqlType = VarcharSqlType
                    ),
                    autoIncrement = false
                ), ColumnDeclarationExpression(
                    name = "manager_id",
                    sqlType = IntSqlType,
                    size = null,
                    notNull = false,
                    default = null,
                    autoIncrement = false
                ), ColumnDeclarationExpression(
                    name = "hire_date",
                    sqlType = LocalDateSqlType,
                    size = null,
                    notNull = false,
                    default = null,
                    autoIncrement = false
                ), ColumnDeclarationExpression(
                    name = "salary",
                    sqlType = LongSqlType,
                    size = null,
                    notNull = false,
                    default = null,
                    autoIncrement = false
                ), ColumnDeclarationExpression(
                    name = "department_id",
                    sqlType = IntSqlType,
                    size = null,
                    notNull = false,
                    default = null,
                    autoIncrement = false
                )
            ),
            constraints = mapOf(
                "null_null_t_employee_unique_name" to UniqueTableConstraintExpression(
                    across = listOf(
                        ColumnReferenceExpression(
                            name = "name"
                        )
                    )
                ),
                "null_null_t_employee_pk" to PrimaryKeyTableConstraintExpression(
                    across = listOf(
                        ColumnReferenceExpression(
                            name = "id"
                        )
                    )
                ),
                "null_null_t_employee_fk_department_id" to
                        ForeignKeyTableConstraintExpression(
                            otherTable = TableReferenceExpression(
                                name = "t_department",
                                catalog = null,
                                schema = null
                            ), correspondence = mapOf(
                                ColumnReferenceExpression(
                                    name = "department_id"
                                ) to ColumnReferenceExpression(
                                    name = "id"
                                )
                            )
                        )
            )
        )
        val tables = BuildingTables()
        tables._tables[Departments.asReferenceExpression()] = Departments.asMigrationTable()
        tables.apply(currentState)
        val recreated = tables._tables[Employees.asReferenceExpression()]!!
        val updates = recreated.upgradeTo(Employees)
        val out = StringBuilder()
        updates.generateMigrationSource("TestMigration", number = 1, dependsOn = listOf("PreviousMigration"), out = out)
        println(out)
    }

    @Test
    fun fullMigration(){
        val updates = listOf<MigrateTableMixin>().upgradeTo(listOf(Departments, Employees, Customers))
        val out = StringBuilder()
        updates.generateMigrationSource("TestMigration", number = 1, dependsOn = listOf("PreviousMigration"), out = out)
        println(out)

        val migration = object: Migration {
            override val number: Int
                get() = 1
            override val actions: List<MigrationAction>
                get() = updates.map { MigrationAction.ReversibleSql(it) }
            override val dependsOn: List<Migration>
                get() = listOf()
        }
        migration.migrate(database)
        database.insert(Departments) {
            set(it.id, 0)
            set(it.name, "Test Department")
            set(it.location, "Some Location")
            set(it.mixedCase, "WhEeEeEe")
        }
        database.insert(Employees) {
            set(it.id, 0)
            set(it.name, "Bob Boberson")
            set(it.departmentId, 0)
        }
        migration.undo(database)
    }

    @Test
    fun makeMigrationsTest() {
        makeMigrations(
            folder = File("src/test/kotlin/org/ktorm/testmigrations"),
            packageName = "org.ktorm.testmigrations",
            latestMigration = LatestMigration,
            tables = arrayOf(
                Departments,
                Employees,
                Customers
            )
        )
    }


    class TestFormatter(
        database: Database,
        beautifySql: Boolean,
        indentSize: Int
    ) : SqlSchemaFormatter(database, beautifySql, indentSize) {
        override fun writePagination(expr: QueryExpression) = TODO()
    }

    interface Department : Entity<Department> {
        companion object : Entity.Factory<Department>()

        val id: Int
        var name: String
        var location: String?
        var mixedCase: String?
    }

    interface Employee : Entity<Employee> {
        companion object : Entity.Factory<Employee>()

        var id: Int
        var name: String
        var job: String
        var manager: Employee?
        var hireDate: LocalDate
        var salary: Long
        var department: Department

        val upperName get() = name.toUpperCase()
        fun upperName() = name.toUpperCase()
    }

    interface Customer : Entity<Customer> {
        companion object : Entity.Factory<Customer>()

        var id: Int
        var name: String
        var email: String
        var phoneNumber: String
        var address: String
    }

    open class Departments(alias: String?) : MigrateTable<Department>("t_department", alias) {
        companion object : Departments(null)

        override fun aliased(alias: String) = Departments(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").size(128).bindTo { it.name }
        val location =
            varchar("location").default("Unimportant").size(128).bindTo { it.location }
        val mixedCase = varchar("mixedCase").size(128).bindTo { it.mixedCase }
    }

    open class Employees(alias: String?) : MigrateTable<Employee>("t_employee", alias) {
        companion object : Employees(null)

        override fun aliased(alias: String) = Employees(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").size(128).unique().bindTo { it.name }
        val job = varchar("job").size(128).default("Minion").bindTo { it.job }
        val managerId = int("manager_id").bindTo { it.manager?.id }
        val hireDate = date("hire_date").bindTo { it.hireDate }
        val salary = long("salary").bindTo { it.salary }
        val departmentId = int("department_id").foreignKey(Departments).references(Departments) { it.department }
        val department = departmentId.referenceTable as Departments
    }

    open class Customers(alias: String?) : MigrateTable<Customer>("t_customer", alias, schema = "company") {
        companion object : Customers(null)

        override fun aliased(alias: String) = Customers(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").size(128).bindTo { it.name }
        val email = varchar("email").size(128).bindTo { it.email }
        val phoneNumber = varchar("phone_number").size(128).bindTo { it.phoneNumber }
        val address = varchar("address").size(512).bindTo { it.address }
    }

    val Database.departments get() = this.sequenceOf(Departments)

    val Database.employees get() = this.sequenceOf(Employees)

    val Database.customers get() = this.sequenceOf(Customers)
}
