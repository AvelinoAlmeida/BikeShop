// BikeShop: interface sem bibliotecas. Todos os pedidos passam pela gateway (mesma origem, sem CORS).
'use strict';

const API = '/api';
const euros = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' });

// ---------- utilitários ----------
function el(tag, atributos = {}, ...filhos) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(atributos)) {
    if (v == null || v === false) continue;
    if (k.startsWith('on')) n.addEventListener(k.slice(2), v);
    else if (k === 'class') n.className = v;
    else n.setAttribute(k, v === true ? '' : v);
  }
  for (const f of filhos.flat()) if (f != null) n.append(f instanceof Node ? f : document.createTextNode(String(f)));
  return n;
}

async function pedido(caminho, opcoes = {}) {
  const r = await fetch(caminho, { headers: { 'Content-Type': 'application/json' }, cache: 'no-store', ...opcoes });
  const texto = await r.text();
  const corpo = texto ? (() => { try { return JSON.parse(texto); } catch { return texto; } })() : null;
  if (!r.ok) throw new Error((corpo && corpo.mensagem) || `Erro ${r.status} em ${caminho}`);
  return corpo;
}

let temporizadorAviso;
function aviso(texto, erro = false) {
  const a = document.getElementById('aviso');
  a.textContent = texto;
  a.className = 'aviso mostrar' + (erro ? ' erro' : '');
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { a.className = 'aviso'; }, 3500);
}

// ---------- navegação ----------
const temporizadores = [];
function mostrarPagina() {
  const [pagina, parametro] = (location.hash.slice(1) || 'loja').split('/');
  temporizadores.splice(0).forEach(clearInterval);
  document.querySelectorAll('.pagina').forEach(p => { p.hidden = p.id !== pagina; });
  document.querySelectorAll('.menu a').forEach(a => a.classList.toggle('ativo', a.dataset.pagina === pagina));
  if (pagina === 'loja') abrirLoja();
  if (pagina === 'encomenda') abrirEncomenda(parametro);
  if (pagina === 'bastidores') abrirBastidores();
}
window.addEventListener('hashchange', mostrarPagina);

// ---------- loja ----------
let categoriaAtual = null;
let stockPorReferencia = {};

async function abrirLoja() {
  try {
    const [categorias, stock] = await Promise.all([pedido(`${API}/categorias`), pedido(`${API}/stock`)]);
    stockPorReferencia = Object.fromEntries(stock.map(s => [s.referencia, s]));
    const filtros = document.getElementById('filtros');
    filtros.replaceChildren(
      el('button', { class: 'filtro' + (categoriaAtual ? '' : ' ativo'), onclick: () => { categoriaAtual = null; abrirLoja(); } }, 'Tudo'),
      ...categorias.map(c => el('button', {
        class: 'filtro' + (categoriaAtual === c.codigo ? ' ativo' : ''), title: c.descricao,
        onclick: () => { categoriaAtual = c.codigo; abrirLoja(); }
      }, c.nome)));
    const pecas = await pedido(`${API}/pecas` + (categoriaAtual ? `?categoria=${categoriaAtual}` : ''));
    document.getElementById('pecas').replaceChildren(...pecas.map(cartaoPeca));
  } catch (e) {
    document.getElementById('pecas').replaceChildren(el('p', { class: 'vazio' }, 'A loja ainda não responde: ' + e.message));
  }
}

function cartaoPeca(p) {
  const s = stockPorReferencia[p.referencia];
  const livre = s ? s.livre : 0;
  const etiqueta = livre === 0 ? ['sem', 'Esgotado'] : livre <= 3 ? ['pouco', `Últimas ${livre}`] : ['ha', `${livre} em stock`];
  return el('article', { class: 'peca' },
    el('span', { class: 'cat' }, p.categoriaNome),
    el('h3', {}, p.nome),
    el('span', { class: 'marca-peca' }, `${p.marca} · ${p.referencia}`),
    p.compatibilidade ? el('span', { class: 'compat' }, p.compatibilidade) : null,
    el('p', {}, p.descricao),
    el('div', { class: 'rodape' },
      el('div', {}, el('div', { class: 'preco' }, euros.format(p.preco)), el('span', { class: 'stock ' + etiqueta[0] }, etiqueta[1])),
      el('button', { class: 'botao', disabled: livre === 0, onclick: () => adicionar(p) }, 'Adicionar')));
}

// ---------- cesto ----------
const cesto = new Map();   // referencia -> { peca, quantidade }

function adicionar(peca) {
  const linha = cesto.get(peca.referencia) || { peca, quantidade: 0 };
  linha.quantidade++;
  cesto.set(peca.referencia, linha);
  desenharCesto();
  aviso(`${peca.nome} no cesto`);
}

function mudarQuantidade(referencia, delta) {
  const linha = cesto.get(referencia);
  linha.quantidade += delta;
  if (linha.quantidade <= 0) cesto.delete(referencia);
  desenharCesto();
}

function desenharCesto() {
  const linhas = [...cesto.values()];
  document.getElementById('contagem').textContent = linhas.reduce((n, l) => n + l.quantidade, 0);
  document.getElementById('totalCesto').textContent = euros.format(linhas.reduce((t, l) => t + l.peca.preco * l.quantidade, 0));
  document.getElementById('linhasCesto').replaceChildren(...(linhas.length ? linhas.map(l => el('div', { class: 'linha' },
    el('div', {}, el('b', {}, l.peca.nome), el('br'), el('small', { style: 'color:var(--cinza)' }, euros.format(l.peca.preco))),
    el('div', { class: 'qtd' },
      el('button', { type: 'button', onclick: () => mudarQuantidade(l.peca.referencia, -1), 'aria-label': 'Menos' }, '−'),
      el('span', {}, l.quantidade),
      el('button', { type: 'button', onclick: () => mudarQuantidade(l.peca.referencia, 1), 'aria-label': 'Mais' }, '+')))) :
    [el('p', { class: 'vazio' }, 'O cesto está vazio.')]));
  document.getElementById('encomendar').disabled = linhas.length === 0;
}

function abrirCesto(aberto) { document.getElementById('cesto').classList.toggle('aberto', aberto); }
document.getElementById('abrirCesto').addEventListener('click', () => abrirCesto(true));
document.getElementById('fecharCesto').addEventListener('click', () => abrirCesto(false));

document.getElementById('encomendar').addEventListener('click', async () => {
  const nome = document.getElementById('nome').value.trim();
  const email = document.getElementById('email').value.trim();
  if (!nome || !email) { aviso('Indique o nome e o email', true); return; }
  try {
    const enc = await pedido(`${API}/encomendas`, {
      method: 'POST',
      body: JSON.stringify({ clienteNome: nome, clienteEmail: email, linhas: [...cesto.values()].map(l => ({ referencia: l.peca.referencia, quantidade: l.quantidade })) })
    });
    cesto.clear();
    desenharCesto();
    abrirCesto(false);
    location.hash = `encomenda/${enc.id}`;
  } catch (e) { aviso(e.message, true); }
});

// ---------- encomenda: a SAGA ----------
async function abrirEncomenda(id) {
  desenharLista();
  temporizadores.push(setInterval(desenharLista, 5000));
  if (!id) return;
  document.getElementById('encomendaId').textContent = id.slice(0, 8);
  await desenharSaga(id);
  temporizadores.push(setInterval(() => desenharSaga(id), 1000));
}

async function desenharLista() {
  try {
    const lista = await pedido(`${API}/encomendas`);
    document.getElementById('encomendas').replaceChildren(...(lista.length ? lista.slice(0, 12).map(e => el('li', { onclick: () => { location.hash = `encomenda/${e.id}`; } },
      el('span', {}, e.clienteNome, el('br'), el('small', {}, `${e.id.slice(0, 8)} · ${euros.format(e.total)}`)),
      el('span', { class: 'estado ' + e.estado }, e.estado))) : [el('li', { class: 'vazio' }, 'Ainda não há encomendas.')]));
  } catch { /* o serviço pode estar a arrancar */ }
}

async function desenharSaga(id) {
  let enc, pag = null;
  try { enc = await pedido(`${API}/encomendas/${id}`); } catch (e) { document.getElementById('saga').replaceChildren(el('p', { class: 'vazio' }, e.message)); return; }
  try { pag = await pedido(`${API}/pagamentos/encomenda/${id}`); } catch { /* ainda não criado */ }

  const semStock = enc.estado === 'CANCELADA' && !pag;
  const recusado = pag && pag.estado === 'RECUSADO';
  const passos = [
    { titulo: 'Encomenda criada', texto: `POST /api/encomendas gravou a encomenda PENDENTE com os preços do catálogo (Feign) e publicou`, chave: 'stock.reservar', feito: true },
    semStock
      ? { titulo: 'Stock insuficiente', texto: enc.motivo || 'O stock respondeu', chave: 'stock.insuficiente', falhou: true }
      : { titulo: 'Stock reservado', texto: 'O bikeshop-stock prendeu as unidades e respondeu', chave: 'stock.reservado', feito: enc.estado !== 'PENDENTE' },
    { titulo: 'Pagamento pedido', texto: 'A encomenda publicou o pedido e o bikeshop-pagamentos criou o pagamento PENDENTE', chave: 'pagamento.criar', feito: !!pag, saltado: semStock },
    recusado
      ? { titulo: 'Pagamento recusado', texto: 'Compensação: a encomenda foi cancelada e pediu ao stock para libertar as unidades', chave: 'stock.libertar', falhou: true }
      : { titulo: 'Pagamento aprovado', texto: 'A encomenda ficou PAGA e o stock deu baixa das unidades', chave: 'stock.confirmar', feito: enc.estado === 'PAGA', saltado: semStock }
  ];
  const atual = passos.findIndex(p => !p.feito && !p.falhou && !p.saltado);

  const lista = el('ol', { class: 'saga' }, passos.map((p, i) => el('li', { class: p.falhou ? 'falhou' : p.feito ? 'feito' : i === atual ? 'agora' : '', style: p.saltado ? 'opacity:.35' : null },
    el('span', { class: 'ponto' }, p.falhou ? '!' : i + 1),
    el('h3', {}, p.titulo),
    el('p', {}, p.texto, ' ', el('code', {}, p.chave)))));

  const linhas = el('table', { class: 'tabela', style: 'margin-top:8px' },
    el('thead', {}, el('tr', {}, el('th', {}, 'Peça'), el('th', {}, 'Qtd'), el('th', {}, 'Subtotal'))),
    el('tbody', {}, enc.linhas.map(l => el('tr', {}, el('td', {}, l.nome), el('td', { class: 'num' }, l.quantidade), el('td', { class: 'num' }, euros.format(l.subtotal))))));

  const acoes = pag && pag.estado === 'PENDENTE' ? el('div', { class: 'acoes' },
    el('button', { class: 'botao', onclick: () => pagar(id, 'aprovar') }, `Aprovar pagamento de ${euros.format(pag.valor)}`),
    el('button', { class: 'botao perigo', onclick: () => pagar(id, 'recusar') }, 'Recusar (simular cartão recusado)')) : null;

  document.getElementById('saga').replaceChildren(
    el('p', {}, el('span', { class: 'estado ' + enc.estado }, enc.estado), ' ', `${enc.clienteNome} · total ${euros.format(enc.total)}`),
    lista, acoes, linhas);
}

async function pagar(id, acao) {
  try {
    await pedido(`${API}/pagamentos/encomenda/${id}/${acao}`, { method: 'POST' });
    aviso(acao === 'aprovar' ? 'Pagamento aprovado' : 'Pagamento recusado: a SAGA vai compensar');
  } catch (e) { aviso(e.message, true); }
}

// ---------- bastidores ----------
const SERVICOS = [
  { nome: 'bikeshop-registo', porta: 9761, texto: 'Eureka: lista onde cada serviço se regista.' },
  { nome: 'bikeshop-gateway', porta: 9080, texto: 'Entrada única: encaminha /api/** e serve esta página.' },
  { nome: 'bikeshop-catalogo', porta: 9081, teste: '/api/pecas/landing', texto: 'Categorias e peças com preço (PostgreSQL bikeshop_catalogo).' },
  { nome: 'bikeshop-stock', porta: 9082, teste: '/api/stock/landing', texto: 'Quantidades e reservas; responde stock.reservado ou stock.insuficiente.' },
  { nome: 'bikeshop-pagamentos', porta: 9083, teste: '/api/pagamentos/landing', texto: 'Pagamentos simulados; publica pagamento.aprovado ou pagamento.recusado.' },
  { nome: 'bikeshop-encomendas', porta: 9084, teste: '/api/encomendas/landing', texto: 'Orquestra a SAGA e chama o catálogo por Feign.' }
];
const CONSUMIDOR = { stock: 'bikeshop-stock', pagamentos: 'bikeshop-pagamentos', encomendas: 'bikeshop-encomendas' };

function abrirBastidores() {
  desenharBastidores();
  temporizadores.push(setInterval(desenharBastidores, 3000));
}

async function desenharBastidores() {
  let registados = [];
  let registoOk = false;
  try {
    const r = await pedido('/bastidores/registo');
    registados = [].concat(r.applications.application || []).map(a => a.name.toLowerCase());
    registoOk = true;
  } catch { /* registo em baixo */ }

  const estados = await Promise.all(SERVICOS.map(async s => {
    if (s.nome === 'bikeshop-registo') return registoOk;
    if (s.nome === 'bikeshop-gateway') return true;
    try { await pedido(s.teste); return true; } catch { return false; }
  }));
  document.getElementById('servicos').replaceChildren(...SERVICOS.map((s, i) => el('div', { class: 'servico ' + (estados[i] ? 'ok' : 'mal') },
    el('h3', {}, s.nome, el('span', { class: 'luz' })),
    el('p', {}, el('span', { class: 'porta' }, `:${s.porta}`), ' · ', s.texto),
    el('p', {}, s.nome === 'bikeshop-registo' ? `${registados.length} serviços registados` : registados.includes(s.nome) ? 'Registado no Eureka' : 'Não registado'))));

  try {
    const filas = (await pedido('/bastidores/filas')).filter(f => f.name.endsWith('.fila'));
    document.getElementById('filas').replaceChildren(...filas.map(f => el('tr', {},
      el('td', {}, el('code', {}, f.name)), el('td', {}, CONSUMIDOR[f.name.split('.')[0]] || ''),
      el('td', { class: 'num' }, f.messages ?? 0), el('td', { class: 'num' }, f.consumers ?? 0))));
  } catch { document.getElementById('filas').replaceChildren(el('tr', {}, el('td', { colspan: 4 }, 'RabbitMQ indisponível'))); }

  try {
    const stock = await pedido(`${API}/stock`);
    document.getElementById('stock').replaceChildren(...stock.map(s => el('tr', {},
      el('td', {}, s.referencia), el('td', { class: 'num' }, s.quantidade), el('td', { class: 'num' }, s.reservada), el('td', { class: 'num' }, s.livre))));
  } catch { /* stock em baixo */ }
}

// ---------- arranque ----------
desenharCesto();
mostrarPagina();
