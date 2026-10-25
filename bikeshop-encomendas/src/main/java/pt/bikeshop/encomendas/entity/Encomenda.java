package pt.bikeshop.encomendas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Raiz do agregado (Aula 03): as linhas só entram por adicionarLinha e o estado só muda pelos métodos da SAGA.
 * Cada método devolve false quando a mudança já não se aplica (mensagem repetida ou fora de ordem).
 */
@Entity
@Table(name = "encomendas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class Encomenda {

    public enum Estado { PENDENTE, RESERVADA, PAGA, ENVIADA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false, length = 80)
    private String clienteNome;

    @Setter
    @Column(nullable = false, length = 120)
    private String clienteEmail;

    /** Id do cliente no bikeshop-clientes (só a identidade; os dados ficam lá). */
    @Setter
    private Long clienteId;

    /** Morada de entrega copiada da ficha do cliente quando a encomenda foi feita. */
    @Setter
    @Column(length = 200)
    private String moradaEntrega;

    /** Código de seguimento dado pelo bikeshop-envios quando a encomenda sai do armazém. */
    @Column(length = 12)
    private String codigoEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Estado estado = Estado.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Porque foi cancelada (stock insuficiente ou pagamento recusado). */
    @Column(length = 200)
    private String motivo;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LinhaEncomenda> linhas = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime alteradoEm;

    public Encomenda(String clienteNome, String clienteEmail) {
        this.clienteNome = clienteNome;
        this.clienteEmail = clienteEmail;
    }

    public void adicionarLinha(String referencia, String nome, BigDecimal precoUnitario, int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("Quantidade inválida para " + referencia);
        }
        linhas.add(new LinhaEncomenda(this, referencia, nome, precoUnitario, quantidade));
        total = linhas.stream().map(LinhaEncomenda::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** O stock reservou tudo: segue para o pagamento. */
    public boolean marcarReservada() {
        if (estado != Estado.PENDENTE) {
            return false;
        }
        estado = Estado.RESERVADA;
        return true;
    }

    /** O pagamento foi aprovado. */
    public boolean marcarPaga() {
        if (estado != Estado.RESERVADA) {
            return false;
        }
        estado = Estado.PAGA;
        return true;
    }

    /** O envio saiu do armazém. */
    public boolean marcarEnviada(String codigoEnvio) {
        if (estado != Estado.PAGA) {
            return false;
        }
        estado = Estado.ENVIADA;
        this.codigoEnvio = codigoEnvio;
        return true;
    }

    /** Stock insuficiente ou pagamento recusado. */
    public boolean cancelar(String motivo) {
        if (estado == Estado.PAGA || estado == Estado.ENVIADA || estado == Estado.CANCELADA) {
            return false;
        }
        estado = Estado.CANCELADA;
        this.motivo = motivo;
        return true;
    }
}
