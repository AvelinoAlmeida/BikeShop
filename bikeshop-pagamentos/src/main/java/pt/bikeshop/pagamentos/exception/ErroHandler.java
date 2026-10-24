package pt.bikeshop.pagamentos.exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Erros em JSON com o nome do serviço, para se saber onde falhou (slide 04). */
@RestControllerAdvice
public class ErroHandler {

    @Value("${spring.application.name}")
    private String servico;

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(NaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> pedidoInvalido(Exception e) {
        String mensagem = e instanceof MethodArgumentNotValidException m && m.getFieldError() != null
                ? m.getFieldError().getField() + ": " + m.getFieldError().getDefaultMessage()
                : e.getMessage();
        return erro(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus estado, String mensagem) {
        return ResponseEntity.status(estado).body(Map.of("servico", servico, "estado", estado.value(), "mensagem", mensagem));
    }
}
