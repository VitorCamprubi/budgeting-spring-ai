package dio.budgeting.application.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryOutput(LocalDate from,
                                   LocalDate to,
                                   BigDecimal total,
                                   int transactionCount,
                                   List<CategoryTotal> byCategory) {

    public record CategoryTotal(String category, BigDecimal total, int transactionCount) {
    }
}
