package pt.bikeshop.encomendas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Uma peça da encomenda, com o nome e o preço copiados do catálogo no momento da compra. */
@Entity
@Table(name = "linhas_encomenda")
@Getter
@NoArgsConstructor
public class LinhaEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "encomenda_id")
    private Encomenda encomenda;

    @Column(nullable = false, length = 20)
    private String referencia;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    LinhaEncomenda(Encomenda encomenda, String referencia, String nome, BigDecimal precoUnitario, int quantidade) {
        this.encomenda = encomenda;
        this.referencia = referencia;
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
