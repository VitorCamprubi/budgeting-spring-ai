package dio.budgeting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void should_createTransaction_when_dataIsValid() {
        var transaction = new Transaction("  Compra na farmácia  ", 4590, Category.PHARMA);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Compra na farmácia");
        assertThat(transaction.getAmount()).isEqualTo(4590);
        assertThat(transaction.getCategory()).isEqualTo(Category.PHARMA);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_rejectTransaction_when_descriptionIsBlank(String description) {
        assertThatThrownBy(() -> new Transaction(description, 1000, Category.GROCERIES))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("A descrição da transação é obrigatória");
    }

    @Test
    void should_rejectTransaction_when_descriptionIsTooLong() {
        var description = "a".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> new Transaction(description, 1000, Category.GROCERIES))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("no máximo 255 caracteres");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -8000})
    void should_rejectTransaction_when_amountIsNotPositive(long amount) {
        assertThatThrownBy(() -> new Transaction("Combustível", amount, Category.AUTO))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("O valor da transação deve ser maior que zero");
    }

    @Test
    void should_rejectTransaction_when_categoryIsMissing() {
        assertThatThrownBy(() -> new Transaction("Compra sem categoria", 1000, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("A categoria da transação é obrigatória");
    }
}
