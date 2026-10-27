// tessere obiettivo disegnate in codice, ispirate alle illustrazioni del regolamento ma originali.
// Misure in "s" = raggio di un esagono. Le tessere con una forma (rami, carpa centenaria) sono disegnate attorno
// al baricentro dei loro esagoni a rotazione 0: lo stagno le mette sul baricentro reale e le orienta con orient()
import { el, g, hexagon, uid } from './svg.js';
import { koi, lily, pathStones } from './art.js';

const H = Math.sqrt(3) / 2;   // meta' della distanza tra i centri di due esagoni vicini, in s

const BARK = '#5a381d';
const BARK_LIGHT = '#9a6a3c';
const STONE = '#b4ad9c';
const STONE_DARK = '#6f6a5e';
const BAMBOO_DARK = '#6b7d22';
const LACQUER = '#c8341f';
const LACQUER_DARK = '#7c1c10';
const FOLIAGE = '#7cae3c';
const FOLIAGE_DARK = '#4d7a24';
const WATER = '#5cc2d0';

// tessere viste dall'alto che seguono la loro figura di esagoni: ruotano e si specchiano con la tessera.
// Lo shishi odoshi no: e' un oggetto in piedi, resta dritto sul baricentro del suo triangolo
export function hasShape(type) {
  return ['SMALLBRANCH', 'MEDIUMBRANCH', 'BIGBRANCH', 'OLDCARP'].includes(type);
}

// obiettivi che sono pezzi nuovi di laghetto (piazzati come una sponda), non tessere sopra l'acqua
export function isPondPiece(base) {
  return ['STATUE', 'BRIDGE', 'WATERFALL', 'THREE'].includes(base);
}

// rotazione 0-5, e 6-11 per la forma specchiata: la specchiatura riflette attorno alla direzione 0 (+30 gradi a schermo)
export function orient(rotation) {
  return `rotate(${-60 * (rotation % 6)})` + (rotation >= 6 ? ' rotate(30) scale(1 -1) rotate(-30)' : '');
}

// il disegno della tessera, centrato in (0, 0); "cells" scala le tessere senza una forma fissa
export function objective(type, s, cells = 1) {
  switch (type) {
    case 'SMALLBRANCH': return smallBranch(s);
    case 'MEDIUMBRANCH': return mediumBranch(s);
    case 'BIGBRANCH': return bigBranch(s);
    case 'SHISHIODOSHI': return scaled(shishiOdoshi(s), 2);
    case 'OLDCARP': return oldCarp(s);
    case 'TURTLE': return turtle(s);
    case 'PURPLELILY': return lily(s * 0.62, true);
    case 'PATH': return pathStones(s * 0.8);
    case 'THREE': return scaled(willow(s), cells);
    case 'WATERFALL': return scaled(waterfall(s), cells);
    case 'BRIDGE': return scaled(bridge(s), cells);
    case 'STATUE': return scaled(statue(s), cells);
    default: return g();
  }
}

// l'obiettivo su un esagono d'acqua, per l'elenco degli obiettivi sul tavolo
export function objectiveIcon(type, s) {
  return g({}, hexagon(0, 0, s, { fill: 'url(#water)', stroke: '#257080' }), type ? objective(type, s, 1) : null);
}

function scaled(art, cells) {
  const factor = cells <= 1 ? 1 : Math.sqrt(cells) * 1.1;   // pezzi da 3 esagoni: il disegno occupa tutta la tessera
  return g({ transform: `scale(${factor.toFixed(3)})` }, art);
}

const pts = (s, list) => list.map(([x, y]) => `${(x * s).toFixed(2)},${(y * s).toFixed(2)}`).join(' ');

// ---------------------------------------------------------------- rami

// ramo piccolo: tre esagoni in fila verso l'alto, centrato su quello di mezzo
function smallBranch(s) {
  const group = g({ filter: 'url(#shade)' });
  const w = 0.2;
  limb(group, s, w, 0.15, 2.1 * H, -0.55, 1.2, 0.55, -0.45, -0.05, -2.1 * H);
  twig(group, s, -0.2, 1.0, -165, 0.75, w * 0.45, true);
  twig(group, s, 0.22, -0.15, -15, 0.7, w * 0.45, true);
  twig(group, s, 0.05, -1.15, -150, 0.6, w * 0.4, false);
  fork(group, s, -0.05, -2.1 * H, -90, 0.42, w * 0.35);
  return group;
}

// ramo medio: un nodo al centro e tre rami a 120 gradi, come un palco di cervo
function mediumBranch(s) {
  const group = g({ filter: 'url(#shade)' });
  const w = 0.22;
  limb(group, s, w, 0, 0.1, 0.18, -0.7, -0.22, -1.3, 0.05, -2 * H - 0.15);
  limb(group, s, w, 0, 0, -0.5, 0.15, -0.95, 0.9, -1.65, H + 0.1);
  limb(group, s, w, 0, 0, 0.5, 0.25, 1.0, 0.65, 1.65, H + 0.1);
  knot(group, s, 0, 0, w);
  fork(group, s, 0.05, -2 * H - 0.15, -90, 0.45, w * 0.35);
  fork(group, s, -1.65, H + 0.1, 155, 0.45, w * 0.35);
  fork(group, s, 1.65, H + 0.1, 25, 0.45, w * 0.35);
  twig(group, s, -0.75, 0.45, -120, 0.55, w * 0.4, true);
  twig(group, s, 0.8, 0.45, -60, 0.5, w * 0.4, true);
  return group;
}

// ramo grande: due rami nodosi che si incrociano a X, come la sorgente
function bigBranch(s) {
  const group = g({ filter: 'url(#shade)' });
  const w = 0.25;
  limb(group, s, w, -1.7, H + 0.1, -0.6, 0.7, 0.45, -0.55, 1.7, -H - 0.1);
  limb(group, s, w, -1.7, -H - 0.1, -0.45, -0.35, 0.65, 0.55, 1.7, H + 0.1);
  knot(group, s, 0, 0, w * 1.1);
  fork(group, s, -1.7, H + 0.1, 150, 0.5, w * 0.35);
  fork(group, s, 1.7, -H - 0.1, -30, 0.5, w * 0.35);
  fork(group, s, -1.7, -H - 0.1, -150, 0.5, w * 0.35);
  fork(group, s, 1.7, H + 0.1, 30, 0.5, w * 0.35);
  twig(group, s, 0.75, -0.42, -95, 0.5, w * 0.35, true);
  twig(group, s, -0.8, 0.45, 95, 0.5, w * 0.35, true);
  return group;
}

// ramo principale: curva di Bezier (inizio, controlli, fine) con corteccia scura e un filo di luce
function limb(group, s, width, ...p) {
  const d = curve(s, p);
  group.append(
    el('path', { d, fill: 'none', stroke: BARK, 'stroke-width': width * s, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }),
    el('path', {
      d, fill: 'none', stroke: BARK_LIGHT, 'stroke-width': width * s * 0.3, 'stroke-linecap': 'round',
      transform: `translate(${(-width * s * 0.18).toFixed(2)} ${(-width * s * 0.12).toFixed(2)})`,
    }));
}

// rametto da (x, y) nella direzione indicata (gradi a schermo), con una fogliolina in punta se richiesta
function twig(group, s, x, y, angle, length, width, leaf) {
  const a = angle * Math.PI / 180;
  const ex = x + Math.cos(a) * length;
  const ey = y + Math.sin(a) * length;
  const cx = x + Math.cos(a + 0.35) * length * 0.55;
  const cy = y + Math.sin(a + 0.35) * length * 0.55;
  group.append(el('path', {
    d: `M${(x * s).toFixed(2)} ${(y * s).toFixed(2)} Q${(cx * s).toFixed(2)} ${(cy * s).toFixed(2)} ${(ex * s).toFixed(2)} ${(ey * s).toFixed(2)}`,
    fill: 'none', stroke: BARK, 'stroke-width': width * s, 'stroke-linecap': 'round',
  }));
  if (leaf) {
    group.append(el('ellipse', {
      cx: ex * s, cy: ey * s, rx: 0.13 * s, ry: 0.07 * s, fill: FOLIAGE, stroke: FOLIAGE_DARK,
      transform: `rotate(${angle} ${(ex * s).toFixed(2)} ${(ey * s).toFixed(2)})`,
    }));
  }
}

// punta del ramo che si apre in due
function fork(group, s, x, y, angle, length, width) {
  twig(group, s, x, y, angle - 28, length, width, false);
  twig(group, s, x, y, angle + 28, length * 0.8, width, true);
}

function knot(group, s, x, y, width) {
  group.append(
    el('circle', { cx: x * s, cy: y * s, r: width * s * 0.75, fill: BARK }),
    el('circle', { cx: x * s, cy: y * s, r: width * s * 0.35, fill: 'none', stroke: BARK_LIGHT, 'stroke-width': Math.max(1, s * 0.03) }));
}

function curve(s, p) {
  let d = `M${(p[0] * s).toFixed(2)} ${(p[1] * s).toFixed(2)}`;
  for (let i = 2; i + 5 < p.length; i += 6) {
    d += ` C${p.slice(i, i + 6).map((v) => (v * s).toFixed(2)).join(' ')}`;
  }
  return d;
}

// ---------------------------------------------------------------- shishi odoshi

// canna di bambu' basculante su una vaschetta di pietra; i tre esagoni formano un triangolo
function shishiOdoshi(s) {
  const group = g({ filter: 'url(#shade)' });
  for (const [x, y, r] of [[-0.75, 0.25, 0.3], [-0.4, 0.62, 0.28], [0.1, 0.72, 0.3], [0.6, 0.55, 0.3],
    [0.85, 0.15, 0.26], [-0.6, -0.15, 0.24], [0.7, -0.25, 0.22]]) {
    group.append(el('ellipse', { cx: x * s, cy: y * s, rx: r * s, ry: r * s * 0.72, fill: 'url(#stone)', stroke: STONE_DARK }));
  }
  group.append(el('ellipse', { cx: 0.08 * s, cy: 0.25 * s, rx: 0.6 * s, ry: 0.32 * s, fill: 'url(#water)', stroke: STONE_DARK }));
  for (const x of [-0.38, 0.3]) {
    group.append(bamboo(s, x, -1.0, 0.1, 1.2, false));
  }
  // canna basculante: perno al centro, bocca aperta verso la vaschetta
  group.append(g({ transform: `translate(0 ${(-0.55 * s).toFixed(2)}) rotate(18)` },
    bamboo(s, -0.95, -0.12, 1.9, 0.24, true),
    el('ellipse', { cx: 0.95 * s, cy: 0, rx: 0.06 * s, ry: 0.12 * s, fill: '#4b5718' })));
  group.append(el('circle', { cx: 0, cy: -0.55 * s, r: 0.06 * s, fill: BARK }));
  // cannello che versa acqua nella parte alta della canna
  group.append(g({ transform: `translate(${(-1.15 * s).toFixed(2)} ${(-1.25 * s).toFixed(2)}) rotate(25)` },
    bamboo(s, -0.32, -0.05, 0.64, 0.1, true)));
  group.append(
    el('line', { x1: -0.87 * s, y1: -1.1 * s, x2: -0.86 * s, y2: -0.92 * s, stroke: WATER, 'stroke-width': 0.05 * s, 'stroke-linecap': 'round' }),
    el('circle', { cx: 0.98 * s, cy: 0.05 * s, r: 0.05 * s, fill: WATER }));
  return group;
}

// canna di bambu' dritta con i nodi, orizzontale o verticale
function bamboo(s, x, y, w, h, horizontal) {
  const group = g({}, el('rect', {
    x: x * s, y: y * s, width: w * s, height: h * s, rx: Math.min(w, h) * s * 0.3,
    fill: horizontal ? 'url(#bamboo-h)' : 'url(#bamboo-v)', stroke: BAMBOO_DARK, 'stroke-width': Math.max(0.8, s * 0.02),
  }));
  for (const t of [0.33, 0.67]) {
    group.append(horizontal
      ? el('line', { x1: (x + w * t) * s, y1: y * s, x2: (x + w * t) * s, y2: (y + h) * s, stroke: BAMBOO_DARK, 'stroke-width': Math.max(0.8, s * 0.03) })
      : el('line', { x1: x * s, y1: (y + h * t) * s, x2: (x + w) * s, y2: (y + h * t) * s, stroke: BAMBOO_DARK, 'stroke-width': Math.max(0.8, s * 0.03) }));
  }
  return group;
}

// ---------------------------------------------------------------- carpa centenaria

// carpa farfalla dorata con pinne lunghe e velate: dalla coda (esagoni in basso) alla testa (esagono in alto a destra)
function oldCarp(s) {
  const l = 3.5 * s;
  const p = (x, y) => `${(x * l).toFixed(2)} ${(y * l).toFixed(2)}`;
  const tail = el('path', {
    d: `M${p(-0.22, 0)} C${p(-0.4, -0.05)} ${p(-0.55, -0.32)} ${p(-0.7, -0.26)} C${p(-0.6, -0.12)} ${p(-0.58, -0.02)} ${p(-0.5, 0)}`
      + ` C${p(-0.58, 0.02)} ${p(-0.6, 0.12)} ${p(-0.7, 0.26)} C${p(-0.55, 0.32)} ${p(-0.4, 0.05)} ${p(-0.22, 0)} Z`,
    fill: 'url(#veil)', stroke: 'rgba(217,102,38,0.5)',
  });
  const fins = g();
  for (const side of [-1, 1]) {
    fins.append(el('path', {
      d: `M${p(0.14, side * 0.08)} C${p(0.08, side * 0.2)} ${p(-0.05, side * 0.3)} ${p(-0.16, side * 0.32)}`
        + ` C${p(-0.06, side * 0.22)} ${p(0.02, side * 0.14)} ${p(0.06, side * 0.08)} Z`,
      fill: 'url(#veil)', stroke: 'rgba(217,102,38,0.45)',
    }));
  }
  const barbels = g();
  for (const side of [-1, 1]) {
    barbels.append(el('path', {
      d: `M${p(0.34, side * 0.03)} Q${p(0.4, side * 0.04)} ${p(0.44, side * 0.1)}`,
      fill: 'none', stroke: '#8a5a2b', 'stroke-width': Math.max(1, s * 0.03),
    }));
  }
  const sparkle = g();
  for (const [x, y] of [[0.1, -0.06], [-0.02, -0.07], [0.04, 0.02], [-0.12, 0]]) {
    sparkle.append(el('circle', { cx: x * l, cy: y * l, r: s * 0.05, fill: 'rgba(255,219,102,0.85)' }));
  }
  // asse dalla coda (tra SW e S dell'ancoraggio) alla testa (NE), coordinate dal baricentro
  return g({ transform: `translate(${(0.3 * s).toFixed(2)} ${(-0.05 * s).toFixed(2)}) rotate(-45)`, filter: 'url(#gold-glow)' },
    tail, fins, koi(l * 0.82, 2), barbels, sparkle);
}

// ---------------------------------------------------------------- tartaruga

function turtle(s) {
  const skin = '#86ad55';
  const clip = uid('shell');
  const animal = g({ transform: 'rotate(-25)', filter: 'url(#shade)' });
  for (const [x, y] of [[0.3, -0.3], [0.3, 0.3], [-0.28, -0.3], [-0.28, 0.3]]) {
    animal.append(el('ellipse', { cx: x * s, cy: y * s, rx: 0.13 * s, ry: 0.09 * s, fill: skin, stroke: FOLIAGE_DARK }));
  }
  animal.append(
    el('polygon', { points: pts(s, [[-0.42, -0.05], [-0.6, 0], [-0.42, 0.05]]), fill: skin }),
    el('ellipse', { cx: 0.53 * s, cy: 0, rx: 0.17 * s, ry: 0.13 * s, fill: skin, stroke: FOLIAGE_DARK }),
    el('circle', { cx: 0.6 * s, cy: -0.05 * s, r: Math.max(1, 0.025 * s), fill: '#2b2622' }),
    el('ellipse', { cx: 0, cy: 0, rx: 0.45 * s, ry: 0.35 * s, fill: 'url(#shell)', stroke: '#24451a', 'stroke-width': Math.max(1, s * 0.03) }));
  // scudi del carapace: un esagono al centro e le linee verso il bordo
  const scutes = g({ 'clip-path': `url(#${clip})` }, hexagon(0, 0, 0.15 * s, { fill: 'none', stroke: '#d6e4a0' }));
  for (let corner = 0; corner < 6; corner++) {
    const a = Math.PI / 3 * corner;
    scutes.append(el('line', {
      x1: Math.cos(a) * 0.15 * s, y1: Math.sin(a) * 0.15 * s, x2: Math.cos(a) * 0.45 * s, y2: Math.sin(a) * 0.35 * s,
      stroke: 'rgba(214,228,160,0.8)',
    }));
  }
  animal.append(el('defs', {}, [el('clipPath', { id: clip }, [el('ellipse', { cx: 0, cy: 0, rx: 0.45 * s, ry: 0.35 * s })])]), scutes);
  return g({},
    el('ellipse', { cx: 0, cy: 0.12 * s, rx: 0.8 * s, ry: 0.52 * s, fill: 'url(#stone)', stroke: STONE_DARK }),
    animal);
}

// ---------------------------------------------------------------- salice piangente

function willow(s) {
  const fronds = g();
  for (let i = -6; i <= 6; i++) {
    const x0 = i * 0.1;
    const top = -0.62 + Math.abs(i) * 0.03;
    const bottom = 0.32 - Math.abs(i) * 0.035;
    fronds.append(el('path', {
      d: `M${(x0 * s).toFixed(2)} ${(top * s).toFixed(2)} Q${(x0 * 1.5 * s).toFixed(2)} ${((top + 0.2) * s).toFixed(2)} ${(x0 * 1.2 * s).toFixed(2)} ${(bottom * s).toFixed(2)}`,
      fill: 'none', stroke: i % 2 === 0 ? FOLIAGE : '#a9cf62', 'stroke-width': 0.075 * s, 'stroke-linecap': 'round',
    }));
  }
  return g({ filter: 'url(#shade)' },
    el('ellipse', { cx: 0, cy: 0.58 * s, rx: 0.75 * s, ry: 0.2 * s, fill: '#8fb55a', stroke: FOLIAGE_DARK }),
    el('path', { d: curve(s, [0.05, 0.6, -0.15, 0.3, 0.2, -0.05, -0.05, -0.45]), fill: 'none', stroke: BARK, 'stroke-width': 0.17 * s, 'stroke-linecap': 'round' }),
    el('ellipse', { cx: 0, cy: -0.5 * s, rx: 0.62 * s, ry: 0.24 * s, fill: FOLIAGE_DARK }),
    fronds);
}

// ---------------------------------------------------------------- cascata

function waterfall(s) {
  const group = g({ filter: 'url(#shade)' },
    el('polygon', {
      points: pts(s, [[-0.82, 0.32], [-0.78, -0.5], [-0.42, -0.8], [0.02, -0.68], [0.4, -0.86], [0.8, -0.5], [0.84, 0.32]]),
      fill: 'url(#stone)', stroke: STONE_DARK,
    }));
  for (const [x, y] of [[-0.55, -0.62], [-0.2, -0.72], [0.45, -0.72], [0.65, -0.45]]) {
    group.append(el('ellipse', { cx: x * s, cy: y * s, rx: 0.16 * s, ry: 0.07 * s, fill: FOLIAGE }));
  }
  group.append(el('rect', { x: -0.24 * s, y: -0.66 * s, width: 0.48 * s, height: 0.92 * s, rx: 0.06 * s, fill: 'url(#fall)' }));
  for (const x of [-0.14, -0.03, 0.08, 0.17]) {
    group.append(el('line', { x1: x * s, y1: -0.6 * s, x2: x * s, y2: 0.2 * s, stroke: 'rgba(255,255,255,0.7)', 'stroke-width': Math.max(1, s * 0.03) }));
  }
  group.append(el('ellipse', { cx: 0, cy: 0.42 * s, rx: 0.72 * s, ry: 0.22 * s, fill: 'url(#water)', stroke: STONE_DARK }));
  for (const [x, y] of [[-0.28, 0.3], [-0.1, 0.36], [0.1, 0.33], [0.26, 0.3], [0, 0.26]]) {
    group.append(el('circle', { cx: x * s, cy: y * s, r: 0.08 * s, fill: 'rgba(255,255,255,0.85)' }));
  }
  group.append(
    el('ellipse', { cx: -0.62 * s, cy: 0.45 * s, rx: 0.22 * s, ry: 0.15 * s, fill: STONE, stroke: STONE_DARK }),
    el('ellipse', { cx: 0.6 * s, cy: 0.48 * s, rx: 0.2 * s, ry: 0.13 * s, fill: STONE, stroke: STONE_DARK }));
  return group;
}

// ---------------------------------------------------------------- ponticello

// punto della curva quadratica a, controllo c, b al parametro u
const quad = (a, c, b, u) => (1 - u) * (1 - u) * a + 2 * u * (1 - u) * c + u * u * b;

// ponte ad arco laccato di rosso, con ringhiera e pomelli dorati
function bridge(s) {
  const ax = -0.95;
  const bx = 0.95;
  const cy = -0.6;
  const p = (x, y) => `${(x * s).toFixed(2)} ${(y * s).toFixed(2)}`;
  const posts = g();
  for (let i = 0; i <= 4; i++) {
    const u = 0.06 + i * 0.22;
    const deckY = quad(0.2, cy, 0.2, u);
    const railY = quad(-0.08, -0.98, -0.08, u);
    const x = quad(-0.9, 0, 0.9, u);
    posts.append(el('line', { x1: x * s, y1: deckY * s, x2: x * s, y2: railY * s, stroke: LACQUER_DARK, 'stroke-width': 0.06 * s }));
    if (i === 0 || i === 4) {
      posts.append(el('circle', { cx: x * s, cy: (railY - 0.06) * s, r: 0.065 * s, fill: '#d4a13a' }));
    }
  }
  return g({ filter: 'url(#shade)' },
    el('ellipse', { cx: 0, cy: 0.48 * s, rx: 0.6 * s, ry: 0.07 * s, fill: 'rgba(199,51,31,0.25)' }),
    el('rect', { x: -1.05 * s, y: 0.2 * s, width: 0.22 * s, height: 0.2 * s, fill: STONE, stroke: STONE_DARK }),
    el('rect', { x: 0.83 * s, y: 0.2 * s, width: 0.22 * s, height: 0.2 * s, fill: STONE, stroke: STONE_DARK }),
    el('path', {
      d: `M${p(ax, 0.2)} Q${p(0, cy)} ${p(bx, 0.2)} L${p(bx, 0.34)} Q${p(0, cy + 0.16)} ${p(ax, 0.34)} Z`,
      fill: 'url(#deck)', stroke: LACQUER_DARK,
    }),
    posts,
    el('path', { d: `M${p(-0.88, -0.08)} Q${p(0, -0.98)} ${p(0.88, -0.08)}`, fill: 'none', stroke: LACQUER, 'stroke-width': 0.08 * s, 'stroke-linecap': 'round' }));
}

// ---------------------------------------------------------------- statua

// statua di pietra seduta su un fiore di loto, con un'aureola appena dorata
function statue(s) {
  const stone = '#a2a89e';
  const stoneDark = '#666c63';
  const p = (x, y) => `${(x * s).toFixed(2)} ${(y * s).toFixed(2)}`;
  const lotus = g();
  for (let i = -3; i <= 3; i++) {
    lotus.append(el('ellipse', { cx: i * 0.16 * s, cy: 0.52 * s, rx: 0.12 * s, ry: 0.1 * s, fill: '#c9cfc4', stroke: stoneDark }));
  }
  const eyes = g();
  for (const side of [-1, 1]) {
    eyes.append(el('path', {
      d: `M${p(side * 0.1, -0.37)} Q${p(side * 0.06, -0.33)} ${p(side * 0.02, -0.37)}`,
      fill: 'none', stroke: stoneDark, 'stroke-width': Math.max(0.8, s * 0.02),
    }));
  }
  return g({ filter: 'url(#shade)' },
    el('circle', { cx: 0, cy: -0.36 * s, r: 0.36 * s, fill: 'url(#halo)' }),
    el('ellipse', { cx: 0, cy: 0.6 * s, rx: 0.62 * s, ry: 0.11 * s, fill: '#71756e', stroke: stoneDark }),
    lotus,
    el('path', {
      d: `M${p(-0.46, 0.42)} C${p(-0.48, 0.05)} ${p(-0.3, -0.12)} ${p(-0.16, -0.17)} L${p(0.16, -0.17)} C${p(0.3, -0.12)} ${p(0.48, 0.05)} ${p(0.46, 0.42)} Z`,
      fill: 'url(#stone)', stroke: stoneDark,
    }),
    el('ellipse', { cx: 0, cy: 0.38 * s, rx: 0.5 * s, ry: 0.12 * s, fill: stone, stroke: stoneDark }),
    el('ellipse', { cx: 0, cy: 0.27 * s, rx: 0.13 * s, ry: 0.065 * s, fill: '#c6cdc2', stroke: stoneDark }),
    el('circle', { cx: 0, cy: -0.36 * s, r: 0.17 * s, fill: 'url(#stone)', stroke: stoneDark }),
    el('circle', { cx: 0, cy: -0.55 * s, r: 0.07 * s, fill: stone, stroke: stoneDark }),
    eyes);
}
