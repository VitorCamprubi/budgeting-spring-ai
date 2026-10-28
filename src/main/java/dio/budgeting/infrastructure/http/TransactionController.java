package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.ExpenseSummaryResponse;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transações", description = "Registro e consulta de gastos")
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;
    private final SummarizeExpensesUseCase summarizeExpensesUseCase;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase,
                                 SummarizeExpensesUseCase summarizeExpensesUseCase) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.listTransactionsByPeriodUseCase = listTransactionsByPeriodUseCase;
        this.summarizeExpensesUseCase = summarizeExpensesUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um gasto",
            description = "O valor (amount) é informado em centavos. Se a data não for enviada, é usada a data de hoje.")
    @ApiResponse(responseCode = "201", description = "Gasto registrado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    public TransactionResponse createTransaction(@RequestBody @Valid TransactionRequest request) {
        var transaction = persistTransactionUseCase.execute(request.toInput());
        return TransactionResponse.from(transaction);
    }

    @GetMapping
    @Operation(summary = "Lista os gastos de um período",
            description = "Sem datas, considera do dia 1º do mês atual até hoje. A categoria é um filtro opcional.")
    public List<TransactionResponse> listTransactions(
            @Parameter(description = "Data inicial (yyyy-MM-dd)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final (yyyy-MM-dd)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filtra por categoria")
            @RequestParam(required = false) Category category) {
        return listTransactionsByPeriodUseCase.execute(from, to, category).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/summary")
    @Operation(summary = "Resume os gastos de um período",
            description = "Total gasto, quantidade de transações e total por categoria (da maior para a menor).")
    public ExpenseSummaryResponse summarizeExpenses(
            @Parameter(description = "Data inicial (yyyy-MM-dd)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final (yyyy-MM-dd)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ExpenseSummaryResponse.from(summarizeExpensesUseCase.execute(from, to));
    }

    @GetMapping("/{category}")
    @Operation(summary = "Lista todos os gastos de uma categoria")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream().map(TransactionResponse::from).toList();
    }
}
