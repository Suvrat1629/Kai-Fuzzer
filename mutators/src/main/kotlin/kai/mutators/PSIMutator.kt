package kai.mutators

import kai.interfaces.mutator.Mutator
import kai.model.FuzzInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import kotlin.random.Random

/**
 * PSI-based mutator that understands Kotlin syntax.
 * This mutator performs AST-aware mutations that produce valid Kotlin code.
 */
class PSIMutator : Mutator {
    override val name: String = "PSIMutator"
    override val description: String = "AST-aware mutations using Kotlin PSI"
    override val priority: Int = 5
    override val complexity: Int = 7

    private val random = Random(System.currentTimeMillis())
    private var rootDisposable: Disposable? = null
    private var environment: KotlinCoreEnvironment? = null

    override suspend fun initialize() {
        val configuration = CompilerConfiguration()
        rootDisposable = Disposer.newDisposable()
        environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable!!,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    override suspend fun shutdown() {
        rootDisposable?.let { Disposer.dispose(it) }
        rootDisposable = null
        environment = null
    }

    override suspend fun mutate(input: FuzzInput): FuzzInput = withContext(Dispatchers.IO) {
        val ktFile = parseKotlinFile(input.sourceCode) ?: return@withContext input

        // Use .entries instead of .values() to avoid deprecation
        val strategy = MutationStrategy.entries.random()

        try {
            val mutatedText = when (strategy) {
                MutationStrategy.DELETE_STATEMENT -> deleteRandomStatement(ktFile)
                MutationStrategy.DUPLICATE_STATEMENT -> duplicateRandomStatement(ktFile)
                MutationStrategy.SWAP_STATEMENTS -> swapRandomStatements(ktFile)
                MutationStrategy.INSERT_RETURN -> insertReturnStatement(ktFile)
                MutationStrategy.ADD_NULL_CHECK -> addNullCheck(ktFile)
                MutationStrategy.CHANGE_VISIBILITY -> changeVisibility(ktFile)
                MutationStrategy.ADD_ANNOTATION -> addRandomAnnotation(ktFile)
                MutationStrategy.CONVERT_TO_EXPRESSION_BODY -> convertToExpressionBody(ktFile)
            }

            FuzzInput(
                sourceCode = mutatedText ?: input.sourceCode,
                generation = input.generation + 1,
                parentId = input.id,
                tags = input.tags + "psi" + strategy.tag
            )
        } catch (e: Exception) {
            // If PSI manipulation fails (common in fuzzing), return original
            input
        }
    }

    override fun canMutate(input: FuzzInput): Boolean {
        return input.sourceCode.isNotBlank() && input.sourceCode.length < 50000 // Don't mutate huge files
    }

    private fun parseKotlinFile(sourceCode: String): KtFile? {
        return try {
            val env = environment ?: return null
            val psiFactory = KtPsiFactory(env.project, markGenerated = true)
            psiFactory.createFile("mutated.kt", sourceCode)
        } catch (e: Exception) {
            null
        }
    }

    // ============ Mutation Strategies ============

    private fun deleteRandomStatement(ktFile: KtFile): String? {
        val statements = ktFile.collectDescendantsOfType<KtExpression>()
        if (statements.isEmpty()) return ktFile.text

        // Deep copy the file to avoid invalidating the PSI tree
        val fileCopy = ktFile.copy() as KtFile
        val targets = fileCopy.collectDescendantsOfType<KtExpression>()
        targets.randomOrNull()?.delete()
        return fileCopy.text
    }

    private fun duplicateRandomStatement(ktFile: KtFile): String? {
        val statements = ktFile.collectDescendantsOfType<KtExpression>()
        if (statements.isEmpty()) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val targets = fileCopy.collectDescendantsOfType<KtExpression>()
        val toDuplicate = targets.randomOrNull() ?: return fileCopy.text
        val duplicate = toDuplicate.copy() as KtExpression
        toDuplicate.parent.addAfter(duplicate, toDuplicate)
        return fileCopy.text
    }

    private fun swapRandomStatements(ktFile: KtFile): String? {
        val statements = ktFile.collectDescendantsOfType<KtExpression>()
        if (statements.size < 2) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val targets = fileCopy.collectDescendantsOfType<KtExpression>()
        val stmt1 = targets.randomOrNull() ?: return fileCopy.text
        val stmt2 = targets.filter { it != stmt1 }.randomOrNull() ?: return fileCopy.text
        val text1 = stmt1.text
        val text2 = stmt2.text
        val psiFactory = KtPsiFactory(fileCopy.project)
        stmt1.replace(psiFactory.createExpression(text2))
        stmt2.replace(psiFactory.createExpression(text1))
        return fileCopy.text
    }

    private fun insertReturnStatement(ktFile: KtFile): String? {
        val functions = ktFile.collectDescendantsOfType<KtNamedFunction>().filter { it.bodyBlockExpression != null }
        if (functions.isEmpty()) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val targets = fileCopy.collectDescendantsOfType<KtNamedFunction>().filter { it.bodyBlockExpression != null }
        val function = targets.randomOrNull() ?: return fileCopy.text
        val body = function.bodyBlockExpression ?: return fileCopy.text
        val psiFactory = KtPsiFactory(fileCopy.project)
        val returnStmt = psiFactory.createExpression("return Unit")
        body.addBefore(returnStmt, body.rBrace)
        return fileCopy.text
    }

    private fun addNullCheck(ktFile: KtFile): String? {
        val references = ktFile.collectDescendantsOfType<KtDotQualifiedExpression>()
        if (references.isEmpty()) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val targets = fileCopy.collectDescendantsOfType<KtDotQualifiedExpression>()
        val ref = targets.randomOrNull() ?: return fileCopy.text
        val receiverText = ref.receiverExpression.text
        val selectorText = ref.selectorExpression?.text ?: return fileCopy.text
        val psiFactory = KtPsiFactory(fileCopy.project)
        val nullCheck = psiFactory.createExpression("$receiverText?.let { $selectorText }")
        ref.replace(nullCheck)
        return fileCopy.text
    }

    private fun changeVisibility(ktFile: KtFile): String? {
        val declarations = ktFile.collectDescendantsOfType<KtNamedDeclaration>().filter { it.modifierList != null }
        if (declarations.isEmpty()) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val targets = fileCopy.collectDescendantsOfType<KtNamedDeclaration>().filter { it.modifierList != null }
        val declaration = targets.randomOrNull() ?: return fileCopy.text
        val modifierList = declaration.modifierList ?: return fileCopy.text
        val psiFactory = KtPsiFactory(fileCopy.project)
        val currentIsPublic = modifierList.hasModifier(KtTokens.PUBLIC_KEYWORD)
        val newModifier = if (currentIsPublic) KtTokens.INTERNAL_KEYWORD else KtTokens.PUBLIC_KEYWORD
        modifierList.add(psiFactory.createModifierList(newModifier))
        modifierList.getModifier(if (currentIsPublic) KtTokens.PUBLIC_KEYWORD else KtTokens.INTERNAL_KEYWORD)?.delete()
        return fileCopy.text
    }

    private fun addRandomAnnotation(ktFile: KtFile): String? {
        val targets = ktFile.collectDescendantsOfType<KtNamedDeclaration>()
        if (targets.isEmpty()) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val annotationTargets = fileCopy.collectDescendantsOfType<KtNamedDeclaration>()
        val target = annotationTargets.randomOrNull() ?: return fileCopy.text
        val psiFactory = KtPsiFactory(fileCopy.project)
        val annotations = listOf(
            "@Suppress(\"UNUSED\")",
            "@Deprecated(\"Fuzzer generated\")",
            "@OptIn(ExperimentalStdlibApi::class)",
            "@JvmName(\"fuzzed\")",
            "@JvmStatic",
            "@JvmOverloads",
            "@Throws(Exception::class)",
            "@Synchronized"
        )
        val annotation = psiFactory.createAnnotationEntry(annotations.random())
        target.addAnnotationEntry(annotation)
        return fileCopy.text
    }

    private fun convertToExpressionBody(ktFile: KtFile): String? {
        val functions = ktFile.collectDescendantsOfType<KtNamedFunction>().filter { it.bodyBlockExpression?.statements?.size == 1 }
        if (functions.isEmpty()) return ktFile.text

        val fileCopy = ktFile.copy() as KtFile
        val targetFunctions = fileCopy.collectDescendantsOfType<KtNamedFunction>().filter { it.bodyBlockExpression?.statements?.size == 1 }
        val function = targetFunctions.randomOrNull() ?: return fileCopy.text
        val block = function.bodyBlockExpression ?: return fileCopy.text
        val singleStmt = block.statements.firstOrNull() as? KtReturnExpression ?: return fileCopy.text
        val psiFactory = KtPsiFactory(fileCopy.project)
        val expressionBody = psiFactory.createExpression(singleStmt.returnedExpression?.text ?: "Unit")
        function.bodyExpression!!.replace(expressionBody)
        return fileCopy.text
    }

    private enum class MutationStrategy(val tag: String) {
        DELETE_STATEMENT("delete"),
        DUPLICATE_STATEMENT("duplicate"),
        SWAP_STATEMENTS("swap"),
        INSERT_RETURN("insert.return"),
        ADD_NULL_CHECK("nullcheck"),
        CHANGE_VISIBILITY("visibility"),
        ADD_ANNOTATION("annotation"),
        CONVERT_TO_EXPRESSION_BODY("expressionbody")
    }
}