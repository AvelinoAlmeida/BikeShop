'use strict';

// Núcleo partilhado pelas três páginas: chamadas à API, cabeçalho e navegação, rodapé e estado dos serviços.
// Todas as chamadas passam pela bikeshop-gateway (mesma origem, sem CORS). As respostas são o objeto
// direto; os erros vêm como { servico, estado, mensagem }. Os dados das páginas estão em servicos.js.

const API = '/api';
const STATUS_MS = 10000;

const euros = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' });
const $ = (id) => document.getElementById(id);

const servico = (id) => SERVICOS.find((s) => s.id === id);
const elementoDe = (id) => GRUPO.find((g) => g.servicos.includes(id));

// ---------- utilitários ----------

function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) {
        if (value === false || value === null || value === undefined) continue;
        if (key === 'class') node.className = value;
        else if (key === 'html') node.innerHTML = value;   // só para SVG e texto fixo deste ficheiro
        else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
        else node.setAttribute(key, value === true ? '' : value);
    }
    for (const child of children.flat()) {
        if (child === null || child === undefined || child === false) continue;
        node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return node;
}

async function lerCorpo(response) {
    const texto = await response.text();
    if (!texto) return null;
    try { return JSON.parse(texto); } catch { return texto; }
}

// Pedido à gateway (caminho completo, ex. /api/pecas): devolve o corpo ou lança { status, mensagem, servico }.
async function api(method, path, body) {
    let response;
    try {
        response = await fetch(path, {
            method,
            headers: body ? { 'Content-Type': 'application/json' } : {},
            body: body ? JSON.stringify(body) : undefined,
            cache: 'no-store',
        });
    } catch {
        throw { status: 0, mensagem: 'A bikeshop-gateway não respondeu (porta 9080).' };
    }
    const corpo = await lerCorpo(response);
    if (!response.ok) {
        throw { status: response.status, servico: corpo?.servico, mensagem: corpo?.mensagem || corpo?.error || `Erro ${response.status} em ${path}` };
    }
    return corpo;
}

// Para os painéis "Experimentar": nunca lança e devolve também o tempo e o corpo completo da resposta.
async function callApi(method, path, body) {
    const start = performance.now();
    let status = 0;
    let json = null;
    try {
        const response = await fetch(API + path, {
            method,
            headers: body ? { 'Content-Type': 'application/json' } : {},
            body: body ? JSON.stringify(body) : undefined,
            cache: 'no-store',
        });
        status = response.status;
        json = await lerCorpo(response);
    } catch {
        json = { mensagem: 'A bikeshop-gateway não respondeu (porta 9080).' };
    }
    return { method, path, body, status, ok: status >= 200 && status < 300, ms: Math.round(performance.now() - start), json };
}

// ---------- avisos ----------

function showError(err, context) {
    const banner = $('error');
    banner.replaceChildren(
        el('strong', {}, `${context}: ${err.status ? `HTTP ${err.status}` : 'sem ligação'}${err.servico ? ` · ${err.servico}` : ''}`),
        err.mensagem || '');
    banner.hidden = false;
}

function hideError() {
    $('error').hidden = true;
}

let noticeTimer = null;

function showNotice(message) {
    const notice = $('notice');
    notice.textContent = message;
    notice.hidden = false;
    clearTimeout(noticeTimer);
    noticeTimer = setTimeout(() => { notice.hidden = true; }, 3500);
}

// ---------- tema ----------

const THEME_KEY = 'bikeshop.theme';

function currentTheme() {
    return document.documentElement.dataset.theme
        || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
}

function toggleTheme() {
    const next = currentTheme() === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    try { localStorage.setItem(THEME_KEY, next); } catch { /* sem armazenamento */ }
    renderThemeButton();
}

function renderThemeButton() {
    const button = $('theme-toggle');
    if (!button) return;
    const dark = currentTheme() === 'dark';
    button.textContent = dark ? '☀' : '☾';
    button.setAttribute('aria-label', dark ? 'Mudar para tema claro' : 'Mudar para tema escuro');
    button.title = button.getAttribute('aria-label');
}

// ---------- logótipo ----------

function logo(cls = 'logo') {
    return el('img', { class: cls, src: 'img/logo.svg', alt: '' });
}

function wordmark() {
    return el('span', { class: 'wordmark' }, 'Bike', el('span', {}, 'Shop'));
}

// ---------- cabeçalho e navegação ----------

const CART_SVG = `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M3 4h2.2l2.1 10.2a2 2 0 0 0 2 1.6h7.6a2 2 0 0 0 1.9-1.4L21 8H6.3" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="20" r="1.6" fill="currentColor"/><circle cx="17" cy="20" r="1.6" fill="currentColor"/></svg>`;

const TOOL_SVG = `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94z" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

const SERVER_SVG = `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="7" rx="1.5"/><rect x="4" y="14" width="16" height="7" rx="1.5"/><path d="M8 6.5h.01M8 17.5h.01"/></svg>`;

// Cabeçalho: logótipo (leva à visão geral), botão da loja, Ferramentas e, à direita, o estado dos serviços por
// equipa e o tema.
function renderHeader() {
    const page = document.body.dataset.page;

    const header = el('header', { class: 'site-header' },
        el('div', { class: 'header-inner' },
            el('a', { class: 'brand', href: 'index.html', 'aria-label': 'BikeShop: visão geral', 'aria-current': page === 'inicio' ? 'page' : false },
                logo(), wordmark()),
            el('a', { class: 'shop-btn', href: 'loja.html', 'aria-current': page === 'loja' ? 'page' : false },
                el('span', { class: 'shop-icon', html: CART_SVG }), el('span', { class: 'shop-label' }, 'Loja')),
            renderTools(),
            el('div', { class: 'header-tools' },
                GRUPO.map(renderTeam),
                el('button', { class: 'icon-btn', id: 'theme-toggle', type: 'button', onclick: toggleTheme }),
            ),
        ),
    );

    document.body.prepend(
        el('a', { class: 'skip', href: '#conteudo' }, 'Saltar para o conteúdo'),
        header,
        el('div', { id: 'error', class: 'banner', role: 'alert', hidden: true, onclick: hideError }),
        el('div', { id: 'notice', class: 'banner notice', role: 'status', hidden: true }),
    );
    renderThemeButton();
}

function renderFooter() {
    document.body.append(el('footer', { class: 'site-footer' },
        el('div', { class: 'footer-inner' },
            el('div', { class: 'footer-brand' },
                logo('logo small'),
                el('div', {},
                    el('strong', {}, 'BikeShop'),
                    el('p', {}, 'Loja de peças para bicicletas elétricas (2026). Projeto académico de MTSDS · ESTG, Politécnico do Porto: microsserviços com Spring Boot e Spring Cloud. Pagamentos simulados.'))),
            el('div', { class: 'footer-links' },
                el('span', { class: 'footer-head' }, 'Grupo'),
                GRUPO.map((g) => el('span', { class: `member-line c-${g.cor}` }, el('span', { class: 'swatch' }), `${g.nome} (${g.equipa}): ${g.servicos.join(', ')}`))),
        ),
        el('p', { class: 'footer-note' }, 'Todos os pedidos da página passam pela bikeshop-gateway, por ', el('code', {}, '/api'), '.'),
    ));
}

// Ferramentas externas: botão no cabeçalho com um cartão por ferramenta (<details> nativo, fecha ao clicar fora
// ou com Esc). Ao abrir verifica se cada ferramenta responde (pedido no-cors: só interessa se lá chega).
function renderTools() {
    const icon = (t) => el('span', { class: 'tool-icon', html: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${t.icon}</svg>` });
    const title = (t) => el('span', { class: 'tool-title' }, t.nome, el('span', { class: 'tool-state', 'data-tool': t.id }, 'a verificar…'));
    // Ferramenta com um endereço por serviço: o cartão não é um link; cada serviço é um botão.
    const multi = (t) => el('li', {},
        el('div', { class: 'tool-card multi', style: `--tc: ${t.cor}` },
            icon(t),
            el('span', { class: 'tool-body' },
                title(t),
                el('span', { class: 'tool-desc' }, t.faz),
                el('span', { class: 'tool-links' }, t.links.map((l) => el('a', { href: l.url, target: '_blank', rel: 'noopener', title: l.url, 'data-link': l.url },
                    el('span', { class: 'dot' }), l.nome))),
                el('span', { class: 'tool-meta' }, t.nota))));
    const card = (t) => t.links ? multi(t) : el('li', {},
        el('a', { class: 'tool-card', href: t.url, target: '_blank', rel: 'noopener', style: `--tc: ${t.cor}` },
            icon(t),
            el('span', { class: 'tool-body' },
                title(t),
                el('span', { class: 'tool-desc' }, t.faz),
                el('span', { class: 'tool-meta' }, el('code', {}, t.url.replace('http://', '')), ` · ${t.nota}`)),
            el('span', { class: 'tool-go', 'aria-hidden': 'true' }, '↗')));
    return headerMenu('tools-main', [el('span', { class: 'shop-icon', html: TOOL_SVG }), el('span', { class: 'shop-label' }, 'Ferramentas')],
        'Ferramentas externas',
        [el('p', { class: 'tools-head' }, 'Ferramentas', el('span', {}, 'abrem num separador novo')), el('ul', {}, FERRAMENTAS.map(card))],
        checkTools);
}

// Botão de uma equipa: ponto com o estado do grupo e, ao abrir, um cartão por microsserviço.
function renderTeam(g) {
    const card = (id) => {
        const s = servico(id);
        return el('li', {},
            el('div', { class: 'tool-card multi' },
                el('span', { class: 'tool-icon', html: SERVER_SVG }),
                el('span', { class: 'tool-body' },
                    el('span', { class: 'tool-title' }, s.nome, el('span', { class: 'tool-state', 'data-svc-text': id }, 'a verificar…')),
                    el('span', { class: 'tool-desc' }, s.resumo),
                    el('span', { class: 'tool-links' },
                        el('a', { href: `servico.html?id=${id}` }, el('span', { class: 'dot', 'data-svc': id }), 'Ficha')),
                    el('span', { class: 'tool-meta' }, el('code', {}, `localhost:${s.porta}`)))));
    };
    return headerMenu(`team-btn c-${g.cor}`, [el('span', { class: 'dot', 'data-team': g.cor }), el('span', { class: 'team-label' }, g.equipa)],
        `${g.equipa}: microsserviços`,
        [el('p', { class: 'tools-head' }, g.equipa, el('span', {}, `${g.servicos.length} microsserviço${g.servicos.length > 1 ? 's' : ''}`)),
            el('ul', {}, g.servicos.map(card))]);
}

// Menu do cabeçalho (<details> nativo): só um aberto de cada vez; fecha ao clicar fora ou com Esc.
function headerMenu(cls, summary, label, panel, onOpen) {
    const menu = el('details', { class: `tools-menu ${cls}`.trim() },
        el('summary', { class: 'tools-btn', 'aria-label': label }, summary),
        el('div', { class: 'tools-panel' }, panel));
    menu.addEventListener('toggle', () => {
        if (!menu.open) return;
        document.querySelectorAll('details.tools-menu[open]').forEach((m) => { if (m !== menu) m.open = false; });
        if (onOpen) onOpen();
    });
    return menu;
}

document.addEventListener('click', (e) => {
    document.querySelectorAll('details.tools-menu[open]').forEach((m) => { if (!m.contains(e.target)) m.open = false; });
});
document.addEventListener('keydown', (e) => {
    if (e.key !== 'Escape') return;
    document.querySelectorAll('details.tools-menu[open]').forEach((m) => { m.open = false; m.querySelector('summary').focus(); });
});

async function reachable(url) {
    try {
        await fetch(url, { mode: 'no-cors', cache: 'no-store', signal: AbortSignal.timeout(3000) });
        return true;
    } catch {
        return false;   // parada ou sem resposta
    }
}

async function checkTools() {
    await Promise.all(FERRAMENTAS.map(async (t) => {
        const badge = document.querySelector(`.tool-state[data-tool="${t.id}"]`);
        let up;
        if (t.links) {
            const results = await Promise.all(t.links.map(async (l) => {
                let ok = false;
                try { ok = (await fetch(l.url, { cache: 'no-store', signal: AbortSignal.timeout(3000) })).ok; } catch { /* em baixo */ }
                document.querySelector(`[data-link="${l.url}"] .dot`).className = `dot ${ok ? 'up' : 'down'}`;
                return ok;
            }));
            const down = results.filter((ok) => !ok).length;
            up = down === 0;
            badge.textContent = up ? 'a funcionar' : `${down} parado${down > 1 ? 's' : ''}`;
        } else {
            up = await reachable(t.url);
            badge.textContent = up ? 'a funcionar' : 'parada';
        }
        badge.className = `tool-state ${up ? 'up' : 'down'}`;
    }));
}

// ---------- estado dos serviços ----------

// Cada serviço de negócio responde ao /landing pela gateway; um 502, 503 ou 504 vem da gateway quando não há
// instância. O Eureka lê-se pela rota /bastidores/registo; a gateway está a funcionar se responder a algum pedido.
const serviceStatus = {};

async function probe(url, options) {
    try {
        const r = await fetch(url, { cache: 'no-store', ...options });
        return { reached: true, up: r.ok };
    } catch {
        return { reached: false, up: false };
    }
}

async function refreshStatus() {
    const business = SERVICOS.filter((s) => s.probe);
    const [eureka, ...results] = await Promise.all([
        probe('/bastidores/registo'),
        ...business.map((s) => probe(API + s.probe)),
    ]);
    business.forEach((s, i) => { serviceStatus[s.id] = results[i].up; });
    serviceStatus.registo = eureka.up;
    serviceStatus.gateway = eureka.reached || results.some((r) => r.reached);
    applyStatus();
}

// Pinta o estado conhecido em todos os elementos com data-svc; as páginas chamam-na depois de desenharem.
function applyStatus() {
    if (!Object.keys(serviceStatus).length) return;
    document.querySelectorAll('[data-team]').forEach((dot) => {
        const g = GRUPO.find((x) => x.cor === dot.dataset.team);
        const down = g.servicos.filter((id) => serviceStatus[id] === false).length;
        dot.classList.toggle('up', down === 0);
        dot.classList.toggle('down', down > 0);
        dot.closest('summary').title = down ? `${g.equipa}: ${down} sem resposta` : `${g.equipa}: tudo a funcionar`;
    });
    document.querySelectorAll('[data-svc-text]').forEach((badge) => {
        const v = serviceStatus[badge.dataset.svcText];
        badge.textContent = v ? 'a funcionar' : 'sem resposta';
        badge.className = `tool-state ${v ? 'up' : 'down'}`;
    });
    document.querySelectorAll('[data-svc]').forEach((node) => {
        const v = serviceStatus[node.dataset.svc];
        node.classList.toggle('up', v === true);
        node.classList.toggle('down', v === false);
    });
}

// ---------- arranque comum ----------

renderHeader();
renderFooter();
refreshStatus();
setInterval(refreshStatus, STATUS_MS);
