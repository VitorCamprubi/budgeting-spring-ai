package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistTransactionUseCaseTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    @Mock
    TransactionRepository transactionRepository;

    PersistTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        useCase = new PersistTransactionUseCase(transactionRepository, clock);
    }

    @Test
    void should_saveTransaction_when_inputIsValid() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var date = LocalDate.of(2026, 10, 20);

        var output = useCase.execute(new PersistTransactionInput("Compra no mercado", 8000, Category.GROCERIES, date));

        assertThat(output.description()).isEqualTo("Compra no mercado");
        assertThat(output.value()).isEqualByComparingTo("80.00");
        assertThat(output.date()).isEqualTo(date);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void should_useToday_when_dateIsNotInformed() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var output = useCase.execute(new PersistTransactionInput("Remédio", 3590, Category.PHARMA, null));

        assertThat(output.date()).isEqualTo(TODAY);
    }

    @Test
    void should_notSaveTransaction_when_amountIsInvalid() {
        var input = new PersistTransactionInput("Compra no mercado", -500, Category.GROCERIES, null);

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(InvalidTransactionException.class);
        verifyNoInteractions(transactionRepository);
    }
}
