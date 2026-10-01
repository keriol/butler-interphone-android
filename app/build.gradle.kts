import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun runtimeValue(name: String): String =
    providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: localProperties.getProperty(name)
        ?: ""

fun quoted(value: String): String =
    "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""

val buildDate = runtimeValue("INTERPHONE_BUILD_DATE").ifBlank {
    java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
}

val releaseKeystorePath = providers.environmentVariable(
    "INTERPHONE_KEYSTORE_PATH"
).orNull
val releaseKeystorePassword = providers.environmentVariable(
    "INTERPHONE_KEYSTORE_PASSWORD"
).orNull
val releaseKeyAlias = providers.environmentVariable(
    "INTERPHONE_KEY_ALIAS"
).orNull
val releaseKeyPassword = providers.environmentVariable(
    "INTERPHONE_KEY_PASSWORD"
).orNull

val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "io.github.keriol.butlerinterphone"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.keriol.butlerinterphone"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1.dev0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "BIFROST_URL",
            quoted(runtimeValue("INTERPHONE_BIFROST_URL")),
        )
        buildConfigField(
            "String",
            "BIFROST_TOKEN",
            quoted(runtimeValue("INTERPHONE_BIFROST_TOKEN")),
        )
        buildConfigField(
            "String",
            "BUILD_DATE",
            quoted(buildDate),
        )
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(
                    checkNotNull(releaseKeystorePath)
                )
                storePassword = checkNotNull(
                    releaseKeystorePassword
                )
                keyAlias = checkNotNull(releaseKeyAlias)
                keyPassword = checkNotNull(
                    releaseKeyPassword
                )
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}


tasks.register("printVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}
