import java.net.URI

plugins {
    id("java")
    id("maven-publish")
}

group = "com.github.keyboardcat1"
version = "2.2.5"

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jogamp.org/deployment/maven/")
    }
}

val extraLibs by configurations.creating

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")

    // Use the version that's already working (2.3.2)
    implementation("org.jogamp.jogl:jogl-all-main:2.3.2")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.3.2")

    // Other dependencies
    extraLibs("org.kynosarges:tektosyne:6.2.0")
    extraLibs("org.ejml:ejml-all:0.43")
}

// Move this outside dependencies block
configurations {
    implementation {
        extendsFrom(extraLibs)
    }
}

tasks.test {
    useJUnitPlatform()
}

// Add JVM args for any JavaExec tasks
tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED"
    )
}

// Create a custom run task
tasks.register<JavaExec>("runErosionGUI") {
    group = "application"
    description = "Run the Erosion GUI"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ErosionGUI")
    jvmArgs = listOf(
        "--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED"
    )
}

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/keyboardcat1/erosio")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.jar {
    from(extraLibs.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}