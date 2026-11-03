// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // KSP: genera en compilación el código de Room (DAO y base de datos)
    alias(libs.plugins.ksp) apply false
}
