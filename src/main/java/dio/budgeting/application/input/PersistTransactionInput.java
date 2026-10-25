package dio.budgeting.application.input;

import dio.budgeting.domain.Category;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;

public record PersistTransactionInput(
        @ToolParam(description = "Descrição curta do gasto, por exemplo: Compra no mercado")
        String description,

        @ToolParam(description = "Valor do gasto em centavos. Exemplos: 80 reais = 8000; 12 reais e 50 centavos = 1250")
        long amount,

        @ToolParam(description = "Categoria do gasto: GROCERIES (mercado), PHARMA (farmácia), AUTO (carro), "
                + "RESTAURANT (restaurantes e lanches), LEISURE (lazer) ou OTHER (outros)")
        Category category,

        @ToolParam(description = "Data do gasto no formato yyyy-MM-dd. Se o usuário não informar, deixe vazio "
                + "que será usada a data de hoje", required = false)
        LocalDate date) {
}
