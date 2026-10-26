package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.ExpenseSummaryOutput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(LocalDate from,
                                     LocalDate to,
                                     BigDecimal total,
                                     int transactionCount,
                                     List<CategoryTotalResponse> byCategory) {

    public static ExpenseSummaryResponse from(ExpenseSummaryOutput output) {
        var byCategory = output.byCategory().stream()
                .map(item -> new CategoryTotalResponse(item.category(), item.total(), item.transactionCount()))
                .toList();

        return new ExpenseSummaryResponse(output.from(), output.to(), output.total(), output.transactionCount(), byCategory);
    }

    public record CategoryTotalResponse(String category, BigDecimal total, int transactionCount) {
    }
}
