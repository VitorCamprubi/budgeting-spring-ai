package dio.budgeting.domain;

import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAllByCategory(Category category);

    List<Transaction> findAllByDateRange(DateRange range);

    List<Transaction> findAllByCategoryAndDateRange(Category category, DateRange range);
}
