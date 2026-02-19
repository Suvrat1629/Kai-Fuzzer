package kai.interfaces.mutator

import kai.model.FuzzInput

/**
 * Mutation strategy for generating new test cases from existing ones.
 * Different mutators target different compiler subsystems.
 */
interface Mutator {

    /**
     * Create a mutated version of the input.
     * Should produce syntactically valid Kotlin when possible.
     */
    suspend fun mutate(input: FuzzInput): FuzzInput

    /**
     * Check if this mutator can handle the input.
     * Some mutators may require specific code structures.
     */
    fun canMutate(input: FuzzInput): Boolean

    /** Display name for logging and configuration */
    val name : String

    /** Description of mutation strategy */
    val description: String

    /** Priority when multiple mutators are available (1=low, 10=high) */
    val priority: Int get() = 5

    /** Estimated complexity (1=fast, 10=slow) */
    val complexity: Int get() = 3

    /** Initialize mutator resources (e.g., load grammar files) */
    suspend fun initialize()

    /** Cleanup mutator resources */
    suspend fun shutdown()
}