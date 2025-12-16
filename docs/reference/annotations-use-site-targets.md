# Annotation use-site targets

Kotlin 2.2 introduces new defaults for annotation targets on constructor properties (KT-73255). Being explicit keeps bytecode stable across toolchain updates.

## Guidelines
- Use `@param:` for DI qualifiers on constructor properties (e.g., `@param:ApplicationContext private val context: Context`).
- Use `@field:` for Moshi `@Json` mappings on primary constructor properties to ensure the annotation is applied to the generated backing field.

## Rationale
- The compiler may otherwise shift annotations between parameter/property/field targets, causing warnings and Hilt/Moshi to miss qualifiers/field names.
- Kotlin docs: https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets

## Examples
```kotlin
class FileWriter @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context
)

data class SpeedResult(
    @field:Json(name = "tcp-download") val tcpDownload: String?,
    @field:Json(name = "tcp-upload") val tcpUpload: String?
)
```
