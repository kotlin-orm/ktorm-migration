
plugins {
    id("ktorm.module-conventions")
}

repositories {
    mavenLocal()
}

dependencies {
    api("org.ktorm:ktorm-core:3.5.0-SNAPSHOT")
    testImplementation("com.h2database:h2:1.4.197")
}

