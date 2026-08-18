package nc.opt.totp;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduit les erreurs metier en reponses JSON stables pour le front. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidConfigException.class)
    public ResponseEntity<Map<String, String>> onInvalidConfig(InvalidConfigException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.code(), "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> onIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }
}
