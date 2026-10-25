package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private final TransactionEntityRepository transactionEntityRepository;

    public JpaTransactionRepository(TransactionEntityRepository transactionEntityRepository) {
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        var entity = TransactionEntity.from(transaction);
        return transactionEntityRepository.save(entity).toDomain();
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return toDomain(transactionEntityRepository.findAllByCategory(category));
    }

    @Override
    public List<Transaction> findAllByDateRange(DateRange range) {
        return toDomain(transactionEntityRepository.findAllByDateBetweenOrderByDateAsc(range.from(), range.to()));
    }

    @Override
    public List<Transaction> findAllByCategoryAndDateRange(Category category, DateRange range) {
        return toDomain(transactionEntityRepository.findAllByCategoryAndDateBetweenOrderByDateAsc(
                category, range.from(), range.to()));
    }

    private static List<Transaction> toDomain(List<TransactionEntity> entities) {
        return entities.stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }
}
