plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("com.diffplug.spotless") version "8.4.0"
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
