package dio.budgeting.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI budgetingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Budgeting API")
                .version("1.0.0")
                .description("""
                        API de controle de gastos com assistente de IA (Spring AI + OpenAI).
                        Registre e consulte transações pela API REST ou converse com o assistente por texto ou por voz.
                        Valores são enviados em centavos e devolvidos em reais."""));
    }
}
