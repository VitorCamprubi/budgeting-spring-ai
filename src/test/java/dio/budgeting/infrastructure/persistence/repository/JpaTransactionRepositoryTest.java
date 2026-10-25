package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.TestcontainersConfiguration;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.DateRange;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o adaptador JPA contra um MySQL real (Testcontainers), garantindo que as consultas
 * derivadas do Spring Data funcionam no banco usado em produção.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaTransactionRepository.class})
class JpaTransactionRepositoryTest {
    private static final DateRange OCTOBER = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

    @Autowired
    JpaTransactionRepository repository;

    @Test
    void should_keepAllFields_when_savingAndReading() {
        var saved = repository.save(new Transaction("Compra no mercado", 8050, Category.GROCERIES, LocalDate.of(2026, 10, 3)));

        var found = repository.findAllByCategory(Category.GROCERIES);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getId()).isEqualTo(saved.getId());
        assertThat(found.getFirst().getAmount()).isEqualTo(8050);
        assertThat(found.getFirst().getDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void should_returnOnlyTransactionsInsideRange_orderedByDate() {
        repository.save(new Transaction("Pizza", 6500, Category.RESTAURANT, LocalDate.of(2026, 10, 24)));
        repository.save(new Transaction("Farmácia de setembro", 3000, Category.PHARMA, LocalDate.of(2026, 9, 30)));
        repository.save(new Transaction("Mercado", 8000, Category.GROCERIES, LocalDate.of(2026, 10, 3)));
        repository.save(new Transaction("Feira", 4000, Category.GROCERIES, LocalDate.of(2026, 10, 1)));

        var result = repository.findAllByDateRange(OCTOBER);

        assertThat(result).extracting(Transaction::getDescription)
                .containsExactly("Feira", "Mercado", "Pizza");
    }

    @Test
    void should_filterByCategoryAndRange() {
        repository.save(new Transaction("Combustível", 20000, Category.AUTO, LocalDate.of(2026, 10, 10)));
        repository.save(new Transaction("Estacionamento", 1500, Category.AUTO, LocalDate.of(2026, 11, 2)));
        repository.save(new Transaction("Cinema", 4000, Category.LEISURE, LocalDate.of(2026, 10, 11)));

        var result = repository.findAllByCategoryAndDateRange(Category.AUTO, OCTOBER);

        assertThat(result).extracting(Transaction::getDescription).containsExactly("Combustível");
    }
}
