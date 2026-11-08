plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val reverseEngineeringReport = rootProject.file("report.md").readText()
fun reportCredential(label: String): String {
    val line = reverseEngineeringReport.lineSequence()
        .firstOrNull { it.startsWith("- **$label**") }
        ?: error("Missing configured map credential: $label")
    val separator = maxOf(line.indexOf(':'), line.indexOf('：'))
    return line.substring(separator + 1).trim()
        .removeSurrounding(96.toChar().toString())
        .takeIf { it.isNotBlank() }
        ?: error("Missing configured map credential: $label")
}
fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.scluzlep.newmangps"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.scluzlep.newmangps"
        minSdk = 26
        targetSdk = 36
        versionCode = 2110722000
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "AMAP_WEB_KEY",
            buildConfigString(reportCredential("高德地图 Web API Key"))
        )
        buildConfigField(
            "String",
            "AMAP_SECURITY_CODE",
            buildConfigString(reportCredential("高德地图安全密钥（SecurityJsCode）"))
        )
        buildConfigField(
            "String",
            "AMAP_WEB_SERVICE_KEY",
            buildConfigString(reportCredential("高德 WebService API Key"))
        )
    }

    signingConfigs {
        create("ciRelease") {
            System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("SIGNING_PASSWORD") ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!System.getenv("ANDROID_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("ciRelease")
            }
            isMinifyEnabled = true
            isShrinkResources = true
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.miuix.ui)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
