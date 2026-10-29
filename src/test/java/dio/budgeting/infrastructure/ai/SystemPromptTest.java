package dio.budgeting.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O prompt de sistema é um template: chaves soltas no texto ou variáveis sem valor quebram a renderização
 * só em tempo de execução, na primeira chamada à IA. Este teste pega esse erro antes.
 */
class SystemPromptTest {

    @Test
    void should_renderSystemPromptWithTodaysDate() {
        var template = new PromptTemplate(new ClassPathResource("prompts/system-message.st"));

        var prompt = template.render(Map.of(
                "today", "2026-10-28 (quarta-feira)",
                "recentDays", "- 2026-10-28 (quarta-feira) - hoje\n- 2026-10-24 (sábado)"));

        assertThat(prompt)
                .contains("Hoje é 2026-10-28 (quarta-feira).")
                .contains("- 2026-10-24 (sábado)")
                .contains("centavos")
                .contains("summarize-expenses")
                .contains("RESTAURANT")
                .doesNotContain("{");
    }
}
