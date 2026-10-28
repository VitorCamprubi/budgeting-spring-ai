package dio.budgeting.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 1000, message = "A mensagem deve ter no máximo 1000 caracteres")
        String message) {
}
