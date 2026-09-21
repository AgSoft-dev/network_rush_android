plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.triviamap"
    compileSdk = 37

    val testAppId = "ca-app-pub-3940256099942544~3347511713"
    val testBannerId = "ca-app-pub-3940256099942544/9214589741"

    defaultConfig {
        applicationId = "com.agsoft.networkrush"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob: Google's public TEST ids. Release builds use the real ids from ~/.gradle/gradle.properties (or -P):
        //   admobAppId=ca-app-pub-XXXX~YYYY   admobBannerId=ca-app-pub-XXXX/ZZZZ   adsEnabled=false (kill switch)
        // Debug builds always keep the test ids: clicking your own live ads can get the AdMob account suspended.
        manifestPlaceholders["admobAppId"] = testAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
        buildConfigField("boolean", "ADS_ENABLED", providers.gradleProperty("adsEnabled").getOrElse("true"))
    }

    // Upload key for Play App Signing. Values live in ~/.gradle/gradle.properties (never in the repo):
    //   nrStoreFile=/abs/path/networkrush-upload.jks  nrStorePassword=...  nrKeyAlias=upload  nrKeyPassword=...
    // Without them the release build is simply left unsigned (debug and CI builds are unaffected).
    val releaseSigning = providers.gradleProperty("nrStoreFile").orNull?.let { storeFile ->
        signingConfigs.create("release") {
            this.storeFile = file(storeFile)
            storePassword = providers.gradleProperty("nrStorePassword").get()
            keyAlias = providers.gradleProperty("nrKeyAlias").get()
            keyPassword = providers.gradleProperty("nrKeyPassword").get()
        }
    }

    buildTypes {
        release {
            releaseSigning?.let { signingConfig = it }
            providers.gradleProperty("admobAppId").orNull?.let { manifestPlaceholders["admobAppId"] = it }
            providers.gradleProperty("admobBannerId").orNull?.let {
                buildConfigField("String", "ADMOB_BANNER_ID", "\"$it\"")
            }
            isMinifyEnabled = true
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

    testOptions {
        // android.jar stubs (SystemClock, ...) return defaults instead of throwing in JVM tests
        unitTests.isReturnDefaultValues = true
    }

    // Exported Room schemas are read by MigrationTestHelper
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// room-testing needs kotlinx-serialization >= 1.8 at runtime; the Kotlin toolchain pins 1.7.3 (AbstractMethodError)
configurations.matching { it.name.contains("AndroidTest", ignoreCase = true) && it.name.contains("Runtime") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization-")) {
            useVersion("1.8.1")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.gson)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)

    implementation(libs.play.services.ads)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}