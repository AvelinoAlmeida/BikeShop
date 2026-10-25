package pt.bikeshop.envios.service;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import pt.bikeshop.envios.configuration.MQConfig;
import pt.bikeshop.envios.entity.Envio;
import pt.bikeshop.envios.messages.CriarEnvioMensagem;
import pt.bikeshop.envios.messages.EnvioExpedidoMensagem;
import pt.bikeshop.envios.publishers.EnviosPublisher;
import pt.bikeshop.envios.repository.EnvioRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnviosServiceTest {

    private final EnvioRepository envios = mock(EnvioRepository.class);
    private final EnviosPublisher publisher = mock(EnviosPublisher.class);
    private final EnviosService servico = new EnviosService(envios, publisher, new ModelMapper());
    private final UUID encomenda = UUID.randomUUID();

    @Test
    void criaOEnvioEmPreparacaoComCodigoDeSeguimento() {
        when(envios.findByEncomendaId(encomenda)).thenReturn(Optional.empty());
        when(envios.save(any())).thenAnswer(i -> i.getArgument(0));

        servico.criar(new CriarEnvioMensagem(encomenda, "Rita Moreira", "rita@bikeshop.pt", "Rua das Flores 10, 4050-262 Porto", 2));

        verify(envios).save(argThat(e -> e.getEstado() == Envio.Estado.EM_PREPARACAO
                && e.getCodigo().matches("BS-[0-9A-F]{8}") && e.getArtigos() == 2));
    }

    @Test
    void mensagemRepetidaNaoCriaOutroEnvio() {
        when(envios.findByEncomendaId(encomenda)).thenReturn(Optional.of(new Envio()));

        servico.criar(new CriarEnvioMensagem(encomenda, "Rita", "rita@bikeshop.pt", null, 1));

        verify(envios, never()).save(any());
    }

    @Test
    void expedirPublicaEnvioExpedido() {
        Envio envio = Envio.builder().encomendaId(encomenda).codigo("BS-1A2B3C4D").estado(Envio.Estado.EM_PREPARACAO).build();
        when(envios.findByEncomendaId(encomenda)).thenReturn(Optional.of(envio));

        assertThat(servico.expedir(encomenda).getEstado()).isEqualTo("EXPEDIDO");
        verify(publisher).publicar(MQConfig.EXPEDIDO, new EnvioExpedidoMensagem(encomenda, "BS-1A2B3C4D"));
    }

    @Test
    void naoSeExpedeDuasVezes() {
        Envio expedido = Envio.builder().encomendaId(encomenda).codigo("BS-1A2B3C4D").estado(Envio.Estado.EXPEDIDO).build();
        when(envios.findByEncomendaId(encomenda)).thenReturn(Optional.of(expedido));

        assertThatThrownBy(() -> servico.expedir(encomenda)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(publisher);
    }
}
