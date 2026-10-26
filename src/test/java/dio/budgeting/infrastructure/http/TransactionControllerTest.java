package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidDateRangeException;
import dio.budgeting.domain.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 25);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PersistTransactionUseCase persistTransactionUseCase;

    @MockitoBean
    ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @MockitoBean
    ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;

    @MockitoBean
    SummarizeExpensesUseCase summarizeExpensesUseCase;

    @Test
    void should_listTransactionsByPeriodAndCategory() throws Exception {
        var from = LocalDate.of(2026, 10, 1);
        var to = LocalDate.of(2026, 10, 31);
        when(listTransactionsByPeriodUseCase.execute(from, to, Category.AUTO)).thenReturn(List.of(
                new TransactionOutput("c3", "Combustível", "AUTO", new BigDecimal("200.00"), DATE)));

        mockMvc.perform(get("/transactions")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("category", "AUTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Combustível"))
                .andExpect(jsonPath("$[0].date").value("2026-10-25"));
    }

    @Test
    void should_return400_when_periodIsInvalid() throws Exception {
        when(listTransactionsByPeriodUseCase.execute(any(), any(), any()))
                .thenThrow(new InvalidDateRangeException("A data inicial não pode ser posterior à data final"));

        mockMvc.perform(get("/transactions").param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Período inválido"));
    }

    @Test
    void should_return400_when_dateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/transactions").param("from", "31/10/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));

        verifyNoInteractions(listTransactionsByPeriodUseCase);
    }

    @Test
    void should_returnExpenseSummary() throws Exception {
        when(summarizeExpensesUseCase.execute(null, null)).thenReturn(new ExpenseSummaryOutput(
                LocalDate.of(2026, 10, 1), DATE, new BigDecimal("320.50"), 3,
                List.of(new CategoryTotal("AUTO", new BigDecimal("200.00"), 1),
                        new CategoryTotal("GROCERIES", new BigDecimal("120.50"), 2))));

        mockMvc.perform(get("/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(320.5))
                .andExpect(jsonPath("$.transactionCount").value(3))
                .andExpect(jsonPath("$.byCategory[0].category").value("AUTO"))
                .andExpect(jsonPath("$.byCategory[1].transactionCount").value(2));
    }

    @Test
    void should_return201_when_transactionIsValid() throws Exception {
        when(persistTransactionUseCase.execute(any())).thenReturn(
                new TransactionOutput("b7c1", "Compra no mercado", "GROCERIES", new BigDecimal("80.00"), DATE));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Compra no mercado", "category": "GROCERIES", "amount": 8000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("b7c1"))
                .andExpect(jsonPath("$.amount").value(80.0))
                .andExpect(jsonPath("$.date").value("2026-10-25"));
    }

    @Test
    void should_return400WithFieldErrors_when_requestIsInvalid() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": " ", "amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.description").value("A descrição é obrigatória"))
                .andExpect(jsonPath("$.errors.category").value("A categoria é obrigatória"))
                .andExpect(jsonPath("$.errors.amount").exists());

        verifyNoInteractions(persistTransactionUseCase);
    }

    @Test
    void should_return400_when_dateIsInTheFuture() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Viagem", "category": "LEISURE", "amount": 50000, "date": "2999-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.date").value("A data do gasto não pode estar no futuro"));

        verifyNoInteractions(persistTransactionUseCase);
    }

    @Test
    void should_return400_when_bodyHasUnknownCategory() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Cinema", "category": "MOVIES", "amount": 3000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    void should_return422_when_domainRuleIsViolated() throws Exception {
        when(persistTransactionUseCase.execute(any()))
                .thenThrow(new InvalidTransactionException("O valor da transação deve ser maior que zero"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Compra no mercado", "category": "GROCERIES", "amount": 8000}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Transação inválida"))
                .andExpect(jsonPath("$.detail").value("O valor da transação deve ser maior que zero"));
    }

    @Test
    void should_listTransactions_when_categoryIsValid() throws Exception {
        when(listTransactionsByCategoryUseCase.execute(Category.PHARMA)).thenReturn(List.of(
                new TransactionOutput("a1", "Remédio", "PHARMA", new BigDecimal("45.90"), DATE)));

        mockMvc.perform(get("/transactions/PHARMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Remédio"))
                .andExpect(jsonPath("$[0].amount").value(45.9));
    }

    @Test
    void should_return400WithAcceptedValues_when_categoryIsUnknown() throws Exception {
        mockMvc.perform(get("/transactions/MOVIES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail", containsString("GROCERIES")));
    }
}
