package dio.budgeting.infrastructure.http;

import dio.budgeting.infrastructure.ai.BudgetAssistant;
import dio.budgeting.infrastructure.http.request.ChatRequest;
import dio.budgeting.infrastructure.http.response.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AssistantController {
    private final BudgetAssistant budgetAssistant;

    public AssistantController(BudgetAssistant budgetAssistant) {
        this.budgetAssistant = budgetAssistant;
    }

    /**
     * Mesmo assistente do endpoint de voz, mas por texto: mais rápido e barato para testar o tool calling.
     */
    @PostMapping("/assistant/chat")
    public ChatResponse chat(@RequestBody @Valid ChatRequest request) {
        return new ChatResponse(budgetAssistant.chat(request.message()));
    }

    /**
     * /transactions/ai é o endpoint original do projeto e continua funcionando.
     */
    @PostMapping(value = {"/assistant/voice", "/transactions/ai"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mp3")
    public ResponseEntity<Resource> talk(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Envie um arquivo de áudio não vazio no campo 'file'");
            problem.setTitle("Áudio inválido");
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
        }

        byte[] audio = budgetAssistant.talk(file.getResource());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.mp3")
                                .build()
                                .toString())
                .body(new ByteArrayResource(audio));
    }
}
