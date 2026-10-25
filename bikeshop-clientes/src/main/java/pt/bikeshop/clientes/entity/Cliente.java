package pt.bikeshop.clientes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** Cliente da loja. O email identifica-o nos outros serviços; a morada é para onde vão os envios. */
@Entity
@Table(name = "clientes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String morada;

    @Column(length = 10)
    private String codigoPostal;

    @Column(length = 60)
    private String localidade;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime alteradoEm;

    /** Morada numa linha, para o envio; vazia se o cliente ainda não a indicou. */
    public String moradaCompleta() {
        if (morada == null || morada.isBlank()) {
            return null;
        }
        String resto = String.join(" ", codigoPostal == null ? "" : codigoPostal, localidade == null ? "" : localidade).trim();
        return resto.isEmpty() ? morada : morada + ", " + resto;
    }
}
