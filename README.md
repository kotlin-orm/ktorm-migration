# ktorm-migration
Ktorm migration support

## Status

Currently broken


Mostly complete, but needs api stabilization, testing and documentation.

## Usage

In your app, use `MigrateTable`, `MigrateBaseTable`, `MigrateMapTable`, or `MigrateTableMixin` instead of your usual tables.

You now have access to additional extension functions on your types, including

```kotlin
column.size(128)  // For size of varchar, etc
column.default("Default Value")
column.default(otherColumn)  // Can be an expression instead
column.unique()  // Adds a unique constraint
column.foreignKey(OtherTable)  // Adds a foreign key constraint
column.notNull()
```

### Create Migrations

When you first start, create the migrations folder using the function `initializeMigrations`.  This will generate a `LatestMigration.kt` you can use later.

```kotlin
initializeMigrations(
    folder = File("src/test/kotlin/org/ktorm/testmigrations"),
    packageName = "org.ktorm.testmigrations"
)
```

Then we can start using `makeMigrations` to automatically create new migrations for us:

```kotlin
makeMigrations(
    folder = File("src/test/kotlin/org/ktorm/testmigrations"),
    packageName = "org.ktorm.testmigrations",
    latestMigration = LatestMigration,
    tables = arrayOf(
        // All of your tables go here
        Departments,
        Employees,
        Customers
    )
)
```

This will populate the folder with your migrations.

### Run Migrations

```kotlin
LatestMigration.migrate(myDatabase)
```

## Known issues

- We need a lot of documentation and testing
- Transformed fields are unsupported in automatic migration generation, due to being not being reproducible in source.
- No specific database `Dialect` implements `SqlSchemaFormatter` yet
  - Specific implementations are gonna be annoying.  This repo will need a module for every supported one.
