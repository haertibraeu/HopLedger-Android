plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("app/src/*/kotlin/**/*.kt", "app/src/*/java/**/*.kt")
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_no-unused-imports" to "enabled",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                ),
            )
    }

    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint()
    }

    flexmark {
        target("**/*.md")
        flexmark()
    }

    format("misc") {
        target(".gitattributes", ".gitignore", "gradle.properties")
        trimTrailingWhitespace()
        leadingSpacesToTabs()
        endWithNewline()
    }
}
