import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

private data class ForbiddenPattern(val regex: Regex, val message: String)

abstract class ForbiddenPatternsTask : DefaultTask() {

    @get:Input
    abstract val roots: ListProperty<String>

    init {
        description = "Fails the build if deprecated/forbidden patterns are present in source or docs."
        group = "verification"
        roots.convention(listOf("app/src", "docs"))
    }

    @TaskAction
    fun check() {
        val patterns = listOf(
            ForbiddenPattern(
                regex = Regex("""fallbackToDestructiveMigration\(\s*\)"""),
                message = "Use fallbackToDestructiveMigration(dropAllTables = true)"
            ),
            ForbiddenPattern(
                regex = Regex("""\bTabRow\("""),
                message = "Use PrimaryTabRow or SecondaryTabRow"
            ),
            ForbiddenPattern(
                regex = Regex("""\bScrollableTabRow\("""),
                message = "Use PrimaryTabRow or SecondaryTabRow"
            ),
            ForbiddenPattern(
                regex = Regex("""centerAlignedTopAppBarColors\("""),
                message = "Replace with TopAppBarDefaults.topAppBarColors"
            ),
            ForbiddenPattern(
                regex = Regex("""@ApplicationContext\s+private\s+val"""),
                message = "Add @param: to the qualifier on constructor properties"
            ),
            ForbiddenPattern(
                regex = Regex("""@(?!(field:|param:))Json\([^)]*\)\s+(val|var)"""),
                message = "Apply explicit use-site target: @param:Json or @field:Json on Moshi-mapped constructor properties"
            )
        )

        val violations = mutableListOf<String>()
        roots.get().map { project.file(it) }
            .filter { it.exists() }
            .forEach { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in setOf("kt", "kts", "md", "txt") }
                    .forEach { file ->
                        val relativePath = file.relativeTo(project.projectDir).toString().replace(File.separatorChar, '/')
                        file.readLines().forEachIndexed { index, line ->
                            patterns.forEach { pattern ->
                                pattern.regex.findAll(line).forEach {
                                    val lineNumber = index + 1
                                    violations.add("$relativePath:$lineNumber -> ${pattern.message}")
                                }
                            }
                        }
                    }
            }

        if (violations.isNotEmpty()) {
            violations.forEach { logger.error(it) }
            throw GradleException("Forbidden patterns found (${violations.size}); see log for details.")
        } else {
            logger.lifecycle("checkForbiddenPatterns: no forbidden patterns found in ${roots.get().joinToString()}.")
        }
    }
}
