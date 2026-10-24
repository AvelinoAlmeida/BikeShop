package pt.bikeshop.encomendas.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.Objects;

/**
 * Erros em JSON com o nome do serviço, para se saber onde falhou (slide 04). O detalhe técnico de um erro
 * inesperado fica só no log; o cliente recebe uma mensagem genérica (slides 03 e 04).
 */
@Slf4j
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

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> pedidoMalFormado(Exception e) {
        return erro(HttpStatus.BAD_REQUEST, "Pedido mal formado: confirme o JSON e os identificadores.");
    }

    /** Erros que o Spring já classifica (caminho inexistente, método errado…) mantêm o seu código HTTP. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        if (e instanceof ErrorResponse resposta) {
            return erro(resposta.getStatusCode(), resposta.getBody().getDetail());
        }
        log.error("Erro inesperado", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatusCode estado, String mensagem) {
        return ResponseEntity.status(estado).body(Map.of("servico", servico, "estado", estado.value(),
                "mensagem", Objects.requireNonNullElse(mensagem, "")));
    }
}
