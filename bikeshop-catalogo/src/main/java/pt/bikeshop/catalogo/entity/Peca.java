package pt.bikeshop.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Peça à venda. A referência (BAT-500) identifica a peça em todos os serviços. */
@Entity
@Table(name = "pecas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String referencia;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 40)
    private String marca;

    @Column(length = 300)
    private String descricao;

    /** Sistema com que a peça é compatível, por exemplo "Bosch Smart System". */
    @Column(length = 60)
    private String compatibilidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime alteradoEm;
}
