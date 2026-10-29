package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Orquestra o fluxo de IA: áudio -> texto -> modelo de linguagem com tools -> áudio.
 * <p>
 * Fica na infraestrutura porque depende do Spring AI. As regras de negócio continuam
 * nos casos de uso, que o modelo chama através do tool calling.
 */
@Component
public class BudgetAssistant {
    private static final int RECENT_DAYS = 8;

    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;
    private final ChatClient chatClient;
    private final DateTimeTools dateTimeTools;

    public BudgetAssistant(TranscriptionModel transcriptionModel,
                           TextToSpeechModel textToSpeechModel,
                           ChatClient.Builder chatClientBuilder,
                           @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                           PersistTransactionUseCase persistTransactionUseCase,
                           ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                           ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase,
                           SummarizeExpensesUseCase summarizeExpensesUseCase,
                           DateTimeTools dateTimeTools) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.dateTimeTools = dateTimeTools;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(
                        persistTransactionUseCase,
                        listTransactionsByCategoryUseCase,
                        listTransactionsByPeriodUseCase,
                        summarizeExpensesUseCase,
                        dateTimeTools)
                .build();
    }

    public String chat(String message) {
        // A data e os últimos dias vão no prompt de sistema a cada chamada: nos testes reais, o modelo nem sempre
        // chamava a tool de data e errava contas de calendário ("ontem" virava hoje, "sábado" virava quinta).
        return chatClient.prompt()
                .system(system -> system
                        .param("today", dateTimeTools.currentDate())
                        .param("recentDays", dateTimeTools.recentDays(RECENT_DAYS)))
                .user(message)
                .call()
                .content();
    }

    public byte[] talk(Resource audio) {
        var message = transcriptionModel.transcribe(audio);
        var reply = chat(message);
        return textToSpeechModel.call(reply);
    }
}
