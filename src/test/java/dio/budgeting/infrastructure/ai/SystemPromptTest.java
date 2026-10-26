package dio.budgeting.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O prompt de sistema é um template: chaves soltas no texto quebram a renderização só em tempo de execução,
 * na primeira chamada à IA. Este teste pega esse erro antes.
 */
class SystemPromptTest {

    @Test
    void should_renderSystemPromptWithoutPlaceholders() {
        var prompt = new PromptTemplate(new ClassPathResource("prompts/system-message.st")).render();

        assertThat(prompt)
                .contains("centavos")
                .contains("current-date")
                .contains("summarize-expenses")
                .doesNotContain("{");
    }
}
