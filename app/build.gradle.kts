plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.triviamap"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.triviamap"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // AdMob ids: Google's public TEST ids unless real ones are provided, in ~/.gradle/gradle.properties or with -P:
        //   admobAppId=ca-app-pub-XXXX~YYYY   admobBannerId=ca-app-pub-XXXX/ZZZZ   adsEnabled=false (kill switch)
        val testAppId = "ca-app-pub-3940256099942544~3347511713"
        val testBannerId = "ca-app-pub-3940256099942544/9214589741"
        manifestPlaceholders["admobAppId"] = providers.gradleProperty("admobAppId").getOrElse(testAppId)
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${providers.gradleProperty("admobBannerId").getOrElse(testBannerId)}\"")
        buildConfigField("boolean", "ADS_ENABLED", providers.gradleProperty("adsEnabled").getOrElse("true"))
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
}