// disegna uno stagno in SVG: esagoni a punta piatta, coordinate assiali (q, r) come in HexPosition.
// Animazioni: increspature sull'acqua, carpe che nuotano, lanterne che tremolano, tessere nuove che "sbocciano".
// Lo stagno si adatta da solo allo spazio (viewBox); quello del giocatore si ingrandisce e si sposta
import { el, g, hexagon, tooltip, SQRT3 } from './svg.js';
import { C, floatingLeaves, grassTufts, koi, lantern, lily, waves } from './art.js';
import { hasShape, isPondPiece, objective, orient } from './objective-art.js';

// stesso ordine di HexPosition.DIRECTIONS: 0 = SE, 1 = NE, 2 = N, 3 = NW, 4 = SW, 5 = S
export const DQ = [1, 1, 0, -1, -1, 0];
export const DR = [0, -1, -1, 0, 1, 1];

const SIZE = 40;                 // raggio di un esagono, in unita' del disegno
const key = (q, r) => `${q},${r}`;
const centerX = (q) => SIZE * 1.5 * q;
const centerY = (q, r) => SIZE * SQRT3 * (r + q / 2);

export class Pond {

  // interactive: zoom, spostamento e clic (lo stagno del giocatore); animated: increspature, nuoto, sboccio;
  // maxScale: quanti pixel al massimo per unita' del disegno, perche' uno stagno piccolo non diventi enorme
  constructor({ interactive = false, animated = false, maxScale = 1.7, nameOf = (type) => type } = {}) {
    this.interactive = interactive;
    this.animated = animated;
    this.maxScale = maxScale;
    this.nameOf = nameOf;
    this.tiles = g();
    this.effects = g({ 'pointer-events': 'none' });
    this.tops = g();
    this.spots = g();
    this.ghost = g({ 'pointer-events': 'none' });
    this.svg = el('svg', { class: 'pond' + (interactive ? ' interactive' : ''), preserveAspectRatio: 'xMidYMid meet' },
      [this.tiles, this.effects, this.tops, this.spots, this.ghost]);

    this.onHexClicked = () => { };
    this.onKoiClicked = () => { };
    this.ghostShape = (coord) => [coord];
    this.movableKoi = new Set();
    this.selectedKoi = -1;
    this.previousBases = null;
    this.previousTops = null;
    this.waterCentres = [];
    this.hovered = null;
    this.bounds = { x: -SIZE, y: -SIZE, w: SIZE * 2, h: SIZE * 2 };
    this.zoom = 1;
    this.panX = 0;
    this.panY = 0;
    this.justDragged = false;

    new ResizeObserver(() => this.layout()).observe(this.svg);
    if (interactive) {
      this.enableZoomAndPan();
    }
    if (animated) {
      this.ripples = setInterval(() => this.spawnRipple(), 900);
    }
  }

  // da chiamare prima di show: quali carpe si possono cliccare per spostarle e quale e' scelta (-1 nessuna)
  setMovableKoi(ids, selected) {
    this.movableKoi = new Set(ids);
    this.selectedKoi = selected;
  }

  show(cells, highlighted = []) {
    this.tiles.replaceChildren();
    this.tops.replaceChildren();
    this.spots.replaceChildren();
    this.ghost.replaceChildren();
    this.hovered = null;
    this.armed = false;
    this.waterCentres = [];

    const byPosition = new Map();
    const bases = new Set();
    const topKeys = new Set();
    for (const cell of cells) {
      byPosition.set(key(cell.q, cell.r), cell);
      bases.add(key(cell.q, cell.r) + cell.base);
      if (cell.top) {
        topKeys.add(key(cell.q, cell.r) + cell.top);
      }
    }
    for (const cell of cells) {
      const fresh = this.previousBases && !this.previousBases.has(key(cell.q, cell.r) + cell.base);
      this.tiles.append(this.place(centerX(cell.q), centerY(cell.q, cell.r), this.drawCell(cell, byPosition), fresh));
    }
    this.drawTops(cells);
    for (const coord of highlighted) {
      this.drawSpot(coord);
    }
    this.previousBases = bases;
    this.previousTops = topKeys;

    // il riquadro da inquadrare: tessere e posizioni valide (non increspature e anteprima)
    const coords = [...cells, ...highlighted];
    if (coords.length) {
      const xs = coords.map((c) => centerX(c.q));
      const ys = coords.map((c) => centerY(c.q, c.r));
      const minX = Math.min(...xs) - SIZE;
      const minY = Math.min(...ys) - SIZE;
      this.bounds = { x: minX, y: minY, w: Math.max(...xs) + SIZE - minX, h: Math.max(...ys) + SIZE - minY };
    }
    this.layout();
  }

  // un disegno spostato nel suo punto, con un gruppo interno che puo' "sbocciare" senza muoversi
  place(x, y, content, fresh, attrs = {}) {
    const inner = g({ class: fresh && this.animated ? 'pop' : null }, content);
    return g({ ...attrs, transform: `translate(${x.toFixed(2)} ${y.toFixed(2)})${attrs.transform ? ' ' + attrs.transform : ''}` }, inner);
  }

  // ---------------------------------------------------------------- tessere

  drawCell(cell, byPosition) {
    const s = SIZE;
    const group = g();
    // aspetto della base: i pezzi tipo sponda sono pietra; il salice e' prato (senza lati azzurri)
    // oppure acqua con le foglie, come il fogliame
    let look;
    if (cell.base === 'THREE') {
      look = cell.blueSides.includes(true) ? 'LEAF' : 'GRASS';
    } else if (cell.base === 'BORDER' || isPondPiece(cell.base)) {
      look = 'BORDER';
    } else {
      look = cell.base;
    }
    const fill = { BORDER: 'url(#stone)', GRASS: 'url(#grass)', WATERSOURCE: 'url(#source)' }[look] || 'url(#water)';
    const hex = hexagon(0, 0, s, { fill });
    hex.append(tooltip(`(${cell.q},${cell.r})  ${this.nameOf(cell.base)}${cell.top ? ' + ' + this.nameOf(cell.top) : ''}`));
    group.append(hex);
    if (look !== 'BORDER' && look !== 'GRASS') {
      this.waterCentres.push([centerX(cell.q), centerY(cell.q, cell.r)]);
    }

    if (look === 'GRASS') {
      group.append(grassTufts(s, Math.abs(cell.q * 5 + cell.r * 3)));
    } else if (look === 'LEAF') {
      group.append(floatingLeaves(s, Math.abs(cell.q * 7 + cell.r * 3)));
    } else if (look === 'WATERSOURCE') {
      for (let i = 1; i <= 2; i++) {
        group.append(el('circle', { r: s * 0.22 * i, fill: 'none', stroke: 'rgba(255,255,255,0.45)' }));
      }
    } else if (look !== 'BORDER') {
      group.append(waves(s));
    }

    // lati: azzurri o di roccia; tra esagoni della stessa tessera solo una linea sottile
    for (let corner = 0; corner < 6; corner++) {
      const direction = (6 - corner) % 6;   // il lato tra l'angolo k e k+1 guarda nella direzione (6-k)%6
      const neighbour = byPosition.get(key(cell.q + DQ[direction], cell.r + DR[direction]));
      const a1 = Math.PI / 3 * corner;
      const a2 = Math.PI / 3 * (corner + 1);
      const x1 = s * Math.cos(a1);
      const y1 = s * Math.sin(a1);
      const x2 = s * Math.cos(a2);
      const y2 = s * Math.sin(a2);
      let stroke;
      let width;
      if (neighbour && neighbour.tileId === cell.tileId) {
        stroke = 'rgba(255,255,255,0.3)';
        width = 1;
      } else if (cell.blueSides[direction]) {
        stroke = 'rgba(46,127,143,0.75)';
        width = Math.max(1, s * 0.05);
      } else {
        stroke = C.woodDark;
        width = Math.max(2, s * 0.13);
        group.append(...rocks(x1, y1, x2, y2));
      }
      group.append(el('line', { x1, y1, x2, y2, stroke, 'stroke-width': width, 'stroke-linecap': 'round' }));
    }
    return group;
  }

  // ---------------------------------------------------------------- cio' che sta sopra

  drawTops(cells) {
    const koiById = new Map();
    const objectivesById = new Map();
    const piecesById = new Map();   // statua, ponticello... sono la base stessa
    const push = (map, id, cell) => (map.get(id) || map.set(id, []).get(id)).push(cell);
    const freshTop = (cell) => this.previousTops && !this.previousTops.has(key(cell.q, cell.r) + cell.top);

    for (const cell of cells) {
      if (isPondPiece(cell.base)) {
        push(piecesById, cell.tileId, cell);
      }
      if (!cell.top) {
        continue;
      }
      const cx = centerX(cell.q);
      const cy = centerY(cell.q, cell.r);
      if (cell.top === 'LILY') {
        this.tops.append(this.place(cx, cy, lily(SIZE * 0.62), freshTop(cell), { transform: `rotate(${(cell.q * 37 + cell.r * 61) % 360})` }));
      } else if (cell.top === 'LANTERN') {
        this.tops.append(this.place(cx, cy - SIZE * 0.05, lantern(SIZE * 1.35), freshTop(cell)));
      } else if (cell.top.endsWith('KOI')) {
        push(koiById, cell.topId, cell);
      } else {
        push(objectivesById, cell.topId, cell);
      }
    }

    for (const [id, koiCells] of koiById) {
      this.drawKoi(id, koiCells, koiCells.some(freshTop));
    }
    for (const objectiveCells of objectivesById.values()) {
      const first = objectiveCells[0];
      this.drawObjective(objectiveCells, first.top, first.topRotation, true, objectiveCells.some(freshTop));
    }
    for (const piece of piecesById.values()) {
      const fresh = piece.some((cell) => this.previousBases && !this.previousBases.has(key(cell.q, cell.r) + cell.base));
      // il salice sta sul prato, non al centro del pezzo (coprirebbe i sassi e l'acqua)
      const under = piece[0].base === 'THREE' ? piece.filter((cell) => !cell.blueSides.includes(true)) : piece;
      this.drawObjective(under.length ? under : piece, piece[0].base, 0, false, fresh);
    }
  }

  // tessera obiettivo: il disegno sul baricentro dei suoi esagoni, girato come la tessera;
  // per quelle sopra l'acqua anche un'impronta dorata, cosi' si vede quali esagoni coprono
  drawObjective(cells, type, rotation, veiled, fresh) {
    let sumX = 0;
    let sumY = 0;
    for (const cell of cells) {
      const x = centerX(cell.q);
      const y = centerY(cell.q, cell.r);
      sumX += x;
      sumY += y;
      if (veiled) {
        this.tops.append(hexagon(x, y, SIZE * 0.9, { fill: 'rgba(212,161,58,0.18)', stroke: 'rgba(212,161,58,0.7)', 'stroke-width': 1.2 }));
      }
    }
    const art = objective(type, SIZE, cells.length);
    const oriented = hasShape(type) ? g({ transform: orient(rotation) }, art) : art;
    const holder = this.place(sumX / cells.length, sumY / cells.length, oriented, fresh);
    holder.append(tooltip(this.nameOf(type)));
    this.tops.append(holder);
  }

  // una carpa sola anche se copre piu' esagoni: al centro delle sue celle, orientata secondo la rotazione
  drawKoi(id, cells, fresh) {
    const xs = cells.map((c) => centerX(c.q));
    const ys = cells.map((c) => centerY(c.q, c.r));
    const cx = xs.reduce((a, b) => a + b) / cells.length;
    const cy = ys.reduce((a, b) => a + b) / cells.length;
    const type = cells[0].top;
    const angle = cells.length === 2
      ? Math.atan2(ys[1] - ys[0], xs[1] - xs[0]) * 180 / Math.PI
      : 30 - 60 * cells[0].topRotation;   // direzione d sullo schermo: 30 - 60*d gradi
    const length = SIZE * (type === 'SMALLKOI' ? 1.35 : type === 'MEDIUMKOI' ? 2.5 : 2.9);

    // il contenitore orienta, il pesce dentro "nuota"
    const fish = g({ class: this.animated ? 'swim' : null }, koi(length, id));
    if (this.animated) {
      fish.style.animationDelay = `${(-Math.random() * 1.6).toFixed(2)}s`;
    }
    const movable = this.movableKoi.has(id);
    const holder = this.place(cx, cy, fish, fresh, {
      class: 'koi-holder' + (movable ? ' movable' : '') + (movable && id === this.selectedKoi ? ' selected' : ''),
      transform: `rotate(${angle.toFixed(1)})`,
    });
    holder.append(tooltip(this.nameOf(type) + (movable ? ' (clicca per spostarla)' : '')));
    if (movable) {
      holder.addEventListener('click', (event) => {
        event.stopPropagation();
        if (!this.justDragged) {
          this.onKoiClicked(id);
        }
      });
    }
    this.tops.append(holder);
  }

  // ---------------------------------------------------------------- posizioni valide e anteprima

  drawSpot(coord) {
    const cx = centerX(coord.q);
    const cy = centerY(coord.q, coord.r);
    const spot = hexagon(cx, cy, SIZE * 0.92, {
      class: this.animated ? 'spot' : null, fill: 'rgba(255,240,179,0.4)', stroke: C.gold, 'stroke-width': 2.5, 'stroke-dasharray': '7 5',
      style: 'cursor:pointer',
    });
    let touch = false;
    spot.addEventListener('pointerdown', (event) => {
      touch = event.pointerType !== 'mouse';
    });
    spot.addEventListener('click', (event) => {
      event.stopPropagation();
      if (this.justDragged) {   // un trascinamento per spostare lo stagno non deve piazzare nulla
        return;
      }
      // col dito non c'e' il passaggio del mouse: il primo tocco mostra l'anteprima, il secondo piazza
      if (touch && !(this.hovered && this.hovered.q === coord.q && this.hovered.r === coord.r && this.armed)) {
        this.showGhost(coord);
        this.armed = true;
        return;
      }
      this.armed = false;
      this.onHexClicked(coord);
    });
    spot.addEventListener('pointerenter', (event) => {
      if (event.pointerType === 'mouse') {
        this.showGhost(coord);
      }
    });
    spot.addEventListener('pointerleave', (event) => {
      if (event.pointerType === 'mouse') {
        this.hovered = null;
        this.ghost.replaceChildren();
      }
    });
    const label = el('text', { class: 'spot-label', x: cx, y: cy + 4, 'text-anchor': 'middle' });
    label.textContent = `${coord.q},${coord.r}`;
    this.spots.append(spot, label);
  }

  // ridisegna l'anteprima, ad esempio dopo un cambio di rotazione con il mouse ancora sopra
  refreshGhost() {
    if (this.hovered) {
      this.showGhost(this.hovered);
    }
  }

  showGhost(coord) {
    this.hovered = coord;
    this.ghost.replaceChildren(...this.ghostShape(coord).map((part) => hexagon(centerX(part.q), centerY(part.q, part.r), SIZE * 0.95, {
      fill: 'rgba(255,255,255,0.55)', stroke: C.vermilion, 'stroke-width': 2.5,
    })));
  }

  // ---------------------------------------------------------------- inquadratura, zoom e spostamento

  layout() {
    const width = this.svg.clientWidth;
    const height = this.svg.clientHeight;
    const pad = SIZE * 0.75;
    let w = this.bounds.w + pad * 2;
    let h = this.bounds.h + pad * 2;
    if (width > 0 && height > 0) {
      w = Math.max(w, width / this.maxScale);
      h = Math.max(h, height / this.maxScale);
    }
    this.base = { w, h };
    const cx = this.bounds.x + this.bounds.w / 2 - this.panX;
    const cy = this.bounds.y + this.bounds.h / 2 - this.panY;
    const vw = w / this.zoom;
    const vh = h / this.zoom;
    this.svg.setAttribute('viewBox', `${(cx - vw / 2).toFixed(1)} ${(cy - vh / 2).toFixed(1)} ${vw.toFixed(1)} ${vh.toFixed(1)}`);
  }

  // unita' del disegno per pixel a schermo, con l'adattamento "meet" del viewBox
  unitsPerPixel() {
    const width = this.svg.clientWidth || 1;
    const height = this.svg.clientHeight || 1;
    return Math.max(this.base.w / this.zoom / width, this.base.h / this.zoom / height);
  }

  resetView() {
    this.zoom = 1;
    this.panX = 0;
    this.panY = 0;
    this.layout();
  }

  // il punto sotto (px, py), in pixel dal centro dell'SVG, resta fermo mentre lo zoom cambia
  zoomAt(factor, px, py) {
    const zoom = Math.max(0.4, Math.min(4, this.zoom * factor));
    const before = this.unitsPerPixel();
    this.zoom = zoom;
    const after = this.unitsPerPixel();
    this.panX -= px * (before - after);
    this.panY -= py * (before - after);
    this.layout();
  }

  // rotella = zoom verso il puntatore, trascinare = spostare, due dita = zoom, doppio clic = vista iniziale
  enableZoomAndPan() {
    const svg = this.svg;
    const pointers = new Map();
    let start = null;
    let pinch = null;
    const fromCentre = (event) => {
      const rect = svg.getBoundingClientRect();
      return [event.clientX - rect.left - rect.width / 2, event.clientY - rect.top - rect.height / 2];
    };

    svg.addEventListener('wheel', (event) => {
      event.preventDefault();
      const [px, py] = fromCentre(event);
      this.zoomAt(event.deltaY < 0 ? 1.12 : 1 / 1.12, px, py);
    }, { passive: false });

    svg.addEventListener('pointerdown', (event) => {
      pointers.set(event.pointerId, [event.clientX, event.clientY]);
      this.justDragged = false;
      if (pointers.size === 1) {
        start = { x: event.clientX, y: event.clientY, panX: this.panX, panY: this.panY, moved: false };
      } else if (pointers.size === 2) {
        const [a, b] = [...pointers.values()];
        pinch = { distance: Math.hypot(a[0] - b[0], a[1] - b[1]), zoom: this.zoom };
      }
    });
    svg.addEventListener('pointermove', (event) => {
      if (!pointers.has(event.pointerId)) {
        return;
      }
      pointers.set(event.pointerId, [event.clientX, event.clientY]);
      if (pointers.size === 2 && pinch) {
        const [a, b] = [...pointers.values()];
        const distance = Math.hypot(a[0] - b[0], a[1] - b[1]);
        const rect = svg.getBoundingClientRect();
        const mid = [(a[0] + b[0]) / 2 - rect.left - rect.width / 2, (a[1] + b[1]) / 2 - rect.top - rect.height / 2];
        this.zoomAt(pinch.zoom * distance / pinch.distance / this.zoom, mid[0], mid[1]);
        this.justDragged = true;
        return;
      }
      if (!start) {
        return;
      }
      const dx = event.clientX - start.x;
      const dy = event.clientY - start.y;
      if (!start.moved && Math.hypot(dx, dy) < 5) {
        return;
      }
      if (!start.moved) {
        start.moved = true;
        svg.setPointerCapture(event.pointerId);
        svg.classList.add('dragging');
      }
      const upp = this.unitsPerPixel();
      this.panX = start.panX + dx * upp;
      this.panY = start.panY + dy * upp;
      this.layout();
    });
    const release = (event) => {
      pointers.delete(event.pointerId);
      if (start && start.moved) {
        this.justDragged = true;   // il click che segue non piazza niente
        setTimeout(() => { this.justDragged = false; }, 0);
      }
      if (pointers.size < 2) {
        pinch = null;
      }
      if (pointers.size === 0) {
        start = null;
        svg.classList.remove('dragging');
      }
    };
    svg.addEventListener('pointerup', release);
    svg.addEventListener('pointercancel', release);
    svg.addEventListener('dblclick', () => this.resetView());
  }

  // ---------------------------------------------------------------- increspature

  // un'increspatura che si allarga e svanisce su un esagono d'acqua a caso
  spawnRipple() {
    if (!this.svg.isConnected || !this.waterCentres.length || document.hidden) {
      return;
    }
    const [x0, y0] = this.waterCentres[Math.floor(Math.random() * this.waterCentres.length)];
    const ring = el('circle', {
      cx: x0 + (Math.random() - 0.5) * SIZE * 0.6, cy: y0 + (Math.random() - 0.5) * SIZE * 0.6,
      r: SIZE * 0.08, fill: 'none', stroke: 'rgba(255,255,255,0.8)', 'stroke-width': 1.5,
    }, [
      el('animate', { attributeName: 'r', to: SIZE * 0.55, dur: '1.8s', begin: 'indefinite', fill: 'freeze', calcMode: 'spline', keySplines: '0 0 0.58 1', keyTimes: '0;1' }),
      el('animate', { attributeName: 'opacity', from: 1, to: 0, dur: '1.8s', begin: 'indefinite', fill: 'freeze' }),
    ]);
    this.effects.append(ring);
    // aggiunte a pagina gia' avviata: senza beginElement partirebbero "nel passato", gia' finite
    for (const animation of ring.children) {
      animation.beginElement();
    }
    setTimeout(() => ring.remove(), 1900);
  }
}

// ciottoli lungo un lato di roccia della sponda
function rocks(x1, y1, x2, y2) {
  const list = [];
  for (let i = 0; i < 3; i++) {
    const t = 0.2 + i * 0.3;
    // spostati un po' verso l'interno dell'esagono (il centro e' 0, 0)
    const x = (x1 + (x2 - x1) * t) * 0.82;
    const y = (y1 + (y2 - y1) * t) * 0.82;
    list.push(el('ellipse', {
      cx: x, cy: y, rx: SIZE * 0.13, ry: SIZE * 0.09,
      fill: i % 2 === 0 ? C.stoneDark : '#a69c88', stroke: 'rgba(77,64,51,0.5)',
    }));
  }
  return list;
}
