package pt.bikeshop.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

/** Família de peças: baterias, motores, travões... O código (BAT, MOT) é o que a interface usa. */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(length = 200)
    private String descricao;
}
