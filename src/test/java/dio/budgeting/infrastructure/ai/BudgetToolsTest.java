package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa as tools do jeito que o modelo de linguagem as chama (nome + argumentos em JSON),
 * sem precisar da OpenAI. Garante que os nomes, os parâmetros e a conversão de datas e
 * categorias continuam funcionando.
 */
@ExtendWith(MockitoExtension.class)
class BudgetToolsTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    @Mock
    TransactionRepository transactionRepository;

    Map<String, ToolCallback> tools;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);

        var callbacks = ToolCallbacks.from(
                new PersistTransactionUseCase(transactionRepository, clock),
                new ListTransactionsByCategoryUseCase(transactionRepository),
                new ListTransactionsByPeriodUseCase(transactionRepository, clock),
                new SummarizeExpensesUseCase(transactionRepository, clock),
                new DateTimeTools(clock));

        tools = Arrays.stream(callbacks)
                .collect(Collectors.toMap(callback -> callback.getToolDefinition().name(), Function.identity()));
    }

    @Test
    void should_exposeAllTools() {
        assertThat(tools).containsOnlyKeys(
                "persist-transaction",
                "list-transactions-by-category",
                "list-transactions-by-period",
                "summarize-expenses",
                "current-date");
    }

    @Test
    void should_persistTransactionWithToday_when_modelDoesNotSendDate() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = tools.get("persist-transaction").call("""
                {"input": {"description": "Compra no mercado", "amount": 8000, "category": "GROCERIES"}}
                """);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(TODAY);
        assertThat(captor.getValue().getAmount()).isEqualTo(8000);
        assertThat(result).contains("Compra no mercado").contains("80.00");
    }

    @Test
    void should_convertDatesAndCategory_when_listingByPeriod() {
        var october = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        when(transactionRepository.findAllByCategoryAndDateRange(Category.AUTO, october)).thenReturn(List.of(
                new Transaction("Combustível", 20000, Category.AUTO, LocalDate.of(2026, 10, 10))));

        var result = tools.get("list-transactions-by-period").call("""
                {"from": "2026-10-01", "to": "2026-10-31", "category": "AUTO"}
                """);

        assertThat(result).contains("Combustível").contains("200.00");
    }

    @Test
    void should_summarizeCurrentMonth_when_modelSendsNoDates() {
        var currentMonth = new DateRange(LocalDate.of(2026, 10, 1), TODAY);
        when(transactionRepository.findAllByDateRange(currentMonth)).thenReturn(List.of(
                new Transaction("Pizza", 6500, Category.RESTAURANT, LocalDate.of(2026, 10, 24))));

        var result = tools.get("summarize-expenses").call("{}");

        assertThat(result).contains("RESTAURANT").contains("65.00");
    }

    @Test
    void should_informTodayWithDayOfWeek() {
        var result = tools.get("current-date").call("{}");

        assertThat(result).contains("2026-10-26").contains("segunda-feira");
    }

    @Test
    void should_markDateAsOptional_inPersistToolSchema() {
        var schema = tools.get("persist-transaction").getToolDefinition().inputSchema();

        assertThat(schema).contains("\"date\"").contains("centavos");
    }
}
