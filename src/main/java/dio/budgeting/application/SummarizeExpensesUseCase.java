package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SummarizeExpensesUseCase {
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public SummarizeExpensesUseCase(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Tool(name = "summarize-expenses",
            description = "Resume os gastos de um período: total gasto, quantidade de transações e total por categoria, "
                    + "da categoria com mais gastos para a com menos. Use para perguntas como 'quanto gastei esse mês?'")
    public ExpenseSummaryOutput execute(
            @ToolParam(description = "Data inicial no formato yyyy-MM-dd. Se vazia, usa o primeiro dia do mês da data final",
                    required = false) LocalDate from,
            @ToolParam(description = "Data final no formato yyyy-MM-dd. Se vazia, usa a data de hoje",
                    required = false) LocalDate to) {
        var range = DateRange.resolve(from, to, LocalDate.now(clock));
        var transactions = transactionRepository.findAllByDateRange(range);

        Map<Category, List<Transaction>> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory));

        // As somas são feitas em centavos (long) e só convertidas para reais no final
        var categoryTotals = byCategory.entrySet().stream()
                .map(entry -> new CategoryTotal(
                        entry.getKey().name(),
                        toReais(sumOf(entry.getValue())),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(CategoryTotal::total).reversed())
                .toList();

        return new ExpenseSummaryOutput(
                range.from(),
                range.to(),
                toReais(sumOf(transactions)),
                transactions.size(),
                categoryTotals);
    }

    private static long sumOf(List<Transaction> transactions) {
        return transactions.stream().mapToLong(Transaction::getAmount).sum();
    }

    private static BigDecimal toReais(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
