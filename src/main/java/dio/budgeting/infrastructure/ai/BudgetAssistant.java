package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
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
    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;
    private final ChatClient chatClient;

    public BudgetAssistant(TranscriptionModel transcriptionModel,
                           TextToSpeechModel textToSpeechModel,
                           ChatClient.Builder chatClientBuilder,
                           @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                           PersistTransactionUseCase persistTransactionUseCase,
                           ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt().user(message).call().content();
    }

    public byte[] talk(Resource audio) {
        var message = transcriptionModel.transcribe(audio);
        var reply = chat(message);
        return textToSpeechModel.call(reply);
    }
}
