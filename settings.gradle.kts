
include("ktorm-migration-core")

rootProject.name = "ktorm-migration"
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}
