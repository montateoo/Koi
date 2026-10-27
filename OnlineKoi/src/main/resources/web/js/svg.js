// piccoli attrezzi per costruire SVG dal codice, e le definizioni condivise (sfumature e ombre)

export const NS = 'http://www.w3.org/2000/svg';
export const SQRT3 = Math.sqrt(3);

// arrotonda a 2 decimali: SVG piu' leggeri, nessuna differenza a schermo
export const f = (n) => Math.round(n * 100) / 100;

export function el(tag, attrs = {}, children = []) {
  const node = document.createElementNS(NS, tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (value !== undefined && value !== null && value !== false) {
      node.setAttribute(key, typeof value === 'number' ? f(value) : value);
    }
  }
  for (const child of children) {
    if (child) {
      node.appendChild(child);
    }
  }
  return node;
}

export const g = (attrs = {}, ...children) => el('g', attrs, children);

export function tooltip(text) {
  const node = el('title');
  node.textContent = text;
  return node;
}

let counter = 0;
export const uid = (prefix) => `${prefix}-${++counter}`;

// esagono a punta piatta centrato in (cx, cy): l'angolo k sta a 60*k gradi (y verso il basso)
export function hexPoints(cx, cy, size) {
  const points = [];
  for (let corner = 0; corner < 6; corner++) {
    const a = Math.PI / 3 * corner;
    points.push(`${f(cx + size * Math.cos(a))},${f(cy + size * Math.sin(a))}`);
  }
  return points.join(' ');
}

export const hexagon = (cx, cy, size, attrs = {}) => el('polygon', { points: hexPoints(cx, cy, size), ...attrs });

// un <svg> che si adatta al proprio contenuto: il riquadro si calcola quando e' nella pagina
export function fitted(content, pad = 2, attrs = {}) {
  const svg = el('svg', { ...attrs, 'aria-hidden': 'true' }, [content]);
  // finche' e' nascosto (schermata non ancora visibile) il riquadro misura zero: si riprova al fotogramma dopo
  const measure = (tries) => {
    const box = svg.isConnected ? content.getBBox() : null;
    if (box && box.width > 0) {
      svg.setAttribute('viewBox', `${f(box.x - pad)} ${f(box.y - pad)} ${f(box.width + pad * 2)} ${f(box.height + pad * 2)}`);
    } else if (tries > 0) {
      requestAnimationFrame(() => measure(tries - 1));
    }
  };
  requestAnimationFrame(() => measure(30));
  return svg;
}

// sfumature e ombre usate da tutti i disegni: una volta sola nella pagina, richiamate con url(#id).
// Le ombre sono in proporzione alla figura (objectBoundingBox), cosi' valgono a qualunque misura
export function installDefs() {
  const radial = (id, stops, cx = 0.32, cy = 0.3, r = 0.8) =>
    el('radialGradient', { id, cx, cy, r, fx: cx, fy: cy },
      stops.map(([offset, color, opacity = 1]) => el('stop', { offset, 'stop-color': color, 'stop-opacity': opacity })));
  const linear = (id, stops, x2 = 1, y2 = 0) =>
    el('linearGradient', { id, x1: 0, y1: 0, x2, y2 },
      stops.map(([offset, color, opacity = 1]) => el('stop', { offset, 'stop-color': color, 'stop-opacity': opacity })));
  const shadow = (id, dx, dy, blur, color, opacity) =>
    el('filter', { id, x: '-40%', y: '-40%', width: '180%', height: '180%', primitiveUnits: 'objectBoundingBox' }, [
      el('feDropShadow', { dx, dy, stdDeviation: blur, 'flood-color': color, 'flood-opacity': opacity }),
    ]);

  const defs = el('defs', {}, [
    radial('water', [[0, '#c3ecf1'], [1, '#6cc3cf']]),
    radial('source', [[0, '#a9e2ea'], [1, '#3fa6b6']]),
    radial('stone', [[0, '#e3d9c3'], [1, '#b3a68c']], 0.33, 0.3, 0.75),
    radial('grass', [[0, '#a6d06a'], [1, '#5e8f2f']], 0.33, 0.3, 0.75),
    radial('lily-pad', [[0, '#8cc66b'], [1, '#4f8a3a']], 0.5, 0.5, 0.5),
    radial('coin', [[0, '#ffe08a'], [1, '#d4a13a']], 0.35, 0.35, 0.8),
    radial('path-stone', [[0, '#e6e1d6'], [1, '#9b958a']], 0.4, 0.4, 0.6),
    radial('shell', [[0, '#7fae45'], [1, '#2f5a1c']], 0.38, 0.36, 0.62),
    radial('halo', [[0, '#ffe68c', 0.55], [1, '#ffe68c', 0]], 0.5, 0.5, 0.5),
    linear('petal-pink', [[0, '#fff0f5'], [1, '#ef6f9a']]),
    linear('petal-purple', [[0, '#fff0f5'], [1, '#a35bc9']]),
    linear('veil', [[0, '#ff8c40', 0.25], [1, '#fffaeb', 0.85]]),
    linear('fall', [[0, '#d9f4f7'], [1, '#5cc2d0']], 0, 1),
    linear('deck', [[0, '#ff4a2c'], [1, '#7c1c10']], 0, 1),
    linear('bamboo-h', [[0, '#ffffa0'], [0.5, '#b3c656'], [1, '#6b7d22']], 0, 1),
    linear('bamboo-v', [[0, '#ffffa0'], [0.5, '#b3c656'], [1, '#6b7d22']], 1, 0),
    shadow('shade', 0.02, 0.04, 0.04, '#1a261a', 0.45),
    shadow('koi-shadow', 0, 0.06, 0.06, '#003348', 0.35),
    shadow('soft', 0, 0, 0.07, '#003320', 0.35),
    shadow('gold-glow', 0, 0, 0.06, '#ffcc4d', 0.75),
  ]);
  const holder = el('svg', { width: 0, height: 0, 'aria-hidden': 'true', style: 'position:absolute' }, [defs]);
  document.body.prepend(holder);
}
