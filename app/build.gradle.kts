// app 模块构建脚本
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 签名配置从 local.properties 读取（该文件不提交仓库）；
// 未配置时 release 构建仍可执行，只是产出未签名 APK。
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProperty(key: String): String? =
    localProps.getProperty(key)?.takeIf { it.isNotBlank() }

val releaseStoreFile = localProperty("RELEASE_STORE_FILE")
val releaseStorePassword = localProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.pvz2tool"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pvz2tool"
        minSdk = 26          // 文档要求
        targetSdk = 35       // 当前稳定版，可按需调整
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose（BOM 统一版本）
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Room（KSP 生成代码）
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore（设置）
    implementation(libs.datastore.preferences)

    // Coil（图片加载 + 磁盘缓存）
    implementation(libs.coil.compose)

    // JSON 序列化（导入导出）
    implementation(libs.kotlinx.serialization.json)
}
