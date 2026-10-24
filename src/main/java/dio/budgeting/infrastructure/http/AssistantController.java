package dio.budgeting.infrastructure.http;

import dio.budgeting.infrastructure.ai.BudgetAssistant;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AssistantController {
    private final BudgetAssistant budgetAssistant;

    public AssistantController(BudgetAssistant budgetAssistant) {
        this.budgetAssistant = budgetAssistant;
    }

    @PostMapping(value = "/transactions/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mp3")
    public ResponseEntity<Resource> talk(@RequestParam("file") MultipartFile file) {
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
