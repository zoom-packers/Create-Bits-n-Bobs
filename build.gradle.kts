import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.imageio.ImageIO

// NeoForge 26.3 build for Zoompack 5, against ZP5's NeoForge port of Create Fly (see PORTING.md).
plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
}

group = property("maven_group") as String
version = "${property("mod_version")}+neoforge-${property("minecraft_version")}-${property("port_build")}"

base {
    archivesName.set(property("archives_base_name") as String)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

val createJar = files(property("create_jar") as String)
val strutsJar = files(property("struts_jar") as String)

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
        // Fabric entrypoints and metadata, replaced by com/kipti/bnb/neoforge/** (see PORTING.md)
        java.exclude("com/kipti/bnb/fabric/**")
        resources.exclude("fabric.mod.json")
    }
}

neoForge {
    version = property("neo_version") as String

    // Create's access transformer and interface injections, so code that uses them compiles
    accessTransformers.from(file("libs/create-accesstransformer.cfg"))
    interfaceInjectionData.from(file("libs/create-interfaces.json"))

    mods {
        create("bits_n_bobs") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
            programArgument("--nogui")
        }
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", "bits_n_bobs")
        }
        configureEach {
            logLevel = org.slf4j.event.Level.INFO
            if (project.hasProperty("mixinDebug")) {
                systemProperty("mixin.debug.verbose", "true")
                systemProperty("mixin.debug.export", "true")
            }
        }
    }
}

dependencies {
    compileOnly(createJar)
    compileOnly(strutsJar)
    // Create and Strut Your Stuff are loaded as mods in the dev runs
    runtimeOnly(createJar)
    runtimeOnly(strutsJar)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000", "-Xmaxwarns", "1000"))
}

tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val generatedConnectedTextureResources = layout.buildDirectory.dir("generated/connected-texture-resources")

val omniTileIndexes = listOf(
    1, 2, 3, 8, 9, 10, 11, 12, 13, 16, 17, 18, 19, 20, 21, 24, 25, 26, 27, 28, 29, 30,
    32, 33, 34, 35, 36, 37, 38, 40, 41, 42, 43, 44, 45, 46, 48, 49, 50, 51, 52, 53, 54, 56, 57, 58,
)
val rectangleTileIndexes = (0..11).toList() + (13..15).toList()
val rectangleWithoutSingleTileIndexes = listOf(1, 2, 3, 5, 6, 7, 9, 10, 11)
val rectangleSingleTileIndex = 12
val verticalTileIndexes = listOf(1, 2, 3)

class ConnectedTextureLayout(val gridSize: Int, val tileIndexes: List<Int>, val singleTileIndex: Int? = null)

val connectedTextureSheets = mapOf(
    "assets/bits_n_bobs/textures/block/industrial_grating_connected.png" to ConnectedTextureLayout(8, omniTileIndexes),
    "assets/bits_n_bobs/textures/block/industrial_grating_cutout_connected.png" to ConnectedTextureLayout(8, omniTileIndexes),
    "assets/bits_n_bobs/textures/block/industrial_grating_pipe_cutout_connected.png" to ConnectedTextureLayout(8, omniTileIndexes),
    "assets/bits_n_bobs/textures/block/weathered_girder_pole_side_connected.png" to ConnectedTextureLayout(2, verticalTileIndexes),
    "assets/bits_n_bobs/textures/block/industrial_iron_encased_cogwheel_side_connected.png" to ConnectedTextureLayout(2, verticalTileIndexes),
    "assets/bits_n_bobs/textures/block/weathered_iron_encased_cogwheel_side_connected.png" to ConnectedTextureLayout(2, verticalTileIndexes),
    "assets/bits_n_bobs/textures/block/chiseled_planks_connected.png" to ConnectedTextureLayout(8, omniTileIndexes),
    "assets/bits_n_bobs/textures/block/dyed_fluid_tank/fluid_tank_connected_*.png" to
        ConnectedTextureLayout(4, rectangleTileIndexes, rectangleSingleTileIndex),
    "assets/bits_n_bobs/textures/block/dyed_fluid_tank/fluid_tank_top_connected_*.png" to
        ConnectedTextureLayout(4, rectangleWithoutSingleTileIndexes, rectangleSingleTileIndex),
    "assets/bits_n_bobs/textures/block/dyed_fluid_tank/fluid_tank_inner_connected_*.png" to
        ConnectedTextureLayout(4, rectangleWithoutSingleTileIndexes, rectangleSingleTileIndex),
)

val generateConnectedTextureSprites = tasks.register("generateConnectedTextureSprites") {
    val resourceRoot = file("src/main/resources")
    inputs.files(fileTree(resourceRoot) { include(connectedTextureSheets.keys) })
        .withPropertyName("connectedTextureSheets")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("connectedTextureLayout", "create-fly-26.2-v3")
    outputs.dir(generatedConnectedTextureResources)
    doLast {
        val outputRoot = generatedConnectedTextureResources.get().asFile
        delete(outputRoot)
        var sheetCount = 0
        var spriteCount = 0
        connectedTextureSheets.forEach { (pattern, layout) ->
            val gridSize = layout.gridSize
            fileTree(resourceRoot) { include(pattern) }.files.sortedBy { it.invariantSeparatorsPath }.forEach { sheetFile ->
                val sheet = ImageIO.read(sheetFile) ?: throw GradleException("Could not decode $sheetFile")
                if (sheet.width != sheet.height || sheet.width % gridSize != 0) {
                    throw GradleException("$sheetFile must be a $gridSize x $gridSize grid of square tiles, but is ${sheet.width} x ${sheet.height}")
                }
                val tileSize = sheet.width / gridSize
                val relativeSheet = resourceRoot.toPath().relativize(sheetFile.toPath()).toString()
                val spriteDirectory = outputRoot.resolve(relativeSheet.removeSuffix(".png"))
                spriteDirectory.mkdirs()
                fun writeTile(sourceTileIndex: Int, target: File) {
                    val tile = sheet.getSubimage(sourceTileIndex % gridSize * tileSize, sourceTileIndex / gridSize * tileSize, tileSize, tileSize)
                    if (!ImageIO.write(tile, "png", target)) {
                        throw GradleException("No PNG writer is available for $target")
                    }
                    spriteCount++
                }
                layout.tileIndexes.forEachIndexed { index, sourceTileIndex ->
                    writeTile(sourceTileIndex, spriteDirectory.resolve("${index + 1}.png"))
                }
                layout.singleTileIndex?.let { sourceTileIndex ->
                    writeTile(sourceTileIndex, spriteDirectory.resolveSibling(sheetFile.name.replace("_connected", "")))
                }
                sheetCount++
            }
        }
        logger.lifecycle("Generated $spriteCount connected-texture sprites from $sheetCount sheets")
    }
}

// Expands ${...} placeholders in src/main/templates (neoforge.mods.toml) from gradle.properties
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version_range" to project.property("minecraft_version_range") as String,
        "neo_version_range" to project.property("neo_version_range") as String,
        "create_version_range" to project.property("create_version_range") as String,
        "struts_version_range" to project.property("struts_version_range") as String,
    )
    inputs.properties(props)
    expand(props)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.processResources {
    dependsOn(generateConnectedTextureSprites)
    from(generatedConnectedTextureResources)
    exclude(connectedTextureSheets.keys)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    from("LICENSE")
    from("NOTICE")
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE")
    from("NOTICE")
}

val ownCodePrefix = "com/kipti/bnb/"

val allowedResourcePrefixes = listOf(
    "assets/bits_n_bobs/",
    "data/bits_n_bobs/",
    "META-INF/",
    "LICENSE",
    "NOTICE",
    "bits_n_bobs.mixins.json",
    "bits_n_bobs.client.mixins.json",
    "data/c/tags/",
    "data/create/tags/",
    "data/create/advancement/",
    "data/create/recipe/",
    "data/minecraft/tags/",
    "data/railways/tags/",
    "assets/create/blockstates/",
    "resourcepacks/cogwheel_items/pack.mcmeta",
    "resourcepacks/cogwheel_items/assets/create/items/",
    "assets/create_connected/models/",
    "assets/create_hypertube/models/",
)

afterEvaluate {
    val checkForeignNamespaces = tasks.register("checkForeignNamespaces") {
        val jarNames = listOf("jar", "sourcesJar").filter { tasks.names.contains(it) }
        require(jarNames.isNotEmpty()) { "checkForeignNamespaces found no jar task to inspect" }
        val jarTasks = jarNames.map { tasks.named<Jar>(it).get() }
        dependsOn(jarTasks)
        val archives: List<Provider<RegularFile>> = jarTasks.map { it.archiveFile }
        val codePrefix = ownCodePrefix
        val prefixes = allowedResourcePrefixes
        doLast {
            val bad = mutableListOf<String>()
            var checked = 0
            for (provider in archives) {
                val jar: File = provider.get().asFile
                if (!jar.exists()) continue
                checked++
                val zip = ZipFile(jar)
                try {
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry: ZipEntry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name: String = entry.name
                        if (name.endsWith(".class") || name.endsWith(".java")) {
                            if (!name.startsWith(codePrefix)) bad.add(jar.name + "!" + name)
                        } else if (prefixes.none { p -> name.startsWith(p) }) {
                            bad.add(jar.name + "!" + name)
                        }
                    }
                } finally {
                    zip.close()
                }
            }
            if (checked == 0) {
                throw GradleException("checkForeignNamespaces inspected no archives")
            }
            if (bad.isNotEmpty()) {
                throw GradleException("Foreign namespace entries in published artifacts:\n" + bad.joinToString("\n"))
            }
        }
    }
    tasks.named("check") { dependsOn(checkForeignNamespaces) }
}

tasks.register("printCompileClasspath") {
    val cp = sourceSets.main.get().compileClasspath
    val out = layout.projectDirectory.file(".classpath.txt")
    doLast {
        out.asFile.writeText(cp.files.joinToString("\n") { it.absolutePath } + "\n")
    }
}
