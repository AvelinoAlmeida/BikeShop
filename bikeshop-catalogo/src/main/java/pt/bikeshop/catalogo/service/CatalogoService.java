package pt.bikeshop.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.catalogo.dto.CategoriaDto;
import pt.bikeshop.catalogo.dto.PecaDto;
import pt.bikeshop.catalogo.dto.PecaPedido;
import pt.bikeshop.catalogo.entity.Categoria;
import pt.bikeshop.catalogo.entity.Peca;
import pt.bikeshop.catalogo.exception.NaoEncontradoException;
import pt.bikeshop.catalogo.repository.CategoriaRepository;
import pt.bikeshop.catalogo.repository.PecaRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final CategoriaRepository categorias;
    private final PecaRepository pecas;
    private final ModelMapper mapper;

    public List<CategoriaDto> listarCategorias() {
        return categorias.findAll().stream().map(c -> mapper.map(c, CategoriaDto.class)).toList();
    }

    public List<PecaDto> listarPecas(String categoria, BigDecimal precoMax) {
        List<Peca> lista = categoria == null
                ? pecas.findAllByOrderByCategoriaCodigoAscPrecoAsc()
                : pecas.procurarPorCategoriaAtePreco(categoria, precoMax == null ? new BigDecimal("999999") : precoMax);
        return lista.stream().map(this::paraDto).toList();
    }

    public PecaDto obterPeca(String referencia) {
        return paraDto(procurar(referencia));
    }

    @Transactional
    public PecaDto criarPeca(PecaPedido pedido) {
        if (pecas.findByReferencia(pedido.getReferencia()).isPresent()) {
            throw new IllegalArgumentException("Já existe a peça " + pedido.getReferencia());
        }
        Peca peca = new Peca();
        preencher(peca, pedido);
        return paraDto(pecas.save(peca));
    }

    @Transactional
    public PecaDto alterarPeca(String referencia, PecaPedido pedido) {
        Peca peca = procurar(referencia);
        preencher(peca, pedido);
        peca.setReferencia(referencia);
        return paraDto(peca);
    }

    @Transactional
    public void apagarPeca(String referencia) {
        pecas.delete(procurar(referencia));
    }

    private void preencher(Peca peca, PecaPedido pedido) {
        Categoria categoria = categorias.findByCodigo(pedido.getCategoriaCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Categoria desconhecida: " + pedido.getCategoriaCodigo()));
        peca.setReferencia(pedido.getReferencia());
        peca.setNome(pedido.getNome());
        peca.setMarca(pedido.getMarca());
        peca.setDescricao(pedido.getDescricao());
        peca.setCompatibilidade(pedido.getCompatibilidade());
        peca.setPreco(pedido.getPreco());
        peca.setCategoria(categoria);
    }

    private Peca procurar(String referencia) {
        return pecas.findByReferencia(referencia)
                .orElseThrow(() -> new NaoEncontradoException("Peça não encontrada: " + referencia));
    }

    private PecaDto paraDto(Peca peca) {
        return mapper.map(peca, PecaDto.class);
    }
}
