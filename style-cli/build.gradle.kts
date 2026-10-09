plugins {
    id("stratastax.kotlin")
    id("stratastax.repositories")
    id("stratastax.coverage")
    application
}

dependencies {
    implementation(project(":style-rules"))
    runtimeOnly(libs.slf4j.nop)

    testImplementation(libs.bundles.test)
    testImplementation(kotlin("test"))
}

application {
    applicationName = "stratafmt"
    mainClass.set("dev.buijs.stratastax.style.cli.MainKt")
}

tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "executable jar with all dependencies"
    archiveFileName.set("stratafmt.jar")
    manifest { attributes["Main-Class"] = application.mainClass.get() }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Signatures of the merged jars would no longer match.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    from(sourceSets.main.get().output)
    from({ configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } })
}
