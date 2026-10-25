package pt.bikeshop.envios.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/** Um envio por encomenda paga. Começa EM_PREPARACAO e passa a EXPEDIDO quando sai do armazém. */
@Entity
@Table(name = "envios")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Envio {

    public enum Estado { EM_PREPARACAO, EXPEDIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID encomendaId;

    /** Código de seguimento dado à transportadora, por exemplo BS-1A2B3C4D. */
    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String clienteNome;

    @Column(nullable = false, length = 120)
    private String clienteEmail;

    /** Morada de entrega que vem da ficha do cliente; vazia se o cliente ainda não a indicou. */
    @Column(length = 200)
    private String morada;

    @Column(nullable = false)
    private int artigos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Estado estado;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime alteradoEm;
}
