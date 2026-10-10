plugins {
    alias(libs.plugins.loom)
}

version = libs.versions.mod.version.get()
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

loom {
    mixin {
        defaultRefmapName.set("chuklclient-refmap.json")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)
    mappings("net.fabricmc:yarn:${libs.versions.yarn.mappings.get()}:v2")
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}
