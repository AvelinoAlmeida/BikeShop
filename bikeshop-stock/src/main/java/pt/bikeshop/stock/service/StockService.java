package pt.bikeshop.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.stock.configuration.MQConfig;
import pt.bikeshop.stock.dto.StockDto;
import pt.bikeshop.stock.entity.Reserva;
import pt.bikeshop.stock.entity.Stock;
import pt.bikeshop.stock.exception.NaoEncontradoException;
import pt.bikeshop.stock.messages.ReservarStockMensagem;
import pt.bikeshop.stock.messages.RespostaStockMensagem;
import pt.bikeshop.stock.publishers.StockPublisher;
import pt.bikeshop.stock.repository.ReservaRepository;
import pt.bikeshop.stock.repository.StockRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final StockRepository stocks;
    private final ReservaRepository reservas;
    private final StockPublisher publisher;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    public List<StockDto> listar() {
        return stocks.findAllByOrderByReferencia().stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public StockDto obter(String referencia) {
        return paraDto(procurar(referencia));
    }

    @Transactional(readOnly = true)
    public List<Reserva> reservasDaEncomenda(UUID encomendaId) {
        return reservas.findByEncomendaId(encomendaId);
    }

    public StockDto alterarQuantidade(String referencia, int quantidade) {
        Stock stock = procurar(referencia);
        if (quantidade < stock.getReservada()) {
            throw new IllegalArgumentException("A quantidade não pode ficar abaixo do reservado (" + stock.getReservada() + ")");
        }
        stock.setQuantidade(quantidade);
        return paraDto(stock);
    }

    /**
     * Reserva todas as linhas ou nenhuma. Se a encomenda já foi tratada (mensagem repetida), responde o mesmo
     * sem reservar outra vez.
     */
    public void reservar(ReservarStockMensagem pedido) {
        UUID id = pedido.encomendaId();
        if (!reservas.findByEncomendaId(id).isEmpty()) {
            log.info("Encomenda {} já tinha reservas: mensagem repetida", id);
            publisher.publicar(MQConfig.RESERVADO, new RespostaStockMensagem(id, null));
            return;
        }
        for (ReservarStockMensagem.Linha linha : pedido.linhas()) {
            Stock stock = stocks.findByReferencia(linha.referencia()).orElse(null);
            if (stock == null || stock.getLivre() < linha.quantidade()) {
                int livre = stock == null ? 0 : stock.getLivre();
                String motivo = "Stock insuficiente para " + linha.referencia() + ": pedidas " + linha.quantidade() + ", livres " + livre;
                log.info("Encomenda {}: {}", id, motivo);
                publisher.publicar(MQConfig.INSUFICIENTE, new RespostaStockMensagem(id, motivo));
                return;
            }
        }
        for (ReservarStockMensagem.Linha linha : pedido.linhas()) {
            Stock stock = procurar(linha.referencia());
            stock.setReservada(stock.getReservada() + linha.quantidade());
            reservas.save(Reserva.builder().encomendaId(id).referencia(linha.referencia())
                    .quantidade(linha.quantidade()).estado(Reserva.Estado.ATIVA).build());
        }
        log.info("Encomenda {}: stock reservado ({} linhas)", id, pedido.linhas().size());
        publisher.publicar(MQConfig.RESERVADO, new RespostaStockMensagem(id, null));
    }

    /** Encomenda paga: as unidades saem do armazém. */
    public void confirmar(UUID encomendaId) {
        for (Reserva reserva : reservas.findByEncomendaIdAndEstado(encomendaId, Reserva.Estado.ATIVA)) {
            Stock stock = procurar(reserva.getReferencia());
            stock.setQuantidade(stock.getQuantidade() - reserva.getQuantidade());
            stock.setReservada(stock.getReservada() - reserva.getQuantidade());
            reserva.setEstado(Reserva.Estado.CONFIRMADA);
        }
        log.info("Encomenda {}: reservas confirmadas", encomendaId);
    }

    /** Compensação da SAGA: o pagamento falhou e as unidades voltam a ficar livres. */
    public void libertar(UUID encomendaId) {
        for (Reserva reserva : reservas.findByEncomendaIdAndEstado(encomendaId, Reserva.Estado.ATIVA)) {
            Stock stock = procurar(reserva.getReferencia());
            stock.setReservada(stock.getReservada() - reserva.getQuantidade());
            reserva.setEstado(Reserva.Estado.LIBERTADA);
        }
        log.info("Encomenda {}: reservas libertadas", encomendaId);
    }

    private Stock procurar(String referencia) {
        return stocks.findByReferencia(referencia)
                .orElseThrow(() -> new NaoEncontradoException("Sem stock para a peça " + referencia));
    }

    private StockDto paraDto(Stock stock) {
        return mapper.map(stock, StockDto.class);
    }
}
