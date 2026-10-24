package pt.bikeshop.stock.entity;

import jakarta.persistence.*;
import lombok.*;

/** Unidades de uma peça no armazém. Livre = quantidade - reservada. */
@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String referencia;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private int reservada;

    public int getLivre() {
        return quantidade - reservada;
    }
}
