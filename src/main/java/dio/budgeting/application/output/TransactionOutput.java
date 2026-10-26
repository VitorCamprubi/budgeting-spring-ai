package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O domínio guarda o valor em centavos (long) para evitar erros de arredondamento.
 * Na saída, o valor é convertido para reais com duas casas decimais.
 */
public record TransactionOutput(String id, String description, String category, BigDecimal value, LocalDate date) {
    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                BigDecimal.valueOf(transaction.getAmount(), 2),
                transaction.getDate());
    }
}
