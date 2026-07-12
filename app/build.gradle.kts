plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

fun apiBaseUrlOverride(): String {
    val value = providers.gradleProperty("apiBaseUrl")
        .orElse(providers.environmentVariable("CHZ_API_BASE_URL"))
        .orNull
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    return value ?: "https://api.chestniy-z.ru/api/v1/"
}

fun apiModeFor(baseUrl: String): String =
    if (baseUrl.trimEnd('/').endsWith("/api/v1")) "SAAS" else "LEGACY"

fun quotedBuildConfigString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "ru.devandprod.chestniyznak"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.devandprod.chestniyznak"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "1.0.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val apiBaseUrl = apiBaseUrlOverride()
        buildConfigField("String", "API_BASE_URL", quotedBuildConfigString(apiBaseUrl))
        buildConfigField("String", "API_MODE", quotedBuildConfigString(apiModeFor(apiBaseUrl)))
        buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
    }

    buildTypes {
        debug {
            val apiBaseUrl = apiBaseUrlOverride()
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", quotedBuildConfigString(apiBaseUrl))
            buildConfigField("String", "API_MODE", quotedBuildConfigString(apiModeFor(apiBaseUrl)))
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "API_BASE_URL", "\"https://api.chestniy-z.ru/api/v1/\"")
            buildConfigField("String", "API_MODE", "\"SAAS\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.google.material)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.mlkit.barcode)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.matching { it.name.startsWith("lintAnalyze") }.configureEach {
    dependsOn(tasks.matching { task -> task.name.startsWith("hiltJavaCompile") })
}
