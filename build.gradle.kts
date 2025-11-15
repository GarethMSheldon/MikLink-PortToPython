// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.ksp) apply false
}

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate

fun countFiles(glob: String): Int {
    val matcher = java.nio.file.FileSystems.getDefault().getPathMatcher("glob:$glob")
    return Files.walk(project.projectDir.toPath())
        .filter { p -> matcher.matches(project.projectDir.toPath().relativize(p)) }
        .count().toInt()
}

fun replaceFirstRegex(content: String, pattern: Regex, replacement: String): String {
    val match = pattern.find(content) ?: return content
    return content.replaceRange(match.range, replacement)
}

tasks.register("docsUpdateProjectState") {
    group = "documentation"
    description = "Aggiorna data e statistiche in PROJECT_STATE_DOCUMENTATION.md"
    doLast {
        val docPath = project.rootDir.toPath().resolve("PROJECT_STATE_DOCUMENTATION.md")
        var text = Files.readString(docPath)

        // Update date at the first occurrence
        val today = LocalDate.now().toString()
        text = replaceFirstRegex(
            text,
            Regex("""\*\*Stato del Progetto al:\s*\d{4}-\d{2}-\d{2}\*\*"""),
            "**Stato del Progetto al: $today**"
        )

        // Compute stats
        val kotlinTotal = countFiles("**/app/src/main/java/**/*.kt")
        val screens = countFiles("**/app/src/main/java/**/ui/**/*Screen.kt")
        val viewModels = countFiles("**/app/src/main/java/**/ui/**/*ViewModel.kt")
        val entities = countFiles("**/app/src/main/java/**/data/db/model/*.kt")
        val daos = countFiles("**/app/src/main/java/**/data/db/dao/*.kt")
        val repositories = countFiles("**/app/src/main/java/**/data/repository/*.kt")

        // Replace stats numbers in the STATISTICHE PROGETTO section (first occurrence per label)
        text = text.replace(Regex("(?m)(- \\*\\*Total Kotlin Files\\*\\*: )\\~?\\d+"), "$1$kotlinTotal")
        text = text.replace(Regex("(?m)(- \\*\\*Screens\\*\\*: )\\~?\\d+"), "$1$screens")
        text = text.replace(Regex("(?m)(- \\*\\*ViewModels\\*\\*: )\\~?\\d+"), "$1$viewModels")
        text = text.replace(Regex("(?m)(- \\*\\*Database Entities\\*\\*: )\\~?\\d+"), "$1$entities")
        text = text.replace(Regex("(?m)(- \\*\\*DAOs\\*\\*: )\\~?\\d+"), "$1$daos")
        text = text.replace(Regex("(?m)(- \\*\\*Repositories\\*\\*: )\\~?\\d+"), "$1$repositories")

        Files.writeString(docPath, text, StandardOpenOption.TRUNCATE_EXISTING)
        println("PROJECT_STATE_DOCUMENTATION.md aggiornato: data=$today, kt=$kotlinTotal, screens=$screens, vms=$viewModels, entities=$entities, daos=$daos, repos=$repositories")
    }
}
