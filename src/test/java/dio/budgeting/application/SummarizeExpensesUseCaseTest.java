package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummarizeExpensesUseCaseTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    @Mock
    TransactionRepository transactionRepository;

    SummarizeExpensesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SummarizeExpensesUseCase(transactionRepository, Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));
    }

    @Test
    void should_sumTotalsByCategory_orderedFromHighestToLowest() {
        var range = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25));
        when(transactionRepository.findAllByDateRange(range)).thenReturn(List.of(
                new Transaction("Mercado", 8000, Category.GROCERIES, LocalDate.of(2026, 10, 3)),
                new Transaction("Feira", 4050, Category.GROCERIES, LocalDate.of(2026, 10, 10)),
                new Transaction("Remédio", 3590, Category.PHARMA, LocalDate.of(2026, 10, 12)),
                new Transaction("Combustível", 20000, Category.AUTO, LocalDate.of(2026, 10, 20))));

        var summary = useCase.execute(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25));

        assertThat(summary.total()).isEqualByComparingTo("356.40");
        assertThat(summary.transactionCount()).isEqualTo(4);
        assertThat(summary.byCategory()).extracting(CategoryTotal::category)
                .containsExactly("AUTO", "GROCERIES", "PHARMA");
        assertThat(summary.byCategory().get(1).total()).isEqualByComparingTo("120.50");
        assertThat(summary.byCategory().get(1).transactionCount()).isEqualTo(2);
    }

    @Test
    void should_useCurrentMonth_when_datesAreNotInformed() {
        var currentMonth = new DateRange(LocalDate.of(2026, 10, 1), TODAY);
        when(transactionRepository.findAllByDateRange(currentMonth)).thenReturn(List.of());

        var summary = useCase.execute(null, null);

        assertThat(summary.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(summary.to()).isEqualTo(TODAY);
        assertThat(summary.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.byCategory()).isEmpty();
    }
}
