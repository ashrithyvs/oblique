import java.io.FileInputStream
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
require(localPropertiesFile.exists()) {
    "Missing android/local.properties. Copy local.properties.example and set API_BASE_URL."
}
FileInputStream(localPropertiesFile).use { localProperties.load(it) }

val apiBaseUrlRaw = localProperties.getProperty("API_BASE_URL")?.trim().orEmpty()
require(apiBaseUrlRaw.isNotBlank()) {
    "API_BASE_URL must be set in android/local.properties"
}

val apiBaseUrl = if (apiBaseUrlRaw.endsWith("/")) apiBaseUrlRaw else "$apiBaseUrlRaw/"

val apiHost = URI(apiBaseUrl).host
require(!apiHost.isNullOrBlank()) {
    "API_BASE_URL must be a valid URL with a host (e.g. http://10.0.2.2:3000/)"
}

val devMode = localProperties.getProperty("DEV_MODE")?.trim()?.equals("true", ignoreCase = true) == true

val generatedNetworkSecurityDir = layout.buildDirectory.dir("generated/network_security_config")

tasks.register("generateNetworkSecurityConfig") {
    val outDir = generatedNetworkSecurityDir.get().asFile
    val outFile = outDir.resolve("xml/network_security_config.xml")
    outputs.file(outFile)
    doLast {
        outFile.parentFile.mkdirs()
        outFile.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="true">$apiHost</domain>
                </domain-config>
            </network-security-config>
            """.trimIndent()
        )
    }
}

tasks.named("preBuild").configure {
    dependsOn("generateNetworkSecurityConfig")
}

android {
    namespace = "com.example.oblique_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.oblique_android"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "DEV_MODE", devMode.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.srcDir(generatedNetworkSecurityDir)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-ktx:1.9.3")

    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
