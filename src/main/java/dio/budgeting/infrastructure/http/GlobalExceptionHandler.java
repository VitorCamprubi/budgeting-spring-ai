package dio.budgeting.infrastructure.http;

import com.openai.errors.OpenAIException;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidDateRangeException;
import dio.budgeting.domain.InvalidTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas no formato Problem Details (RFC 9457),
 * para que o cliente sempre receba um erro claro em vez de um 500 genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ACCEPTED_CATEGORIES = Arrays.toString(Category.values());

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problem.setTitle("Dados inválidos");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Não foi possível ler o corpo da requisição. Verifique o JSON enviado e se a categoria é uma de "
                        + ACCEPTED_CATEGORIES);
        problem.setTitle("Corpo da requisição inválido");

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        var detail = "Valor inválido: " + ex.getValue();
        if (ex.getRequiredType() == Category.class) {
            detail += ". Categorias aceitas: " + ACCEPTED_CATEGORIES;
        }

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Parâmetro inválido");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ProblemDetail handleInvalidTransaction(InvalidTransactionException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Transação inválida");
        return problem;
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Período inválido");
        return problem;
    }

    @ExceptionHandler(OpenAIException.class)
    public ProblemDetail handleAiProviderFailure(OpenAIException ex) {
        LOGGER.warn("Falha ao chamar o provedor de IA: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Não foi possível falar com o serviço de IA agora. Verifique a chave OPENAI_API_KEY e tente novamente.");
        problem.setTitle("Serviço de IA indisponível");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // O Spring AI pode embrulhar a exceção do SDK da OpenAI em outra; nesse caso continua sendo falha do provedor
        var aiFailure = findCause(ex, OpenAIException.class);
        if (aiFailure != null) {
            return handleAiProviderFailure(aiFailure);
        }

        LOGGER.error("Erro inesperado ao processar a requisição", ex);

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problem.setTitle("Erro interno");
        return problem;
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }
}
