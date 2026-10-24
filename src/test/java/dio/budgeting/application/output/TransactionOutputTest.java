package dio.budgeting.application.output;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOutputTest {

    @Test
    void should_convertCentsToReais_when_mappingTransaction() {
        var transaction = new Transaction("Compra no mercado", 8050, Category.GROCERIES);

        var output = TransactionOutput.from(transaction);

        assertThat(output.value()).isEqualByComparingTo(new BigDecimal("80.50"));
        assertThat(output.description()).isEqualTo("Compra no mercado");
        assertThat(output.category()).isEqualTo("GROCERIES");
        assertThat(output.id()).isEqualTo(transaction.getId().uuid().toString());
    }
}
