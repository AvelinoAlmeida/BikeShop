package pt.bikeshop.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/** Unidades presas para uma encomenda até ela ser paga (CONFIRMADA) ou cancelada (LIBERTADA). */
@Entity
@Table(name = "reservas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    public enum Estado { ATIVA, CONFIRMADA, LIBERTADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID encomendaId;

    @Column(nullable = false, length = 20)
    private String referencia;

    @Column(nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadaEm;
}
