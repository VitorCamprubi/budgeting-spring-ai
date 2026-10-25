package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PersistTransactionUseCase persistTransactionUseCase;

    @MockitoBean
    ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @Test
    void should_return201_when_transactionIsValid() throws Exception {
        when(persistTransactionUseCase.execute(any())).thenReturn(
                new TransactionOutput("b7c1", "Compra no mercado", "GROCERIES", new BigDecimal("80.00")));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Compra no mercado", "category": "GROCERIES", "amount": 8000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("b7c1"))
                .andExpect(jsonPath("$.amount").value(80.0));
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
                new TransactionOutput("a1", "Remédio", "PHARMA", new BigDecimal("45.90"))));

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
