// Gradle 设置：声明插件仓库、依赖仓库与包含的模块
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 禁止在子模块里单独声明仓库，统一在这里管理
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pvz2tool"
include(":app")
