package pt.bikeshop.encomendas.exception;

/** Um serviço de que a encomenda depende não respondeu ou tem o circuito aberto: 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
