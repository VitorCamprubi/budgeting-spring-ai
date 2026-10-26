package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.InvalidDateRangeException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListTransactionsByPeriodUseCaseTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    @Mock
    TransactionRepository transactionRepository;

    ListTransactionsByPeriodUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListTransactionsByPeriodUseCase(transactionRepository, Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));
    }

    @Test
    void should_listAllCategories_when_categoryIsNotInformed() {
        var range = new DateRange(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25));
        when(transactionRepository.findAllByDateRange(range)).thenReturn(List.of(
                new Transaction("Pizza", 6500, Category.RESTAURANT, LocalDate.of(2026, 10, 24))));

        var result = useCase.execute(range.from(), range.to(), null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().category()).isEqualTo("RESTAURANT");
        verify(transactionRepository, never()).findAllByCategoryAndDateRange(any(), any());
    }

    @Test
    void should_filterByCategory_when_categoryIsInformed() {
        var currentMonth = new DateRange(LocalDate.of(2026, 10, 1), TODAY);
        when(transactionRepository.findAllByCategoryAndDateRange(Category.AUTO, currentMonth)).thenReturn(List.of());

        var result = useCase.execute(null, null, Category.AUTO);

        assertThat(result).isEmpty();
        verify(transactionRepository).findAllByCategoryAndDateRange(Category.AUTO, currentMonth);
    }

    @Test
    void should_rejectPeriod_when_fromIsAfterTo() {
        assertThatThrownBy(() -> useCase.execute(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 1), null))
                .isInstanceOf(InvalidDateRangeException.class);
        verifyNoInteractions(transactionRepository);
    }
}
