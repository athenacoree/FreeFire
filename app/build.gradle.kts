plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.athenacoree.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.athenacoree.music"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"

        // Publishable key: safe for client apps. Never put a Supabase secret/service_role key here.
        buildConfigField("String", "SUPABASE_URL", "\"https://wlbnulfixkfmvcifvpum.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_rs8AM2PluCPJD1P4fhBznw_SrZSDh4F\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("MUSIC_KEYSTORE_FILE")
            if (!storeFilePath.isNullOrBlank()) storeFile = file(storeFilePath)
            storePassword = System.getenv("MUSIC_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("MUSIC_KEY_ALIAS")
            keyPassword = System.getenv("MUSIC_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("MUSIC_KEYSTORE_FILE") ?: "missing-release-keystore.p12")
            storePassword = System.getenv("MUSIC_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("MUSIC_KEY_ALIAS")
            keyPassword = System.getenv("MUSIC_KEY_PASSWORD")
            storeType = "PKCS12"
        }
    }

    buildTypes {\n        getByName("release") {\n            signingConfig = signingConfigs.getByName("release")\n            isMinifyEnabled = false\n        }\n    }\n\n    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

configurations.configureEach {
    resolutionStrategy.force(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.7",
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7",
        "androidx.browser:browser:1.8.0"
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.browser:browser:1.8.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.7")

    // Offline-first library and persistent preferences.
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Native playback for user-owned or otherwise authorized media only.
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-session:1.6.1")

    // Supabase Kotlin SDK: public catalog access for the first integration stage.
    implementation(platform("io.github.jan-tennert.supabase:bom:3.9.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Image loading for catalog artwork and thumbnails.
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.ktor:ktor-client-android:3.4.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}
