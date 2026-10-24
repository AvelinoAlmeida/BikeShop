'use strict';

// Visão geral: diagrama da arquitetura, cartões do grupo, passos da SAGA (tudo a partir de servicos.js)
// e as tabelas "Ao vivo" das filas e do stock.

function card(s) {
    const g = elementoDe(s.id);
    return el('a', { class: `arch-node svc c-${g.cor}`, href: `servico.html?id=${s.id}` },
        el('span', { class: 'svc-top' },
            el('span', { class: 'dot', 'data-svc': s.id, title: 'estado visto pela gateway' }),
            el('strong', {}, s.nome),
            el('span', { class: 'port' }, `:${s.porta}`)),
        el('span', { class: 'svc-desc' }, s.resumo),
        el('span', { class: 'svc-foot' },
            el('span', { class: 'db' }, s.bd.replace(/ \(\d+\)/, '')),
            el('span', { class: 'who' }, g.equipa)));
}

function renderArch() {
    $('arch-infra').replaceChildren(card(servico('gateway')), card(servico('registo')));
    $('arch-business').replaceChildren(...SERVICOS.filter((s) => s.tipo === 'negócio').map(card));
    $('arch-legend').replaceChildren(
        ...GRUPO.map((g) => el('span', { class: `legend c-${g.cor}` }, el('span', { class: 'swatch' }), `${g.equipa} · Elemento ${g.n}`)),
        el('span', { class: 'legend' }, el('span', { class: 'dot up' }), 'responde'),
        el('span', { class: 'legend' }, el('span', { class: 'dot down' }), 'sem resposta'),
    );
}

function renderTeam() {
    $('team').replaceChildren(...GRUPO.map((g) => el('article', { class: `member c-${g.cor}` },
        el('div', { class: 'member-head' },
            el('span', { class: 'avatar', 'aria-hidden': 'true' }, g.n),
            el('div', {}, el('h3', {}, g.equipa), el('p', { class: 'muted' }, `Elemento ${g.n}`))),
        el('ul', { class: 'member-svcs' }, g.servicos.map((id) => {
            const s = servico(id);
            return el('li', {},
                el('a', { href: `servico.html?id=${id}` }, el('span', { class: 'dot', 'data-svc': id }), s.nome),
                el('span', { class: 'muted small' }, s.resumo));
        })),
    )));
}

function renderSaga() {
    $('stepper').replaceChildren(...SAGA.map((step, i) => {
        const g = elementoDe(step.svc);
        return el('li', { class: `step c-${g.cor}` },
            el('span', { class: 'step-n' }, i + 1),
            el('strong', {}, step.label),
            el('span', { class: 'muted small' }, step.who));
    }));

    const tag = (id) => el('a', { class: `tag c-${elementoDe(id).cor}`, href: `servico.html?id=${id}` }, id);
    const rows = SERVICOS.flatMap((s) => s.publica.map(([key, para, nota]) => ({ key, de: s.id, para, nota })));
    $('events').replaceChildren(...rows.map((r) => el('tr', {},
        el('td', {}, el('code', {}, r.key)),
        el('td', {}, tag(r.de)),
        el('td', {}, tag(r.para)),
        el('td', { class: 'muted' }, r.nota),
    )));
}

async function renderLive() {
    try {
        const filas = (await api('GET', '/bastidores/filas')).filter((f) => f.name.endsWith('.fila'));
        $('filas').replaceChildren(...filas.map((f) => {
            const quem = CONSUMIDOR[f.name.split('.')[0]];
            return el('tr', {},
                el('td', {}, el('code', {}, f.name)),
                el('td', {}, quem ? el('a', { class: `tag c-${elementoDe(quem).cor}`, href: `servico.html?id=${quem}` }, quem) : ''),
                el('td', { class: 'num' }, f.messages ?? 0),
                el('td', { class: 'num' }, f.consumers ?? 0));
        }));
    } catch {
        $('filas').replaceChildren(el('tr', {}, el('td', { colspan: 4, class: 'bad-text' }, 'RabbitMQ indisponível')));
    }

    try {
        const stock = await api('GET', `${API}/stock`);
        $('stock').replaceChildren(...stock.map((s) => el('tr', {},
            el('td', {}, el('code', {}, s.referencia)),
            el('td', { class: 'num' }, s.quantidade),
            el('td', { class: 'num' }, s.reservada),
            el('td', { class: `num ${s.livre === 0 ? 'bad-text' : ''}` }, s.livre))));
    } catch {
        $('stock').replaceChildren(el('tr', {}, el('td', { colspan: 4, class: 'bad-text' }, 'bikeshop-stock indisponível')));
    }
}

renderArch();
renderTeam();
renderSaga();
renderLive();
setInterval(renderLive, 3000);
applyStatus();   // os pontos de estado do diagrama só existem agora
