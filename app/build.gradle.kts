import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// The TMDb v4 Read Access Token is never committed. Lookup order:
// local.properties → Gradle property (-P / ~/.gradle/gradle.properties) → environment variable (CI).
val tmdbTokenKey = "TMDB_ACCESS_TOKEN"
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val tmdbAccessToken: String = (
    localProperties.getProperty(tmdbTokenKey)
        ?: providers.gradleProperty(tmdbTokenKey).orNull
        ?: providers.environmentVariable(tmdbTokenKey).orNull
        ?: ""
    )
    // .properties files keep quotes literally; tolerate TOKEN="..." and TOKEN='...'.
    .trim().removeSurrounding("\"").removeSurrounding("'")
if (tmdbAccessToken.isBlank()) {
    logger.warn(
        "$tmdbTokenKey is not set. The app will build, but every TMDb request will fail " +
            "with Unauthorized. Add $tmdbTokenKey=<your TMDb v4 Read Access Token> to local.properties " +
            "(see README › Configuration)."
    )
}

android {
    namespace = "com.trianglz_task_ahmedhussin"
    compileSdk {
        // The latest stable AndroidX/Compose/OkHttp releases require compiling against API 37.
        // targetSdk stays at 36, so runtime behaviour does not change.
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.trianglz_task_ahmedhussin"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.trianglz_task_ahmedhussin.HiltTestRunner"

        buildConfigField("String", tmdbTokenKey, "\"$tmdbAccessToken\"")
    }

    buildTypes {
        debug {
            // Adds createDebugUnitTestCoverageReport (JaCoCo) for local unit tests.
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    // Exported schemas are committed so database migrations can be reviewed and tested.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // AndroidX core & lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines: declared directly so the app and its tests share one version (AndroidX alone
    // resolves an older core, which breaks kotlinx-coroutines-test in instrumented tests).
    implementation(libs.kotlinx.coroutines.android)

    // Logging (debug builds only plant a tree; release logs nothing)
    implementation(libs.timber)

    // Images
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Paging & Room
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.mockwebserver)

    // Instrumentation / UI tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.paging.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Debug-only tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
