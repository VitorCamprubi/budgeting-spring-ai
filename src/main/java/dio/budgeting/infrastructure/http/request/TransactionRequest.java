package dio.budgeting.infrastructure.http.request;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,

        @NotNull(message = "A categoria é obrigatória")
        Category category,

        @Positive(message = "O valor deve ser maior que zero (informado em centavos)")
        long amount,

        // Opcional: quando não informada, o caso de uso considera a data de hoje
        @PastOrPresent(message = "A data do gasto não pode estar no futuro")
        LocalDate date) {

    public PersistTransactionInput toInput() {
        return new PersistTransactionInput(description, amount, category, date);
    }
}
