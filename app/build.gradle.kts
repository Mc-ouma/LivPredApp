import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    /*id("org.jetbrains.compose") version "1.8.0-dev1875"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"*/
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

// Load values from dot.env file
val dotEnvFile = file("${project.rootDir}/dot.env")
val dotEnvProps = Properties()

if (dotEnvFile.exists()) {
    dotEnvFile.inputStream().reader().use {
        val content = it.readText()
        content.split("\n").forEach { line ->
            if (!line.startsWith("//") && !line.startsWith("#") && line.contains("=")) {
                val (key, value) = line.split("=", limit = 2)
                dotEnvProps[key.trim()] = value.trim()
            }
        }
    }
}

android {
    namespace = "com.soccertips.predictx"
    compileSdkVersion(libs.versions.compileSdk.get().toInt())

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.soccertips.predictx"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 23
        versionName = "2.0.2"

        buildConfigField("boolean", "DEBUG", "true")

        // Use API keys from dot.env file
        buildConfigField("String", "DEFAULT_API_KEY", "\"${dotEnvProps["API_KEY"] ?: ""}\"")
        buildConfigField("String", "DEFAULT_API_HOST", "\"${dotEnvProps["API_HOST"] ?: ""}\"")
        buildConfigField("String", "API_BASE_URL", "\"${dotEnvProps["API_BASE_URL"] ?: ""}\"")
        buildConfigField(
                "String",
                "DAILY_BONUS_BASE_URL",
                "\"${dotEnvProps["DAILY_BONUS_BASE_URL"] ?: ""}\""
        )
        buildConfigField(
                "String",
                "API_BASE_URL_VALUE",
                "\"${dotEnvProps["API_BASE_URL_VALUE"] ?: ""}\""
        )

        javaCompileOptions {
            annotationProcessorOptions { arguments += "room.incremental" to "true" }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            // enableUnitTestCoverage = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }

    packaging { resources { excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") } }
    buildToolsVersion = "36.0.0"

    /*composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }*/
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "GradleDependency"
        disable += "MissingTranslation"
        disable += "NewApi"
        disable += "UnusedResources"
        disable += "InvalidPackage"
        disable += "GradleDependency"
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-opt-in=kotlin.Experimental")
    }
}

/*
Dependency versions are defined in the top level build.gradle file. This helps keeping track of
all versions in a single place. This improves readability and helps managing project complexity.
*/
dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.foundation.layout)

    implementation(libs.androidx.annotation)
    implementation(libs.timber)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.coroutines.guava)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.hilt.android.core)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx.v281)
    implementation(libs.kotlin.stdlib)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compiler)
    implementation(composeBom)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.foundation.core)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.windowsizeclass)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.accompanist.appcompat.theme)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.accompanist.permissions)

    debugImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.tooling.core)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.coil.kt.coil.compose.v240)
    implementation(libs.androidx.compiler)

    implementation(libs.sheets.m3)

    implementation(libs.lottie.compose)

    // App update
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // Review
    implementation(libs.review)
    implementation(libs.review.ktx)

    // shared elements
    implementation(libs.accompanist.navigation.material)


    // Splash Screen
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.dotenv.kotlin)

    // paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Firebase remote config
    implementation(libs.firebase.config)

    // Admob
    implementation(libs.play.services.ads)

    // User Messaging Platform (UMP) for consent management
    implementation(libs.user.messaging.platform)
}
