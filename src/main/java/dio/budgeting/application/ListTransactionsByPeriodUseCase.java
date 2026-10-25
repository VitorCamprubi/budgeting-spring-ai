package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class ListTransactionsByPeriodUseCase {
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public ListTransactionsByPeriodUseCase(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Tool(name = "list-transactions-by-period",
            description = "Lista os gastos de um período, do mais antigo para o mais recente. Pode filtrar por categoria.")
    public List<TransactionOutput> execute(
            @ToolParam(description = "Data inicial no formato yyyy-MM-dd. Se vazia, usa o primeiro dia do mês da data final",
                    required = false) LocalDate from,
            @ToolParam(description = "Data final no formato yyyy-MM-dd. Se vazia, usa a data de hoje",
                    required = false) LocalDate to,
            @ToolParam(description = "Categoria para filtrar (opcional): GROCERIES, PHARMA, AUTO, RESTAURANT, LEISURE ou OTHER",
                    required = false) Category category) {
        var range = DateRange.resolve(from, to, LocalDate.now(clock));

        var transactions = category == null
                ? transactionRepository.findAllByDateRange(range)
                : transactionRepository.findAllByCategoryAndDateRange(category, range);

        return transactions.stream().map(TransactionOutput::from).toList();
    }
}
