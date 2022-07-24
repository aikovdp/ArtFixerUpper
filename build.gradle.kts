plugins {
    id("java")
    id("application")
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
    implementation("net.kyori:adventure-nbt:4.11.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.11.0")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}