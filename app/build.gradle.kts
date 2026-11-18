plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("io.github.takahirom.roborazzi")
}

import java.util.Properties

android {
    namespace = "com.legionforge.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.legionforge.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 334
                                                versionName = "2.57.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signature STABLE pour le debug APK : le keystore est stocké en secret GitHub
    // (LEGIONFORGE_KEYSTORE, base64) et décodé par le workflow dans app/keystore.jks.
    // Sans ça, chaque runner CI génère un keystore debug différent -> signature change
    // -> Android refuse la mise à jour par-dessus (il faut désinstaller/réinstaller).
    signingConfigs {
        create("stable") {
            val ks = file("keystore.jks")
            if (ks.exists()) {
                storeFile = ks
                storePassword = "legionforge2026"
                keyAlias = "legionforge"
                keyPassword = "legionforge2026"
            }
        }
        // Signature RELEASE pour le Google Play : keystore + credentials injectés par le
        // workflow via release-keystore.properties (gitignored). NE JAMAIS committer de secret.
        create("release") {
            val propsFile = file("release-keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply { load(propsFile.inputStream()) }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (file("release-keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("stable")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Roborazzi : ressources Android necessaires a stringResource/painterResource sous Robolectric.
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("com.google.code.gson:gson:2.11.0")
        implementation("com.google.android.gms:play-services-nearby:19.2.0")

    testImplementation("junit:junit:4.13.2")
    // Roborazzi 1.47.0 : dernier compatible Kotlin 1.9 (1.48+ exige Kotlin 2.0).
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.47.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.47.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}