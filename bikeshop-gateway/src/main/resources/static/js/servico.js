'use strict';

// Ficha de um microsserviço (servico.html?id=<id>): como foi construído e um painel "Experimentar" com
// chamadas reais pela gateway. O conteúdo vem de servicos.js; os painéis estão em PAINEIS, um por serviço.

const params = new URLSearchParams(location.search);
const S = servico(params.get('id')) || SERVICOS[0];
const G = elementoDe(S.id);

// ---------- blocos reutilizáveis ----------

function section(id, title, ...content) {
    return el('section', { id, class: 'svc-section', 'aria-labelledby': `${id}-t` },
        el('h2', { id: `${id}-t` }, title), ...content);
}

function method(m) {
    return el('span', { class: `method m-${m.toLowerCase().replace('*', 'any')}` }, m);
}

function tagFor(id) {
    const s = servico(id);
    if (!s) return el('span', { class: 'tag' }, id);
    return el('a', { class: `tag c-${elementoDe(s.id).cor}`, href: `servico.html?id=${s.id}` }, id);
}

function statusChip(value) {
    return el('span', { class: `status ${value || ''}` }, value || '?');
}

// Mostra o pedido e a resposta completa, para se ver exatamente o que a API devolve.
function responseView(r) {
    const text = typeof r.json === 'string' ? r.json : (JSON.stringify(r.json, null, 2) ?? '(sem corpo)');
    return el('details', { class: `response ${r.ok ? 'ok' : 'bad'}` },
        el('summary', {},
            method(r.method), el('code', {}, `/api${r.path}`),
            el('span', { class: 'resp-status' }, r.status ? `HTTP ${r.status}` : 'sem ligação'),
            el('span', { class: 'muted small' }, `${r.ms} ms`),
            !r.ok && r.json?.servico ? el('span', { class: 'resp-code' }, r.json.servico) : null),
        r.body ? el('div', {}, el('span', { class: 'muted small' }, 'Corpo do pedido'), el('pre', {}, JSON.stringify(r.body, null, 2))) : null,
        el('span', { class: 'muted small' }, 'Resposta'),
        el('pre', {}, text.length > 6000 ? `${text.slice(0, 6000)}\n…` : text));
}

// Corre um pedido e mostra o resultado em "out": primeiro a vista amigável (se houver) e depois a resposta.
async function run(out, methodName, path, body, view) {
    out.replaceChildren(el('p', { class: 'muted' }, 'A pedir…'));
    const r = await callApi(methodName, path, body);
    const friendly = r.ok && view ? view(r.json) : null;
    const error = !r.ok ? el('p', { class: 'resp-error' }, r.json?.mensagem || 'Pedido falhou.') : null;
    out.replaceChildren(...[friendly, error, responseView(r)].filter(Boolean));
    return r;
}

function table(headers, rows) {
    return el('div', { class: 'table-wrap' }, el('table', { class: 'data' },
        el('thead', {}, el('tr', {}, headers.map((h) => el('th', {}, h)))),
        el('tbody', {}, rows.length ? rows : el('tr', {}, el('td', { colspan: headers.length, class: 'muted' }, 'Sem registos.')))));
}

function field(label, attrs) {
    return el('label', {}, label, el('input', attrs));
}

async function latestOrderId() {
    const r = await callApi('GET', '/encomendas');
    return r.ok && r.json.length ? r.json[0].id : '';
}

// Campo de encomendaId com um botão que vai buscar a encomenda mais recente.
function orderIdField(id) {
    const input = el('input', { id, placeholder: 'UUID da encomenda', required: true, spellcheck: 'false' });
    return el('div', { class: 'order-id-field' },
        el('label', { for: id }, 'Encomenda (encomendaId)'),
        el('div', { class: 'inline' }, input,
            el('button', {
                type: 'button',
                class: 'btn secondary small',
                onclick: async () => { input.value = await latestOrderId() || input.value; },
            }, 'Última encomenda')));
}

const pecasTabela = (list) => table(['Referência', 'Nome', 'Categoria', 'Preço'], (list || []).map((p) => el('tr', {},
    el('td', {}, el('code', {}, p.referencia)), el('td', {}, p.nome), el('td', {}, p.categoriaNome || ''),
    el('td', { class: 'num' }, euros.format(p.preco)))));

const stockTabela = (rows) => table(['Referência', 'Armazém', 'Reservado', 'Livre', ''], (rows || []).map((r) => {
    const pct = r.quantidade ? Math.max(0, Math.min(100, (r.reservada / r.quantidade) * 100)) : 100;
    return el('tr', {},
        el('td', {}, el('code', {}, r.referencia)), el('td', { class: 'num' }, r.quantidade),
        el('td', { class: 'num' }, r.reservada), el('td', { class: `num ${r.livre <= 0 ? 'bad-text' : ''}` }, r.livre),
        el('td', {}, el('span', { class: 'bar', title: `${Math.round(pct)}% reservado` }, el('span', { style: `width:${pct}%` }))));
}));

const encomendaCartao = (o) => el('div', { class: 'order-card' },
    el('div', { class: 'order-head' },
        el('div', {}, el('strong', {}, `${o.clienteNome} · ${euros.format(o.total)}`), el('div', { class: 'muted small mono' }, o.id)),
        statusChip(o.estado)),
    o.motivo ? el('p', { class: 'muted small' }, o.motivo) : null,
    table(['Referência', 'Peça', 'Qtd', 'Subtotal'], (o.linhas || []).map((l) => el('tr', {},
        el('td', {}, el('code', {}, l.referencia)), el('td', {}, l.nome), el('td', { class: 'num' }, l.quantidade),
        el('td', { class: 'num' }, euros.format(l.subtotal))))));

const pagamentoCartao = (p) => el('dl', { class: 'kv' },
    el('dt', {}, 'Estado'), el('dd', {}, statusChip(p.estado)),
    el('dt', {}, 'Valor'), el('dd', {}, euros.format(p.valor)),
    el('dt', {}, 'Encomenda'), el('dd', {}, el('code', {}, p.encomendaId)),
    el('dt', {}, 'Criado'), el('dd', {}, new Date(p.criadoEm).toLocaleString('pt-PT')));

const clientesTabela = (list) => table(['Id', 'Nome', 'Email', 'Morada'], (list || []).map((c) => el('tr', {},
    el('td', { class: 'num' }, c.id), el('td', {}, c.nome), el('td', {}, el('code', {}, c.email)),
    el('td', { class: 'muted' }, c.moradaCompleta || 'sem morada'))));

const envioCartao = (v) => el('dl', { class: 'kv' },
    el('dt', {}, 'Estado'), el('dd', {}, statusChip(v.estado)),
    el('dt', {}, 'Código'), el('dd', {}, el('code', {}, v.codigo)),
    el('dt', {}, 'Cliente'), el('dd', {}, v.clienteNome),
    el('dt', {}, 'Morada'), el('dd', {}, v.morada || 'sem morada'),
    el('dt', {}, 'Artigos'), el('dd', {}, v.artigos),
    el('dt', {}, 'Encomenda'), el('dd', {}, el('code', {}, v.encomendaId)));

// ---------- painéis "Experimentar", um por serviço ----------

const PAINEIS = {
    catalogo() {
        const out = el('div', { class: 'out' });
        const btn = (label, path, view) => el('button', { class: 'btn secondary small', type: 'button', onclick: () => run(out, 'GET', path, null, view) }, label);
        const ref = el('input', { value: 'BAT-500', 'aria-label': 'Referência', spellcheck: 'false' });
        const cat = el('input', { value: 'BAT', 'aria-label': 'Categoria', spellcheck: 'false' });
        const max = el('input', { value: '700', type: 'number', min: '0', 'aria-label': 'Preço máximo' });
        return [
            el('div', { class: 'try-bar' },
                btn('Categorias', '/categorias', (list) => table(['Código', 'Nome', 'Descrição'],
                    list.map((c) => el('tr', {}, el('td', {}, el('code', {}, c.codigo)), el('td', {}, c.nome), el('td', { class: 'muted' }, c.descricao || ''))))),
                btn('Todas as peças', '/pecas', pecasTabela)),
            el('form', { class: 'try-bar', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'GET', `/pecas?categoria=${encodeURIComponent(cat.value.trim())}&precoMax=${encodeURIComponent(max.value)}`, null, pecasTabela);
            } }, el('label', { class: 'inline-label' }, 'Categoria', cat), el('label', { class: 'inline-label' }, 'Preço até', max),
                el('button', { class: 'btn primary small' }, 'GET filtrado (JPQL)')),
            el('form', { class: 'try-bar', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'GET', `/pecas/${encodeURIComponent(ref.value.trim())}`, null, (p) => pecasTabela([p]));
            } }, el('label', { class: 'inline-label' }, 'Peça por referência', ref), el('button', { class: 'btn primary small' }, 'GET por referência')),
            el('p', { class: 'muted small' }, 'Experimente uma referência que não existe para ver o 404 com o nome do serviço.'),
            out,
        ];
    },

    stock() {
        const out = el('div', { class: 'out' });
        const ref = el('input', { value: 'BAT-500', 'aria-label': 'Referência', spellcheck: 'false' });
        const qtd = el('input', { value: '20', type: 'number', min: '0', 'aria-label': 'Quantidade' });
        return [
            el('div', { class: 'try-bar' },
                el('button', { class: 'btn primary small', type: 'button', onclick: () => run(out, 'GET', '/stock', null, stockTabela) }, 'Ver stock')),
            el('form', { class: 'try-bar', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'PUT', `/stock/${encodeURIComponent(ref.value.trim())}`, { quantidade: Number(qtd.value) }, (s) => stockTabela([s]));
            } }, el('label', { class: 'inline-label' }, 'Referência', ref), el('label', { class: 'inline-label' }, 'Quantidade', qtd),
                el('button', { class: 'btn secondary small' }, 'PUT repor armazém')),
            el('form', { class: 'try-form', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'GET', `/stock/reservas/${encodeURIComponent($('stk-order').value.trim())}`, null, (list) => table(['Referência', 'Qtd', 'Estado'],
                    list.map((r) => el('tr', {}, el('td', {}, el('code', {}, r.referencia)), el('td', { class: 'num' }, r.quantidade), el('td', {}, statusChip(r.estado))))));
            } }, orderIdField('stk-order'), el('button', { class: 'btn secondary small' }, 'GET reservas')),
            el('p', { class: 'muted small' }, 'A quantidade não pode ficar abaixo do reservado (400).'),
            out,
        ];
    },

    pagamentos() {
        const out = el('div', { class: 'out' });
        const id = () => $('pag-order').value.trim();
        const act = (acao) => el('button', {
            type: 'button',
            class: `btn ${acao === 'aprovar' ? 'primary' : 'secondary'} small`,
            onclick: () => id() && run(out, 'POST', `/pagamentos/encomenda/${encodeURIComponent(id())}/${acao}`, null, pagamentoCartao),
        }, `POST ${acao}`);
        return [
            el('div', { class: 'try-bar' },
                el('button', { class: 'btn primary small', type: 'button', onclick: () => run(out, 'GET', '/pagamentos', null, (list) => table(['Encomenda', 'Estado', 'Valor'],
                    list.map((p) => el('tr', {}, el('td', {}, el('code', { title: p.encomendaId }, `${p.encomendaId.slice(0, 8)}…`)), el('td', {}, statusChip(p.estado)),
                        el('td', { class: 'num' }, euros.format(p.valor)))))) }, 'Todos os pagamentos')),
            el('form', { class: 'try-form', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'GET', `/pagamentos/encomenda/${encodeURIComponent(id())}`, null, pagamentoCartao);
            } }, orderIdField('pag-order'), el('button', { class: 'btn secondary small' }, 'GET pagamento')),
            el('div', { class: 'try-bar' }, el('span', { class: 'muted small' }, 'Simular o banco (faz avançar a encomenda real):'), act('aprovar'), act('recusar')),
            out,
        ];
    },

    clientes() {
        const out = el('div', { class: 'out' });
        const email = el('input', { value: 'rita@bikeshop.pt', type: 'email', 'aria-label': 'Email', spellcheck: 'false' });
        const form = el('form', { class: 'form', onsubmit: (e) => {
            e.preventDefault();
            const f = e.target.elements;
            run(out, 'POST', '/clientes', {
                nome: f.nome.value, email: f.email.value, telefone: f.telefone.value || null,
                morada: f.morada.value || null, codigoPostal: f.codigoPostal.value || null, localidade: f.localidade.value || null,
            }, (c) => clientesTabela([c]));
        } },
        el('h3', { class: 'wide' }, 'Criar cliente'),
        field('Nome', { name: 'nome', value: 'Marta Sousa', required: true }),
        field('Email', { name: 'email', type: 'email', value: 'marta@bikeshop.pt', required: true }),
        field('Telefone', { name: 'telefone', value: '917654321' }),
        field('Morada', { name: 'morada', value: 'Rua do Almada 50' }),
        field('Código postal', { name: 'codigoPostal', value: '4050-032' }),
        field('Localidade', { name: 'localidade', value: 'Porto' }),
        el('div', { class: 'actions wide' }, el('button', { class: 'btn primary' }, 'POST clientes')),
        el('p', { class: 'muted small wide' }, 'Responde 201. Um email repetido ou um código postal fora do formato 0000-000 dão 400.'));
        return [
            el('div', { class: 'try-bar' },
                el('button', { class: 'btn primary small', type: 'button', onclick: () => run(out, 'GET', '/clientes', null, clientesTabela) }, 'Todos os clientes')),
            el('form', { class: 'try-bar', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'GET', `/clientes/email/${encodeURIComponent(email.value.trim())}`, null, (c) => clientesTabela([c]));
            } }, el('label', { class: 'inline-label' }, 'Email', email), el('button', { class: 'btn secondary small' }, 'GET por email')),
            form,
            out,
        ];
    },

    envios() {
        const out = el('div', { class: 'out' });
        const id = () => $('env-order').value.trim();
        return [
            el('div', { class: 'try-bar' },
                el('button', { class: 'btn primary small', type: 'button', onclick: () => run(out, 'GET', '/envios', null, (list) => table(['Encomenda', 'Código', 'Estado', 'Artigos'],
                    list.map((v) => el('tr', {}, el('td', {}, el('code', { title: v.encomendaId }, `${v.encomendaId.slice(0, 8)}…`)), el('td', {}, el('code', {}, v.codigo)),
                        el('td', {}, statusChip(v.estado)), el('td', { class: 'num' }, v.artigos))))) }, 'Todos os envios')),
            el('form', { class: 'try-form', onsubmit: (e) => {
                e.preventDefault();
                run(out, 'GET', `/envios/encomenda/${encodeURIComponent(id())}`, null, envioCartao);
            } }, orderIdField('env-order'), el('button', { class: 'btn secondary small' }, 'GET envio')),
            el('div', { class: 'try-bar' }, el('span', { class: 'muted small' }, 'Simular a saída do armazém (a encomenda passa a ENVIADA):'),
                el('button', { class: 'btn primary small', type: 'button', onclick: () => id() && run(out, 'POST', `/envios/encomenda/${encodeURIComponent(id())}/expedir`, null, envioCartao) }, 'POST expedir')),
            el('p', { class: 'muted small' }, 'Só existe envio depois de a encomenda ser paga.'),
            out,
        ];
    },

    encomendas() {
        const out = el('div', { class: 'out' });
        const form = el('form', { class: 'form', onsubmit: (e) => {
            e.preventDefault();
            const f = e.target.elements;
            run(out, 'POST', '/encomendas', {
                clienteNome: f.nome.value, clienteEmail: f.email.value,
                linhas: [{ referencia: f.referencia.value.trim(), quantidade: Number(f.quantidade.value) }],
            }, encomendaCartao);
        } },
        el('h3', { class: 'wide' }, 'Criar encomenda'),
        field('Nome', { name: 'nome', value: 'Rita Moreira', required: true }),
        field('Email', { name: 'email', type: 'email', value: 'rita@bikeshop.pt', required: true }),
        field('Referência', { name: 'referencia', value: 'BAT-500', required: true }),
        field('Quantidade', { name: 'quantidade', type: 'number', min: '1', value: '1', required: true }),
        el('div', { class: 'actions wide' }, el('button', { class: 'btn primary' }, 'POST encomendas')),
        el('p', { class: 'muted small wide' }, 'Responde 202 PENDENTE; consulte de seguida para a ver RESERVADA. Uma referência inexistente dá erro.'));
        const find = el('form', { class: 'form', onsubmit: (e) => {
            e.preventDefault();
            run(out, 'GET', `/encomendas/${encodeURIComponent($('enc-order').value.trim())}`, null, encomendaCartao);
        } },
        el('h3', { class: 'wide' }, 'Consultar'),
        el('div', { class: 'wide' }, orderIdField('enc-order')),
        el('div', { class: 'actions wide' },
            el('button', { class: 'btn primary' }, 'GET encomenda'),
            el('button', { class: 'btn secondary', type: 'button', onclick: () => run(out, 'GET', '/encomendas', null, (list) => table(['Cliente', 'Estado', 'Total'],
                list.slice(0, 10).map((o) => el('tr', {}, el('td', {}, o.clienteNome), el('td', {}, statusChip(o.estado)), el('td', { class: 'num' }, euros.format(o.total)))))) }, 'Últimas 10')));
        return [el('div', { class: 'try-grid' }, form, find), out,
            el('p', { class: 'muted small' }, 'Para ver a SAGA passo a passo use a ', el('a', { href: 'loja.html' }, 'loja'), '.')];
    },

    gateway() {
        const rows = S.rotas.map(([prefix, target, probePath]) => {
            const result = el('td', { class: 'muted' }, '—');
            const test = async () => {
                result.replaceChildren('…');
                const start = performance.now();
                let status = 0;
                try { status = (await fetch(probePath, { cache: 'no-store' })).status; } catch { /* sem ligação */ }
                const ok = status >= 200 && status < 300;
                result.replaceChildren(el('span', { class: `status ${ok ? 'PAGA' : 'CANCELADA'}` }, `HTTP ${status || '—'}`), ` ${Math.round(performance.now() - start)} ms`);
            };
            const row = el('tr', {},
                el('td', {}, el('code', {}, prefix)),
                el('td', {}, tagFor(target)),
                el('td', {}, el('button', { class: 'link', type: 'button', onclick: test }, 'Testar')),
                result);
            row.test = test;
            return row;
        });
        return [
            el('div', { class: 'try-bar' },
                el('button', { class: 'btn primary small', type: 'button', onclick: () => rows.forEach((r) => r.test()) }, 'Testar todas as rotas')),
            table(['Caminho público', 'Destino', '', 'Resultado'], rows),
            el('p', { class: 'muted small' }, '503 quer dizer que a gateway não encontrou nenhuma instância do serviço no Eureka.'),
        ];
    },

    registo() {
        const grid = el('div', { class: 'status-grid' }, SERVICOS.map((s) => el('a', { class: `status-card c-${elementoDe(s.id).cor}`, href: `servico.html?id=${s.id}` },
            el('span', { class: 'dot', 'data-svc': s.id }), el('strong', {}, s.nome), el('span', { class: 'muted small' }, `:${s.porta}`))));
        const out = el('div', { class: 'out' });
        const registry = async () => {
            out.replaceChildren(el('p', { class: 'muted' }, 'A pedir…'));
            try {
                const apps = [].concat((await api('GET', '/bastidores/registo')).applications.application || []);
                const rows = apps.flatMap((a) => [].concat(a.instance).map((i) => el('tr', {},
                    el('td', {}, el('code', {}, a.name)), el('td', {}, i.instanceId || `${i.hostName}:${i.port?.$}`),
                    el('td', {}, el('span', { class: `status ${i.status === 'UP' ? 'PAGA' : 'CANCELADA'}` }, i.status)))));
                out.replaceChildren(table(['Aplicação', 'Instância', 'Estado'], rows),
                    el('p', { class: 'muted small' }, `${apps.length} aplicações registadas (o Eureka não se regista a si próprio).`));
            } catch {
                out.replaceChildren(el('p', { class: 'resp-error' }, 'O Eureka não respondeu (porta 9761).'));
            }
        };
        return [
            el('div', { class: 'try-bar' },
                el('button', { class: 'btn primary small', type: 'button', onclick: refreshStatus }, 'Atualizar estado'),
                el('button', { class: 'btn secondary small', type: 'button', onclick: registry }, 'Ler o registo do Eureka'),
                el('a', { class: 'btn secondary small', href: 'http://localhost:9761', target: '_blank', rel: 'noopener' }, 'Abrir o painel do Eureka')),
            grid,
            out,
            el('p', { class: 'muted small' }, 'O browser lê o registo do Eureka através da gateway (rota só de leitura /bastidores/registo), porque o Eureka está noutra porta e não permite CORS.'),
        ];
    },
};

// ---------- ficha ----------

// Barra lateral: uma caixa por equipa, com os seus serviços, porta e estado.
function renderSide() {
    $('svc-list').replaceChildren(...GRUPO.map((g) => el('div', { class: `side-box c-${g.cor}` },
        el('span', { class: 'side-head' }, g.equipa, el('small', {}, `Elemento ${g.n}`)),
        g.servicos.map((id) => {
            const s = servico(id);
            return el('a', { href: `servico.html?id=${id}`, 'aria-current': id === S.id ? 'page' : false },
                el('span', { class: 'dot', 'data-svc': id }), s.nome, el('span', { class: 'port' }, s.porta));
        }))));
}

function flow() {
    const list = (items, empty) => (items.length ? el('ul', {}, items.map((x) => el('li', {}, x))) : el('p', { class: 'muted small' }, empty));
    const evt = ([key, other, nota]) => el('span', {}, el('code', {}, key), ' ', tagFor(other), el('span', { class: 'muted small' }, ` ${nota}`));
    return el('div', { class: 'flow' },
        el('div', { class: 'flow-col' },
            el('h3', {}, 'Recebe'),
            el('span', { class: 'flow-sub' }, 'REST de'), list(S.chamadoPor.map(tagFor), '—'),
            el('span', { class: 'flow-sub' }, 'Mensagens que consome'), list(S.consome.map(evt), 'Nenhuma')),
        el('div', { class: 'flow-core' }, el('span', { class: 'dot', 'data-svc': S.id }), el('strong', {}, S.nome), el('span', { class: 'muted small' }, S.bd)),
        el('div', { class: 'flow-col' },
            el('h3', {}, 'Envia'),
            el('span', { class: 'flow-sub' }, S.id === 'gateway' ? 'Encaminha para' : 'Chama por Feign'), list(S.chama.map(tagFor), 'Ninguém'),
            el('span', { class: 'flow-sub' }, 'Mensagens que publica'), list(S.publica.map(evt), 'Nenhuma')));
}

function render() {
    document.title = `${S.nome} · BikeShop`;
    document.body.style.setProperty('--svc', `var(--${G.cor})`);
    document.body.classList.add(`c-${G.cor}`);

    const idx = SERVICOS.indexOf(S);
    const prev = SERVICOS[(idx + SERVICOS.length - 1) % SERVICOS.length];
    const next = SERVICOS[(idx + 1) % SERVICOS.length];
    const totalTests = S.testes.reduce((n, t) => n + t[1], 0);
    const sections = [
        ['faz', 'O que faz'], ['encaixa', 'Como encaixa'], ['dados', S.rotas ? 'Rotas' : 'Dados'], ['api', 'API'],
        ['decisoes', 'Decisões'], ['testes', 'Testes'], ['experimentar', 'Experimentar'], ['correr', 'Como correr'],
    ].filter(([id]) => id !== 'testes' || S.testes.length);

    $('svc').replaceChildren(
        el('nav', { class: 'crumbs', 'aria-label': 'Caminho' }, el('a', { href: 'index.html' }, 'Visão geral'), ' / ', el('span', {}, 'Microsserviços'), ' / ', el('span', { 'aria-current': 'page' }, S.nome)),
        el('header', { class: 'svc-hero' },
            el('div', { class: 'svc-title' },
                el('h1', {}, el('span', { class: 'dot big', 'data-svc': S.id }), S.nome),
                el('p', { class: 'lead' }, S.resumo)),
            el('div', { class: 'svc-meta' },
                el('span', { class: 'chip' }, `porta ${S.porta}`),
                el('span', { class: 'chip' }, S.tipo === 'negócio' ? 'serviço de negócio' : 'infraestrutura'),
                el('span', { class: 'chip' }, S.bd),
                el('span', { class: 'chip owner' }, el('span', { class: 'swatch' }), `${G.equipa} · Elemento ${G.n}`))),
        el('nav', { class: 'toc', 'aria-label': 'Secções' }, sections.map(([id, label]) => el('a', { href: `#${id}` }, label))),

        section('faz', 'O que faz',
            el('ul', { class: 'checks' }, S.responsabilidade.map((r) => el('li', {}, r))),
            el('h3', {}, 'Tecnologias'),
            el('div', { class: 'chips-row' }, S.tecnologias.map((t) => el('span', { class: 'chip' }, t)))),

        section('encaixa', 'Como encaixa', flow()),

        S.rotas
            ? section('dados', 'Rotas', table(['Caminho público', 'Encaminha para'], S.rotas.map(([p, t]) => el('tr', {}, el('td', {}, el('code', {}, p)), el('td', {}, tagFor(t))))),
                el('p', { class: 'muted small' }, 'As rotas de negócio usam lb://<nome no Eureka>; as de leitura (/bastidores/*) vão diretas ao Eureka e à API de gestão do RabbitMQ.'))
            : section('dados', 'Modelo de dados',
                S.entidades.length
                    ? el('div', { class: 'entities' }, S.entidades.map((e) => el('div', { class: 'entity' },
                        el('strong', {}, e.nome), el('ul', {}, e.atributos.map((a) => el('li', {}, a))))))
                    : el('p', { class: 'muted' }, 'Não guarda dados de negócio.'),
                S.relacao ? el('p', { class: 'muted' }, S.relacao) : null,
                S.estados.map((st) => el('div', { class: 'states' },
                    el('span', { class: 'flow-sub' }, st.nome), el('div', { class: 'state-line' }, st.valores.map((v) => statusChip(v)))))),

        section('api', 'API',
            S.tipo === 'negócio' ? el('p', { class: 'muted small' }, 'Caminhos públicos, pela gateway em http://localhost:9080.') : null,
            table(['Método', 'Caminho', 'Para quê'], S.endpoints.map(([m, p, d]) => el('tr', {}, el('td', {}, method(m)), el('td', {}, el('code', {}, p)), el('td', {}, d))))),

        section('decisoes', 'Decisões e padrões', el('ul', { class: 'decisions' }, S.decisoes.map((d) => el('li', {}, d)))),

        S.testes.length ? section('testes', `Testes (${totalTests})`,
            table(['Classe', 'Testes', 'O que verifica'], S.testes.map(([c, n, d]) => el('tr', {}, el('td', {}, el('code', {}, c)), el('td', { class: 'num' }, n), el('td', {}, d)))),
            el('pre', { class: 'cmd' }, `.\\mvnw.cmd -pl ${S.nome} test`)) : null,

        section('experimentar', 'Experimentar ao vivo',
            el('p', { class: 'muted' }, 'Pedidos reais pela gateway. Abra a resposta para ver o corpo completo.'),
            el('div', { class: 'try' }, PAINEIS[S.id]())),

        section('correr', 'Como correr',
            el('dl', { class: 'kv' },
                el('dt', {}, 'Base de dados'), el('dd', {}, S.bd),
                el('dt', {}, 'Porta'), el('dd', {}, String(S.porta))),
            el('h3', {}, 'Tudo em Docker'),
            el('pre', { class: 'cmd' }, ['.\\scripts\\arrancar.ps1', '.\\scripts\\e2e-test.ps1'].join('\n')),
            el('h3', {}, 'Só este serviço, no anfitrião'),
            el('pre', { class: 'cmd' }, ['docker compose up -d --wait', `.\\mvnw.cmd -pl ${S.nome} -am package -DskipTests`, `java -jar ${S.nome}\\target\\${S.nome}.jar`].join('\n')),
            el('p', { class: 'muted small' }, 'Primeiro o bikeshop-registo; os valores por omissão do application.yml apontam para localhost.'),
            el('h3', {}, 'Confirmar'),
            el('pre', { class: 'cmd' }, S.verificar[0]),
            el('p', { class: 'muted small' }, 'Esperado: ', S.verificar[1])),

        el('nav', { class: 'pager', 'aria-label': 'Outros serviços' },
            el('a', { href: `servico.html?id=${prev.id}` }, el('span', { class: 'muted small' }, '← anterior'), prev.nome),
            el('a', { href: `servico.html?id=${next.id}` }, el('span', { class: 'muted small' }, 'seguinte →'), next.nome)),
    );
}

renderSide();
render();
applyStatus();
