// shared/src/commonMain/kotlin/kai/model/FuzzInput.kt
package kai.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Represents a Kotlin program to be fuzzed with metadata for evolutionary tracking.
 * @property sourceCode The Kotlin source code
 * @property id Unique identifier (UUID-based)
 * @property generation Evolutionary generation number
 * @property parentId ID of parent input (for genealogy)
 * @property tags Classification tags for filtering and analysis
 * @property createdAt Timestamp of creation
 */
@Serializable
data class FuzzInput(
    val sourceCode: String,
    val id: String = generateId(),
    val generation: Int = 0,
    val parentId: String? = null,
    val tags: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Generate a unique identifier for fuzzing inputs.
         * Uses UUID v4 for collision resistance in high-throughput scenarios.
         */
        public fun generateId(): String = "fuzz_${UUID.randomUUID()}"

        /**
         * Parse a FuzzInput from JSON string.
         */
        fun fromJson(json: String): FuzzInput {
            return Json.decodeFromString(serializer(), json)

        }
    }

    /**
     * Convert to JSON string for persistence.
     */
    fun toJson(): String = Json.encodeToString(serializer(), this)

    /**
     * Compute a content hash for deduplication.
     */
    fun contentHash(): String = sourceCode.hashCode().toString(16)
}