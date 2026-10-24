package dio.budgeting.domain;

import lombok.Getter;

@Getter
public class Transaction {
    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private final TransactionId id;
    private final String description;
    private final long amount;
    private final Category category;

    public Transaction(TransactionId id, String description, long amount, Category category) {
        validate(id, description, amount, category);
        this.id = id;
        this.description = description.strip();
        this.amount = amount;
        this.category = category;
    }

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category);
    }

    // As regras ficam no domínio para valer em qualquer entrada: API REST, tool calling da IA ou testes.
    private static void validate(TransactionId id, String description, long amount, Category category) {
        if (id == null) {
            throw new InvalidTransactionException("O identificador da transação é obrigatório");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("A descrição da transação é obrigatória");
        }
        if (description.strip().length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidTransactionException(
                    "A descrição da transação deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException("O valor da transação deve ser maior que zero");
        }
        if (category == null) {
            throw new InvalidTransactionException("A categoria da transação é obrigatória");
        }
    }
}
