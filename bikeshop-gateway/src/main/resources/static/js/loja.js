'use strict';

// Loja: catálogo por categoria com o stock em tempo real, cesto, encomenda e a SAGA passo a passo.
// A encomenda aberta fica no endereço (loja.html#<id>), para se poder voltar a ela.

let categoriaAtual = null;
let stockPorReferencia = {};
let encomendaAberta = null;
let temporizadorSaga = null;

// ---------- catálogo ----------

async function abrirLoja() {
    try {
        const [categorias, stock] = await Promise.all([api('GET', `${API}/categorias`), api('GET', `${API}/stock`)]);
        stockPorReferencia = Object.fromEntries(stock.map((s) => [s.referencia, s]));
        const chip = (codigo, nome, titulo) => el('button', {
            class: 'chip', type: 'button', title: titulo, 'aria-pressed': String(categoriaAtual === codigo),
            onclick: () => { categoriaAtual = codigo; abrirLoja(); },
        }, nome);
        $('categories').replaceChildren(chip(null, 'Tudo'), ...categorias.map((c) => chip(c.codigo, c.nome, c.descricao)));
        const pecas = await api('GET', `${API}/pecas` + (categoriaAtual ? `?categoria=${encodeURIComponent(categoriaAtual)}` : ''));
        $('products').replaceChildren(...(pecas.length ? pecas.map(cartaoPeca) : [el('p', { class: 'muted' }, 'Sem peças nesta categoria.')]));
    } catch (e) {
        $('products').replaceChildren(el('p', { class: 'resp-error' }, 'A loja ainda não responde: ', e.mensagem));
    }
}

function cartaoPeca(p) {
    const s = stockPorReferencia[p.referencia];
    const livre = s ? s.livre : 0;
    const etiqueta = livre === 0 ? ['none', 'Esgotado'] : livre <= 3 ? ['low', `Últimas ${livre}`] : ['ok', `${livre} em stock`];
    return el('article', { class: 'product' },
        el('span', { class: 'cat' }, p.categoriaNome),
        el('span', { class: 'name' }, p.nome),
        el('span', { class: 'brand-line' }, `${p.marca} · ${p.referencia}`),
        p.compatibilidade ? el('span', { class: 'compat' }, p.compatibilidade) : null,
        el('p', { class: 'desc' }, p.descricao),
        el('span', { class: `stock ${etiqueta[0]}` }, etiqueta[1]),
        el('div', { class: 'row' },
            el('span', { class: 'price' }, euros.format(p.preco)),
            el('button', { class: 'primary add', type: 'button', disabled: livre === 0, onclick: () => adicionar(p) }, 'Adicionar')));
}

// ---------- cesto ----------

const cesto = new Map();   // referencia -> { peca, quantidade }

function adicionar(peca) {
    const linha = cesto.get(peca.referencia) || { peca, quantidade: 0 };
    linha.quantidade++;
    cesto.set(peca.referencia, linha);
    desenharCesto();
    showNotice(`${peca.nome} no cesto`);
}

function mudarQuantidade(referencia, delta) {
    const linha = cesto.get(referencia);
    linha.quantidade += delta;
    if (linha.quantidade <= 0) cesto.delete(referencia);
    desenharCesto();
}

function desenharCesto() {
    const linhas = [...cesto.values()];
    $('cart-total').textContent = euros.format(linhas.reduce((t, l) => t + l.peca.preco * l.quantidade, 0));
    $('cart').replaceChildren(...(linhas.length ? linhas.map((l) => el('li', {},
        el('span', {}, el('strong', {}, l.peca.nome), el('br'), el('span', { class: 'muted small' }, euros.format(l.peca.preco))),
        el('span', { class: 'qty' },
            el('button', { type: 'button', onclick: () => mudarQuantidade(l.peca.referencia, -1), 'aria-label': 'Menos' }, '−'),
            el('span', {}, l.quantidade),
            el('button', { type: 'button', onclick: () => mudarQuantidade(l.peca.referencia, 1), 'aria-label': 'Mais' }, '+')),
        el('span', { class: 'num' }, euros.format(l.peca.preco * l.quantidade)))) :
        [el('li', { class: 'muted' }, 'Vazio. Adicione peças do catálogo.')]));
    $('btn-order').disabled = linhas.length === 0;
    $('btn-clear-cart').disabled = linhas.length === 0;
}

async function encomendar() {
    const nome = $('f-nome').value.trim();
    const email = $('f-email').value.trim();
    if (!nome || !email) { showError({ mensagem: 'Indique o nome e o email.' }, 'Encomenda'); return; }
    hideError();
    $('btn-order').disabled = true;
    try {
        const enc = await api('POST', `${API}/encomendas`, {
            clienteNome: nome,
            clienteEmail: email,
            linhas: [...cesto.values()].map((l) => ({ referencia: l.peca.referencia, quantidade: l.quantidade })),
        });
        cesto.clear();
        desenharCesto();
        location.hash = enc.id;
        desenharLista();
    } catch (e) {
        showError(e, 'Encomenda');
        desenharCesto();
    }
}

// ---------- encomenda: a SAGA ----------

function abrirEncomenda(id) {
    clearInterval(temporizadorSaga);
    encomendaAberta = id || null;
    $('order').hidden = !id;
    $('order-empty').hidden = !!id;
    document.querySelectorAll('.history-item').forEach((b) => b.classList.toggle('selected', b.dataset.id === id));
    if (!id) return;
    desenharSaga(id);
    temporizadorSaga = setInterval(() => desenharSaga(id), 1000);
}

async function desenharSaga(id) {
    let enc;
    let pag = null;
    let envio = null;
    try { enc = await api('GET', `${API}/encomendas/${id}`); } catch (e) {
        $('timeline').replaceChildren(el('li', {}, el('span', { class: 'resp-error' }, e.mensagem)));
        return;
    }
    if (id !== encomendaAberta) return;
    try { pag = await api('GET', `${API}/pagamentos/encomenda/${id}`); } catch { /* ainda não criado */ }
    if (['PAGA', 'ENVIADA'].includes(enc.estado)) {
        try { envio = await api('GET', `${API}/envios/encomenda/${id}`); } catch { /* ainda não criado */ }
    }

    $('o-cliente').textContent = [enc.clienteNome, euros.format(enc.total), enc.moradaEntrega, enc.codigoEnvio].filter(Boolean).join(' · ');
    $('o-id').textContent = enc.id;
    $('o-status').className = `status ${enc.estado}`;
    $('o-status').textContent = enc.estado;

    const semStock = enc.estado === 'CANCELADA' && !pag;
    const recusado = pag && pag.estado === 'RECUSADO';
    const passos = [
        { titulo: 'Encomenda criada', who: 'encomendas · preços do catálogo (Feign)', chave: 'stock.reservar', svc: 'encomendas', feito: true },
        semStock
            ? { titulo: 'Stock insuficiente', who: enc.motivo || 'o stock respondeu', chave: 'stock.insuficiente', svc: 'stock', falhou: true }
            : { titulo: 'Stock reservado', who: 'stock prendeu as unidades', chave: 'stock.reservado', svc: 'stock', feito: enc.estado !== 'PENDENTE' },
        { titulo: 'Pagamento pedido', who: 'pagamentos criou o pagamento PENDENTE', chave: 'pagamento.criar', svc: 'pagamentos', feito: !!pag, saltado: semStock },
        recusado
            ? { titulo: 'Pagamento recusado', who: 'compensação: o stock liberta as unidades', chave: 'stock.libertar', svc: 'stock', falhou: true }
            : { titulo: 'Pagamento aprovado', who: 'encomenda PAGA; o stock dá baixa', chave: 'stock.confirmar', svc: 'stock', feito: ['PAGA', 'ENVIADA'].includes(enc.estado), saltado: semStock },
        { titulo: 'Envio preparado', who: envio ? `envios · ${envio.codigo}` : 'envios prepara a entrega', chave: 'envio.criar', svc: 'envios', feito: !!envio, saltado: semStock || recusado },
        { titulo: 'Encomenda enviada', who: 'saiu do armazém; encomenda ENVIADA', chave: 'envio.expedido', svc: 'envios', feito: enc.estado === 'ENVIADA', saltado: semStock || recusado },
    ];
    const atual = passos.findIndex((p) => !p.feito && !p.falhou && !p.saltado);

    $('timeline').replaceChildren(...passos.map((p, i) => el('li', {
        class: p.falhou ? 'failed' : p.feito ? 'done' : i === atual ? 'current' : p.saltado ? 'skipped' : '',
        style: `--step: var(--${elementoDe(p.svc).cor})`,
    },
        el('span', { class: 'mark' }),
        el('span', { class: 'label' }, p.titulo, el('span', { class: 'who' }, p.who)),
        el('span', { class: 'when' }, el('code', {}, p.chave)))));

    // Ação simulada do passo atual: o banco decide o pagamento; depois, o armazém expede o envio
    const acao = $('o-payment');
    const pagar = pag && pag.estado === 'PENDENTE';
    const expedir = envio && envio.estado === 'EM_PREPARACAO';
    acao.hidden = !(pagar || expedir);
    if (pagar) {
        acao.replaceChildren(
            el('p', { class: 'muted small' }, 'O pagamento é simulado: decida como se fosse o banco.'),
            el('div', { class: 'actions' },
                el('button', { class: 'primary', type: 'button', onclick: () => decidirPagamento(id, 'aprovar') }, `Aprovar ${euros.format(pag.valor)}`),
                el('button', { class: 'danger', type: 'button', onclick: () => decidirPagamento(id, 'recusar') }, 'Recusar')));
    } else if (expedir) {
        acao.replaceChildren(
            el('p', { class: 'muted small' }, `Envio ${envio.codigo} em preparação${envio.morada ? ` para ${envio.morada}` : ''}.`),
            el('div', { class: 'actions' },
                el('button', { class: 'primary', type: 'button', onclick: () => expedirEnvio(id) }, 'Expedir')));
    }

    $('o-lines').replaceChildren(
        el('div', { class: 'table-wrap lines' }, el('table', { class: 'data' },
            el('thead', {}, el('tr', {}, el('th', {}, 'Peça'), el('th', { class: 'num' }, 'Qtd'), el('th', { class: 'num' }, 'Subtotal'))),
            el('tbody', {}, enc.linhas.map((l) => el('tr', {},
                el('td', {}, l.nome), el('td', { class: 'num' }, l.quantidade), el('td', { class: 'num' }, euros.format(l.subtotal))))))));
}

async function decidirPagamento(id, acao) {
    try {
        await api('POST', `${API}/pagamentos/encomenda/${id}/${acao}`);
        showNotice(acao === 'aprovar' ? 'Pagamento aprovado: o envio vai ser preparado' : 'Pagamento recusado: a SAGA vai compensar');
        desenharSaga(id);
    } catch (e) { showError(e, 'Pagamento'); }
}

async function expedirEnvio(id) {
    try {
        await api('POST', `${API}/envios/encomenda/${id}/expedir`);
        showNotice('Envio expedido: a encomenda passa a ENVIADA');
        desenharSaga(id);
    } catch (e) { showError(e, 'Envio'); }
}

async function desenharLista() {
    try {
        const lista = await api('GET', `${API}/encomendas`);
        $('history').replaceChildren(...(lista.length ? lista.slice(0, 12).map((e) => el('li', {},
            el('button', {
                class: 'history-item' + (e.id === encomendaAberta ? ' selected' : ''), type: 'button', 'data-id': e.id,
                onclick: () => { location.hash = e.id; },
            },
                el('span', {}, e.clienteNome),
                el('span', { class: `status ${e.estado}` }, e.estado),
                el('span', { class: 'muted small' }, e.id.slice(0, 8)),
                el('strong', {}, euros.format(e.total))))) :
            [el('li', { class: 'muted' }, 'Ainda não há encomendas.')]));
    } catch { /* o serviço pode estar a arrancar */ }
}

// ---------- arranque ----------

$('btn-order').addEventListener('click', encomendar);
$('btn-clear-cart').addEventListener('click', () => { cesto.clear(); desenharCesto(); });
window.addEventListener('hashchange', () => abrirEncomenda(location.hash.slice(1)));

desenharCesto();
abrirLoja();
desenharLista();
setInterval(desenharLista, 5000);
abrirEncomenda(location.hash.slice(1));
