package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistTransactionUseCaseTest {
    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    PersistTransactionUseCase useCase;

    @Test
    void should_saveTransaction_when_inputIsValid() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var output = useCase.execute(new PersistTransactionInput("Compra no mercado", 8000, Category.GROCERIES));

        assertThat(output.description()).isEqualTo("Compra no mercado");
        assertThat(output.value()).isEqualByComparingTo("80.00");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void should_notSaveTransaction_when_amountIsInvalid() {
        var input = new PersistTransactionInput("Compra no mercado", -500, Category.GROCERIES);

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(InvalidTransactionException.class);
        verifyNoInteractions(transactionRepository);
    }
}
