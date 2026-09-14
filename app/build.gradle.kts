import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import ir.taqvim.buildlogic.TaqvimVersion
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.taqvim.android.application)
    alias(libs.plugins.taqvim.android.compose)
    // Serializable navigation destinations, saved with the back stack (ADR-0015).
    id("org.jetbrains.kotlin.plugin.serialization")
}

/** Plan §9: release APK at most 8 MB (T-1800). */
val apkBudgetBytes: Long = 8L * 1024 * 1024

/** The release tag passed by CI (`-Ptaqvim.version`), otherwise the checked-in `version.properties` (T-1900). */
val taqvimVersion: TaqvimVersion =
    TaqvimVersion.resolve(
        property = providers.gradleProperty(TaqvimVersion.PROPERTY).orNull,
        propertiesFile =
            providers
                .fileContents(rootProject.layout.projectDirectory.file("version.properties"))
                .asText
                .orNull,
    )

android {
    namespace = "ir.taqvim.app"
    defaultConfig {
        applicationId = "ir.taqvim.app"
        versionCode = taqvimVersion.code
        versionName = taqvimVersion.name
    }
    buildFeatures {
        // Version and build type shown on the About screen and in problem reports (T-1504).
        buildConfig = true
    }
    signingConfigs {
        // Release signing comes from CI secrets (release.yml). Without them release builds stay unsigned.
        val keystore = providers.environmentVariable("TAQVIM_KEYSTORE_FILE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("TAQVIM_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("TAQVIM_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("TAQVIM_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        getByName("debug") {
            // en-XA (accented, expanded) and ar-XB (mirrored RTL) pseudo-locales catch untranslated or clipped text.
            isPseudoLocalesEnabled = true
        }
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(projects.core.praytimes)
    implementation(projects.core.astronomy)
    implementation(projects.core.i18n)
    implementation(projects.core.nlp)
    implementation(projects.core.ics)
    implementation(projects.core.workdays)
    implementation(projects.core.ui)

    implementation(projects.data.preferences)
    implementation(projects.data.database)
    implementation(projects.data.events)
    implementation(projects.data.deviceCalendar)
    implementation(projects.data.location)
    implementation(projects.data.scheduler)

    implementation(projects.feature.calendar)
    implementation(projects.feature.timeline)
    implementation(projects.feature.month)
    implementation(projects.feature.year)
    implementation(projects.feature.agenda)
    implementation(projects.feature.events)
    implementation(projects.feature.times)
    implementation(projects.feature.astronomy)
    implementation(projects.feature.map)
    implementation(projects.feature.compass)
    implementation(projects.feature.tools)
    implementation(projects.feature.settings)
    implementation(projects.feature.about)
    implementation(projects.feature.widgets)
    implementation(projects.feature.notification)
    implementation(projects.feature.wallpaper)
    implementation(projects.feature.search)
    implementation(projects.feature.backup)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.browser)
    // LevelCalibration (T-1303) exposes an ImmutableMap in the compass port; stored by the T-1500 adapters.
    implementation(libs.kotlinx.collections.immutable)

    // Plan T-1803: leak detection in debug builds only (ADR-0021); never in release.
    debugImplementation(libs.leakcanary.android)

    testImplementation(libs.koin.test)
    // Screenshot environments of the navigation frame (ADR-0015).
    testImplementation(projects.core.uiTesting)
    testImplementation(libs.kotlinx.coroutines.test)
}

/**
 * T-1800 (ADR-0018): the shrunk release keeps what reflection and persisted names need. Each line of
 * `shrinking-requirements.txt` is `kept <class>` (present, may be renamed), `named <class>` (present with its name) or
 * `fields <class>` (present with every field name unchanged), checked against the R8 mapping.
 */
abstract class ReleaseShrinkingCheck : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mapping: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val requirements: RegularFileProperty

    @TaskAction
    fun verify() {
        val classes = mutableMapOf<String, String>()
        val fields = mutableMapOf<String, MutableList<Pair<String, String>>>()
        var current: String? = null
        mapping.get().asFile.forEachLine { line ->
            val header = Regex("""^(\S+) -> (\S+):$""").matchEntire(line)
            val member = Regex("""^\s+\S+ (\S+) -> (\S+)$""").matchEntire(line)
            when {
                header != null -> {
                    current = header.groupValues[1].also { classes[it] = header.groupValues[2] }
                }

                member != null && '(' !in line -> {
                    val owner = current ?: return@forEachLine
                    fields.getOrPut(owner) { mutableListOf() } += member.groupValues[1] to member.groupValues[2]
                }
            }
        }
        val problems =
            requirements
                .get()
                .asFile
                .readLines()
                .map(String::trim)
                .filterNot { it.isEmpty() || it.startsWith("#") }
                .mapNotNull { line ->
                    val (kind, name) = line.split(' ', limit = 2)
                    when (kind) {
                        "kept" -> {
                            "$name was removed".takeIf { name !in classes }
                        }

                        "named" -> {
                            "$name was renamed or removed".takeIf { classes[name] != name }
                        }

                        "fields" -> {
                            val kept = fields[name].orEmpty()
                            "$name lost field names".takeIf { kept.isEmpty() || kept.any { it.first != it.second } }
                        }

                        else -> {
                            "unknown requirement: $line"
                        }
                    }
                }
        check(problems.isEmpty()) { "Release shrinking requirements not met (T-1800):\n" + problems.joinToString("\n") }
    }
}

/** T-1800 plan §9 budget: every APK of the release variant is at most [budgetBytes] bytes. */
abstract class ApkSizeCheck : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val loader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val budgetBytes: Property<Long>

    @TaskAction
    fun measure() {
        val artifacts = checkNotNull(loader.get().load(apkDirectory.get())) { "No APKs in ${apkDirectory.get()}" }
        artifacts.elements.forEach { element ->
            val apk = File(element.outputFile)
            val budget = budgetBytes.get()
            logger.lifecycle("APK ${apk.name}: ${apk.length()} bytes (budget $budget)")
            check(apk.length() <= budget) { "${apk.name} is ${apk.length()} bytes, over the $budget byte budget" }
        }
    }
}

/**
 * T-1900 (docs/RELEASE.md): every output of the variant carries the resolved Taqvim version. Also prints it, so
 * `./gradlew :app:verifyReleaseVersion -Ptaqvim.version=<tag>` shows the name and code a tag produces.
 */
abstract class VersionCheck : DefaultTask() {
    @get:Input
    abstract val expectedName: Property<String>

    @get:Input
    abstract val expectedCode: Property<Int>

    @get:Input
    abstract val outputNames: ListProperty<String>

    @get:Input
    abstract val outputCodes: ListProperty<Int>

    @TaskAction
    fun verify() {
        logger.lifecycle("Taqvim version ${expectedName.get()} (versionCode ${expectedCode.get()})")
        check(outputNames.get().isNotEmpty() && outputNames.get().all { it == expectedName.get() }) {
            "versionName ${outputNames.get()} differs from ${expectedName.get()}"
        }
        check(outputCodes.get().all { it == expectedCode.get() }) {
            "versionCode ${outputCodes.get()} differs from ${expectedCode.get()}"
        }
    }
}

/**
 * T-1804 manifest audit (ADR-0017): the release manifest exports exactly the components listed for `all` builds in
 * `src/test/resources/security/exported-components.txt`. The debug manifest is checked by ExportedComponentsTest.
 */
abstract class ExportedComponentsAudit : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val allowlist: RegularFileProperty

    @TaskAction
    fun audit() {
        val android = "http://schemas.android.com/apk/res/android"
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(manifest.get().asFile)
        val exported =
            listOf("activity", "activity-alias", "service", "receiver", "provider")
                .flatMap { tag ->
                    val nodes = document.getElementsByTagName(tag)
                    (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }
                }.filter { it.getAttributeNS(android, "exported") == "true" }
                .map { component ->
                    val permission = component.getAttributeNS(android, "permission").ifEmpty { "-" }
                    "${component.getAttributeNS(android, "name")} | $permission"
                }.toSet()
        val allowed =
            allowlist
                .get()
                .asFile
                .readLines()
                .map(String::trim)
                .filterNot { it.isEmpty() || it.startsWith("#") }
                .map { line -> line.split('|').map(String::trim) }
                .filter { it.getOrNull(2) == "all" }
                .map { "${it[0]} | ${it[1]}" }
                .toSet()
        check(exported == allowed) {
            "Exported components differ from the allowlist (T-1804).\n" +
                "Not allowed: ${exported - allowed}\nMissing: ${allowed - exported}"
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val taskName = "audit${variant.name.replaceFirstChar(Char::uppercase)}ExportedComponents"
        val audit =
            tasks.register<ExportedComponentsAudit>(taskName) {
                group = "verification"
                description = "Checks the merged ${variant.name} manifest against the exported-components allowlist."
                manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                allowlist.set(layout.projectDirectory.file("src/test/resources/security/exported-components.txt"))
            }
        tasks.named("check") { dependsOn(audit) }

        val variantName = variant.name.replaceFirstChar(Char::uppercase)
        tasks.register<ReleaseShrinkingCheck>("verify${variantName}Shrinking") {
            group = "verification"
            description = "Checks the R8 mapping of ${variant.name} against shrinking-requirements.txt (T-1800)."
            mapping.set(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE))
            requirements.set(layout.projectDirectory.file("shrinking-requirements.txt"))
        }
        tasks.register<ApkSizeCheck>("check${variantName}ApkSize") {
            group = "verification"
            description = "Fails when a ${variant.name} APK is over the plan §9 size budget (T-1800)."
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            loader.set(variant.artifacts.getBuiltArtifactsLoader())
            budgetBytes.set(apkBudgetBytes)
        }
    }
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar(Char::uppercase)
        tasks.register<VersionCheck>("verify${variantName}Version") {
            group = "verification"
            description = "Checks that ${variant.name} uses the version from -Ptaqvim.version or version.properties."
            expectedName.set(taqvimVersion.name)
            expectedCode.set(taqvimVersion.code)
            variant.outputs.forEach { output ->
                outputNames.add(output.versionName)
                outputCodes.add(output.versionCode)
            }
        }
    }
}
