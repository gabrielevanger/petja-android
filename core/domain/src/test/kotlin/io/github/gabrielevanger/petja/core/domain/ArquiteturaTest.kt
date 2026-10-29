package io.github.gabrielevanger.petja.core.domain

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Testes arquiteturais do projeto PetJá.
 *
 * Konsist analisa o código-fonte em tempo de compilação/teste para garantir
 * que as convenções de arquitetura sejam mantidas ao longo do crescimento do projeto.
 */
class ArquiteturaTest {

    /**
     * O módulo core/domain é puro Kotlin: não deve referenciar nada de `android.*`
     * nem de `androidx.*` para permanecer testável sem o runtime do Android.
     */
    @Test
    fun `domain nao importa classes Android`() {
        Konsist
            .scopeFromModule("core/domain")
            .files
            .assertFalse(
                additionalMessage = "core/domain não pode importar android.* nem androidx.*" +
                    " (use abstrações puras de Kotlin em vez de APIs da plataforma)",
            ) { arquivo ->
                arquivo.imports.any { import ->
                    import.name.startsWith("android.") || import.name.startsWith("androidx.")
                }
            }
    }

    /**
     * Todo ViewModel deve ser anotado com `@HiltViewModel` e herdar de `ViewModel`
     * para que o Hilt gerencie o ciclo de vida corretamente.
     * Classes de teste (sufixo `Test`) são excluídas da verificação.
     */
    @Test
    fun `ViewModels tem HiltViewModel e herdam de ViewModel`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withNameEndingWith("ViewModel")
            .filter { classe -> !classe.name.endsWith("ViewModelTest") && !classe.hasAbstractModifier }
            .assertTrue(
                additionalMessage = "Toda classe *ViewModel concreta deve ter @HiltViewModel e herdar de ViewModel." +
                    " Confira se o import de dagger.hilt.android.lifecycle.HiltViewModel está presente.",
            ) { classe ->
                val temAnnotation = classe.annotations.any { it.name == "HiltViewModel" }
                val herda = classe.hasParentWithName("ViewModel", indirectParents = true)
                temAnnotation && herda
            }
    }

    /**
     * Arquivos de UiState não devem expor `List<` mutável no estado da UI.
     * Use `ImmutableList` (kotlinx-collections-immutable) para garantir
     * recomposições inteligentes no Compose e imutabilidade do estado.
     */
    @Test
    fun `UiState nao usa List mutavel`() {
        Konsist
            .scopeFromProject()
            .files
            // No Konsist, name é o nome do arquivo sem a extensão.
            .filter { arquivo -> arquivo.name.endsWith("UiState") }
            .assertFalse(
                additionalMessage = "UiState não deve declarar List<...> mutável." +
                    " Use ImmutableList<...> de kotlinx-collections-immutable.",
            ) { arquivo ->
                // Detecta "List<" que NÃO seja precedida por "Immutable" ou "Persistent"
                val conteudo = arquivo.text
                val regexListaMutavel = Regex("(?<!Immutable)(?<!Persistent)List<")
                regexListaMutavel.containsMatchIn(conteudo)
            }
    }

    /**
     * Screens são stateless: só a função Route (que fica no mesmo arquivo *Screen.kt)
     * pode obter o ViewModel com `hiltViewModel()` ou `viewModel()`. A Screen e as
     * partes da tela recebem estado e `onAction`.
     */
    @Test
    fun `Screens sao stateless, so a Route obtem o ViewModel`() {
        val obtemViewModel = Regex("""\b(hiltViewModel|viewModel)\(""")
        Konsist
            .scopeFromProject()
            .files
            .filter { arquivo -> arquivo.name.endsWith("Screen") }
            .functions()
            .filter { funcao -> !funcao.name.endsWith("Route") }
            .assertFalse(
                additionalMessage = "Só a função *Route pode chamar hiltViewModel() ou viewModel()." +
                    " A Screen recebe o estado e onAction.",
            ) { funcao -> obtemViewModel.containsMatchIn(funcao.text) }
    }
}
