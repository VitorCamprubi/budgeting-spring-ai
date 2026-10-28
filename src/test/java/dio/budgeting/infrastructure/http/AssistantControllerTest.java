package dio.budgeting.infrastructure.http;

import com.openai.errors.OpenAIException;
import dio.budgeting.infrastructure.ai.BudgetAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BudgetAssistant budgetAssistant;

    @Test
    void should_replyWithText_when_chatMessageIsValid() throws Exception {
        when(budgetAssistant.chat("Gastei 80 reais no mercado")).thenReturn("Pronto, registrei 80 reais em mercado.");

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Gastei 80 reais no mercado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Pronto, registrei 80 reais em mercado."));
    }

    @Test
    void should_return400_when_chatMessageIsBlank() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").value("A mensagem é obrigatória"));

        verifyNoInteractions(budgetAssistant);
    }

    @Test
    void should_return502_when_aiProviderFails() throws Exception {
        when(budgetAssistant.chat(any())).thenThrow(new OpenAIException("401 Unauthorized"));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Quanto gastei esse mês?"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Serviço de IA indisponível"));
    }

    @Test
    void should_returnMp3_when_audioIsSent() throws Exception {
        when(budgetAssistant.talk(any())).thenReturn(new byte[]{1, 2, 3});
        var audio = new MockMultipartFile("file", "gasto.m4a", "audio/mp4", new byte[]{9, 9, 9});

        mockMvc.perform(multipart("/assistant/voice").file(audio))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mp3"))
                .andExpect(header().string("Content-Disposition", containsString("audio.mp3")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void should_keepOriginalEndpoint_working() throws Exception {
        when(budgetAssistant.talk(any())).thenReturn(new byte[]{1});
        var audio = new MockMultipartFile("file", "gasto.m4a", "audio/mp4", new byte[]{9});

        mockMvc.perform(multipart("/transactions/ai").file(audio))
                .andExpect(status().isOk());
    }

    @Test
    void should_return400_when_audioIsEmpty() throws Exception {
        var empty = new MockMultipartFile("file", "vazio.m4a", "audio/mp4", new byte[0]);

        mockMvc.perform(multipart("/assistant/voice").file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Áudio inválido"));

        verifyNoInteractions(budgetAssistant);
    }
}
