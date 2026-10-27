// i pannelli della stanza: tabellone, plancia del giocatore, obiettivi, avversari, classifica
import { el as svgEl, fitted, hexagon } from './svg.js';
import { C, badgeIcon, seal, tileIcon } from './art.js';
import { objectiveIcon } from './objective-art.js';

// elemento HTML con classe e figli (testo o nodi)
export function h(tag, className, ...children) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  for (const child of children) {
    if (child !== null && child !== undefined && child !== false) {
      node.append(child);
    }
  }
  return node;
}

export const capitalize = (text) => text ? text.charAt(0).toUpperCase() + text.slice(1) : text;

// ---------------------------------------------------------------- tabellone

// stessi effetti di Board.drawEffect (posizioni 0-4)
const SLOT_EFFECT = ['paga 1 moneta', 'gratis', '+2 monete  +1 acqua', '1 moneta + 2 acqua\no mercato carpe', '2 sponde\no 2 fogliami'];

const CARD_LOOK = {
  Aiutante: ['助', C.tealDeep],
  Magazzino: ['蔵', C.wood],
  Mercato: ['市', C.gold],
  Pioggia: ['雨', '#4a78b5'],
  Pozzo: ['井', C.stoneDark],
  Pergamena: ['巻', C.vermilion],
};
const lookOf = (type) => CARD_LOOK[type] || ['札', C.ink];

// le descrizioni arrivano gia' in italiano dal server: si toglie solo la parte degli usi
// ("piazza una carpa o sposta una carpa | usi: 1/1") e si va a capo prima di ogni alternativa
export const prettify = (body) => body.replace(/\s*\|\s*usi:.*/, '').replaceAll(' o ', '\no ');

// "[acqua, acqua, fogliame]" -> "2 acqua + 1 fogliame, piazzate subito"
function rainTiles(body) {
  const counts = new Map();
  for (const tile of body.replace(/[[\]]/g, '').split(',')) {
    if (tile.trim()) {
      const name = tile.trim().toLowerCase();
      counts.set(name, (counts.get(name) || 0) + 1);
    }
  }
  return [...counts].map(([name, count]) => `${count} ${name}`).join(' + ') + '\npiazzate subito';
}

export class Board {

  constructor(container, onDraw) {
    this.container = container;
    this.onDraw = onDraw;
    this.previous = [];
  }

  update(board, canDraw) {
    const slots = board.map((description, position) => {
      const slot = h('div', 'slot', this.card(description, position, canDraw),
        h('div', 'slot-effect' + (position === 0 ? ' pay' : ''), SLOT_EFFECT[position] || ''));
      if (this.previous.length && this.previous[position] !== description) {
        slot.classList.add('slide');
        slot.style.animationDelay = `${position * 60}ms`;
      }
      return slot;
    });
    this.container.replaceChildren(...slots);
    this.previous = board;
  }

  card(description, position, canDraw) {
    if (!description) {
      return h('div', 'card empty', 'vuoto');   // mazzo finito: lo spazio resta vuoto
    }
    const colon = description.indexOf(':');
    const type = colon >= 0 ? description.slice(0, colon).trim() : description;
    const body = colon >= 0 ? description.slice(colon + 1).trim() : '';
    const [kanji, color] = lookOf(type);

    const band = h('div', 'band', seal(kanji, 30, color), h('span', 'card-name' + (type.length > 7 ? ' long' : ''), type.toUpperCase()));
    band.style.background = color + '40';
    const card = h(canDraw ? 'button' : 'div', 'card' + (canDraw ? ' drawable' : ''),
      band,
      h('div', 'card-text', type === 'Pioggia' ? rainTiles(body) : prettify(body)),
      h('div', 'card-number', String(position)));
    if (canDraw) {
      card.title = 'Prendi questa carta';
      card.addEventListener('click', () => this.onDraw(position));
    }
    return card;
  }
}

// ---------------------------------------------------------------- plancia del giocatore

function sectionTitle(text) {
  return h('h3', 'panel-title', text);
}

function count(kind, value, label) {
  const box = h('span', 'count', badgeIcon(kind, 11), `× ${value}`);
  box.title = `${value} ${label}`;
  return box;
}

// il magazzino come sulla plancia: esagoni di legno, scuri quelli occupati;
// oltre la capienza (possibile durante il turno) quelli in piu' sono vermigli: a fine turno si scarta
function storageSlots(used, max) {
  const slots = h('div', 'storage');
  for (let i = 0; i < Math.max(used, max); i++) {
    const fill = i >= max ? C.vermilion : i < used ? C.woodDark : 'rgba(186,147,104,0.6)';
    slots.append(svgEl('svg', { viewBox: '-12 -11 24 22', 'aria-hidden': 'true' }, [hexagon(0, 0, 11, { fill, stroke: C.woodDark })]));
  }
  return slots;
}

function cardLine(kanji, text, color) {
  return h('div', 'card-line', seal(kanji, 22, color), h('span', null, prettify(text)));
}

// placeable: i tipi che ora si possono piazzare (dalle posizioni valide della view); selected: quello scelto
export function renderPlayerPanel(container, me, placeable, selected, { onSelect, onSpendLucky, canSpendLucky, nameOf }) {
  const counts = new Map();
  for (const tile of me.tiles) {
    counts.set(tile, (counts.get(tile) || 0) + 1);
  }
  for (const upgrade of ['LILY', 'LANTERN']) {
    if (placeable.has(upgrade) && !counts.has(upgrade)) {
      counts.set(upgrade, 0);   // 0 = dalla riserva
    }
  }

  const tokens = h('div', 'tokens');
  if (!counts.size) {
    tokens.append(h('span', 'muted', 'vuoto'));
  }
  for (const [type, number] of counts) {
    const canPlace = placeable.has(type);
    const token = h('button', 'token' + (canPlace ? ' placeable' : '') + (type === selected ? ' selected' : ''),
      fitted(tileIcon(type, 21), 3), number === 0 ? 'riserva' : `× ${number}`);
    token.title = capitalize(nameOf(type)) + (canPlace ? '  (clicca, poi clicca un esagono evidenziato)' : '');
    token.disabled = !canPlace;
    if (canPlace) {
      token.addEventListener('click', () => onSelect(type));
    }
    tokens.append(token);
  }

  // segnalini Fortuna: un pulsante per spenderne uno (le opzioni arrivano come domanda dal server)
  let spend = null;
  if (me.luckyCards > 0) {
    spend = h('button', 'ghost small fortune-button', badgeIcon('fortune', 8), 'Usa Fortuna');
    spend.title = 'Spendi un segnalino Fortuna: 1 fogliame, 2 acqua, 1 sponda o una carpa a +1 moneta';
    spend.disabled = !canSpendLucky;
    spend.addEventListener('click', onSpendLucky);
  }

  const parts = [
    h('div', 'me-name', me.name),
    h('div', 'wealth', count('coin', me.coins, 'monete'), count('fortune', me.luckyCards, 'segnalini Fortuna'), spend),
    sectionTitle(`Magazzino  ${me.storageUsed}/${me.storageMax}`),
    storageSlots(me.storageUsed, me.storageMax),
    tokens,
  ];
  if (me.helpers.length) {
    parts.push(sectionTitle('Aiutanti'));
    for (const helper of me.helpers) {
      // "Aiutante: piazza fino a 2 acque | usi: 1/2" -> "piazza fino a 2 acque   (1/2 in questo turno)"
      const uses = helper.includes('usi:') ? helper.slice(helper.indexOf('usi:') + 4).trim() : '';
      const text = prettify(helper.replace('Aiutante:', '').trim()).replaceAll('\n', ' ');
      parts.push(cardLine('助', text + (uses ? `   (${uses} in questo turno)` : ''), C.tealDeep));
    }
  }
  if (me.scrolls.length) {
    parts.push(sectionTitle('Pergamene'));
    for (const scroll of me.scrolls) {
      parts.push(cardLine('巻', scroll.replace('Pergamena:', '').trim(), C.vermilion));
    }
  }
  if (me.cards.length) {
    parts.push(sectionTitle('Carte'));
    for (const card of me.cards) {
      parts.push(cardLine('札', card, C.inkSoft));
    }
  }
  container.replaceChildren(...parts);
}

// riga compatta per gli avversari, con le stesse icone
export function summary(player) {
  return h('div', null,
    h('div', 'summary-name', player.name),
    h('div', 'summary-stats',
      h('span', 'count', badgeIcon('coin', 7), `× ${player.coins}`),
      h('span', 'count', badgeIcon('fortune', 7), `× ${player.luckyCards}`),
      h('span', null, `magazzino ${player.storageUsed}/${player.storageMax}`)));
}

// ---------------------------------------------------------------- obiettivi

// i tre obiettivi sul tavolo: disegno, nome, costo, requisito e tessere rimaste;
// bordo dorato e "Reclamabile" quando chi guarda lo puo' reclamare a fine turno
export function renderObjectives(container, objectives) {
  if (!objectives.length) {
    container.replaceChildren(h('p', 'muted', "Si scoprono all'inizio della partita"));
    return;
  }
  container.replaceChildren(...objectives.map((objective) => {
    const title = h('div', 'objective-title', objective.name);
    if (objective.cost > 0) {
      title.append(badgeIcon('coin', 7), String(objective.cost));
    }
    title.append(h('span', 'spacer'));
    if (objective.claimable) {
      title.append(h('span', 'badge', 'Reclamabile'));
    }
    const left = objective.tilesLeft === 0 ? 'Finite'
      : `${objective.tilesLeft} ${objective.tilesLeft === 1 ? 'tessera rimasta' : 'tessere rimaste'}`;
    const card = h('div', 'objective' + (objective.claimable ? ' claimable' : '') + (objective.tilesLeft === 0 ? ' finished' : ''),
      fitted(objectiveIcon(objective.tileType, 23), 1),
      h('div', null, title, h('small', null, objective.requirement), h('small', null, left)));
    card.title = `${objective.name}\n${objective.requirement}`;
    return card;
  }));
}

// ---------------------------------------------------------------- classifica

const COLUMNS = ['', 'Giocatore', 'Carpe', 'Lanterne', 'Pergamene', 'Obiettivi', 'Fortuna', 'Totale', 'Monete'];

// la tabella dei punti, una riga per giocatore (come il blocchetto segnapunti); la usa anche lo storico
export function scoreTable(ranking, viewer) {
  const table = h('table', 'score-table',
    h('thead', null, h('tr', null, ...COLUMNS.map((column) => h('th', null, column)))));
  const body = h('tbody');
  ranking.forEach((sheet, i) => {
    const values = [`${i + 1}.`, sheet.name, sheet.koi, sheet.lanterns, sheet.scrolls, sheet.objectives, sheet.fortune, sheet.total, sheet.coins];
    const row = h('tr', (i === 0 ? 'winner ' : '') + (sheet.name === viewer ? 'me' : ''),
      ...values.map((value, c) => h('td', c === 7 ? 'total' : null, String(value))));
    body.append(row);
  });
  table.append(body);
  return h('div', 'table-scroll', table);
}
