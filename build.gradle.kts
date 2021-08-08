
group = "org.ktorm.migration"
version = "0.0.1-SNAPSHOT"

task("printClasspath") {
    doLast {
        val jars = subprojects
            .map { it.configurations["compileClasspath"] }
            .flatMap { it.files }
            .filterNotTo(HashSet()) { it.name.contains("ktorm") }
            .onEach { println(it.name) }

        val file = file("build/ktorm.classpath")
        file.parentFile.mkdirs()
        file.writeText(jars.joinToString(File.pathSeparator) { it.absolutePath })
        println("Classpath written to build/ktorm.classpath")
    }
}
