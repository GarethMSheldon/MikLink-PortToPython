# Annotation use-site targets

Kotlin 2.2 introduces new defaults for annotation targets on constructor properties (KT-73255). Being explicit keeps bytecode stable across toolchain updates.

## Guidelines
- Use `@param:` for DI qualifiers on constructor properties (e.g., `@param:ApplicationContext private val context: Context`).
- In questo repository **si usa esplicitamente `@param:Json`** sui parametri del primary constructor per DTO (data class); **`@field:Json` resta ammesso solo quando necessario** (ad es. proprietà con accessor personalizzato, proprietà delegate o quando non esiste un parametro corrispondente).

## Rationale
- Il compilatore può spostare annotazioni tra parametro/proprietà/field: dichiarare esplicitamente il target evita regressioni e mantiene stabile il bytecode.
- Annotare `@param:Json` sui parametri dei data class rende esplicito il mapping per gli adapter Kotlin/Moshi; usare `@field:Json` solo quando l'annotazione deve essere applicata al backing field.

## Examples
```kotlin
class FileWriter @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context
)

data class SpeedResult(
    @param:Json(name = "tcp-download") val tcpDownload: String?,
    @param:Json(name = "tcp-upload") val tcpUpload: String?
)

// Quando è necessario applicare l'annotazione al backing field:
data class FieldExample(
    @field:Json(name = "raw_value") val rawValue: String
)
```
