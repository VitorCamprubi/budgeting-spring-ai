package dio.budgeting.infrastructure.http;

import dio.budgeting.infrastructure.ai.BudgetAssistant;
import dio.budgeting.infrastructure.http.request.ChatRequest;
import dio.budgeting.infrastructure.http.response.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Assistente de IA", description = "Converse com o assistente financeiro por texto ou por voz")
public class AssistantController {
    private final BudgetAssistant budgetAssistant;

    public AssistantController(BudgetAssistant budgetAssistant) {
        this.budgetAssistant = budgetAssistant;
    }

    /**
     * Mesmo assistente do endpoint de voz, mas por texto: mais rápido e barato para testar o tool calling.
     */
    @PostMapping("/assistant/chat")
    @Operation(summary = "Conversa com o assistente por texto",
            description = "Ex.: \"gastei 80 reais no mercado\" ou \"quanto gastei esse mês?\". Requer OPENAI_API_KEY.")
    @ApiResponse(responseCode = "200", description = "Resposta do assistente")
    @ApiResponse(responseCode = "502", description = "Falha ao chamar a OpenAI")
    public ChatResponse chat(@RequestBody @Valid ChatRequest request) {
        return new ChatResponse(budgetAssistant.chat(request.message()));
    }

    /**
     * /transactions/ai é o endpoint original do projeto e continua funcionando.
     */
    @PostMapping(value = {"/assistant/voice", "/transactions/ai"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mp3")
    @Operation(summary = "Conversa com o assistente por voz",
            description = "Recebe um áudio (campo 'file'), transcreve, executa as tools necessárias e responde com um MP3. "
                    + "Requer OPENAI_API_KEY.")
    @ApiResponse(responseCode = "200", description = "Áudio MP3 com a resposta")
    @ApiResponse(responseCode = "400", description = "Arquivo ausente ou vazio")
    @ApiResponse(responseCode = "502", description = "Falha ao chamar a OpenAI")
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
