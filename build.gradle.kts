plugins {
    id("application")
    id("fabric-loom") version "0.12-SNAPSHOT"
}

application {
    mainClass.set("me.aikovdp.artfixerupper.ArtFixerUpper")
}

group = "me.aikovdp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.16.5")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.14.8")
    implementation("net.kyori:adventure-nbt:4.11.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.11.0")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}