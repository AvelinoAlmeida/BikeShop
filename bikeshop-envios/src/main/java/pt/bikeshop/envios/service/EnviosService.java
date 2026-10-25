package pt.bikeshop.envios.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.envios.configuration.MQConfig;
import pt.bikeshop.envios.dto.EnvioDto;
import pt.bikeshop.envios.entity.Envio;
import pt.bikeshop.envios.exception.NaoEncontradoException;
import pt.bikeshop.envios.messages.CriarEnvioMensagem;
import pt.bikeshop.envios.messages.EnvioExpedidoMensagem;
import pt.bikeshop.envios.publishers.EnviosPublisher;
import pt.bikeshop.envios.repository.EnvioRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnviosService {

    private final EnvioRepository envios;
    private final EnviosPublisher publisher;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    public List<EnvioDto> listar() {
        return envios.findAllByOrderByCriadoEmDesc().stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public EnvioDto daEncomenda(UUID encomendaId) {
        return paraDto(procurar(encomendaId));
    }

    /** Prepara o envio de uma encomenda paga. Uma mensagem repetida não cria um segundo envio. */
    public void criar(CriarEnvioMensagem pedido) {
        if (envios.findByEncomendaId(pedido.encomendaId()).isPresent()) {
            log.info("Encomenda {} já tem envio: mensagem repetida", pedido.encomendaId());
            return;
        }
        Envio envio = envios.save(Envio.builder().encomendaId(pedido.encomendaId()).codigo(codigoDeSeguimento())
                .clienteNome(pedido.clienteNome()).clienteEmail(pedido.clienteEmail()).morada(pedido.morada())
                .artigos(pedido.artigos()).estado(Envio.Estado.EM_PREPARACAO).build());
        log.info("Encomenda {}: envio {} em preparação ({} artigos)", pedido.encomendaId(), envio.getCodigo(), pedido.artigos());
    }

    /** Simula a saída do armazém: o envio fica EXPEDIDO e a encomenda passa a ENVIADA. */
    public EnvioDto expedir(UUID encomendaId) {
        Envio envio = procurar(encomendaId);
        if (envio.getEstado() != Envio.Estado.EM_PREPARACAO) {
            throw new IllegalArgumentException("O envio já está " + envio.getEstado());
        }
        envio.setEstado(Envio.Estado.EXPEDIDO);
        publisher.publicar(MQConfig.EXPEDIDO, new EnvioExpedidoMensagem(encomendaId, envio.getCodigo()));
        log.info("Encomenda {}: envio {} expedido", encomendaId, envio.getCodigo());
        return paraDto(envio);
    }

    private static String codigoDeSeguimento() {
        return "BS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private Envio procurar(UUID encomendaId) {
        return envios.findByEncomendaId(encomendaId)
                .orElseThrow(() -> new NaoEncontradoException("Sem envio para a encomenda " + encomendaId));
    }

    private EnvioDto paraDto(Envio envio) {
        return mapper.map(envio, EnvioDto.class);
    }
}
