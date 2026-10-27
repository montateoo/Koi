// disegni originali in stile giapponese, fatti solo di forme SVG: acqua, foglie, carpe, ninfee, lanterne, monete.
// Ogni disegno e' centrato in (0, 0); le misure sono in proporzione a "size" (raggio dell'esagono) o "length"
import { el, g, hexagon, uid } from './svg.js';

export const C = {
  paper: '#f6efdc',
  paperLight: '#fbf7ec',
  ink: '#2b2622',
  inkSoft: '#7a6c5a',
  vermilion: '#c8341f',
  gold: '#d4a13a',
  tealLight: '#c3ecf1',
  teal: '#6cc3cf',
  tealDeep: '#2e7f8f',
  wood: '#9a7048',
  woodDark: '#6b4a2e',
  stoneDark: '#8f8574',
};

// ---------------------------------------------------------------- acqua, prato, foglie

// due piccole onde bianche, come nei disegni delle tessere Acqua
export function waves(size) {
  const group = g();
  for (let i = 0; i < 2; i++) {
    const cx = -size * 0.28 + i * size * 0.42;
    const cy = size * 0.18 - i * size * 0.36;
    const rx = size * 0.17;
    const ry = size * 0.09;
    group.append(el('path', {
      d: `M${cx + rx} ${cy} A${rx} ${ry} 0 0 0 ${cx - rx} ${cy}`,
      fill: 'none', stroke: 'rgba(255,255,255,0.6)', 'stroke-width': Math.max(1, size * 0.045),
    }));
  }
  return group;
}

// foglioline gialle e verdi che galleggiano, come sulla tessera Fogliame
export function floatingLeaves(size, seed) {
  const spots = [[-0.35, -0.2], [0.25, -0.35], [0.3, 0.25], [-0.2, 0.35], [0, 0]];
  const colors = ['#e3c53c', '#b9cf4a', '#d9a52c', '#9fc24a'];
  const scale = size / 34 * 1.15;
  const group = g();
  spots.forEach(([x, y], i) => {
    const angle = (seed * 47 + i * 73) % 360;
    group.append(el('path', {
      d: 'M0 0 C3 -4 9 -4 12 0 C9 4 3 4 0 0 Z',
      fill: colors[(i + seed) % colors.length], stroke: 'rgba(77,89,26,0.5)', 'stroke-width': 1 / scale,
      transform: `translate(${(x * size).toFixed(2)} ${(y * size).toFixed(2)}) rotate(${angle}) scale(${scale.toFixed(3)}) translate(-6 0)`,
    }));
  });
  return group;
}

// ciuffi d'erba e qualche fiorellino giallo, sparsi in modo diverso per ogni esagono (prato del salice)
export function grassTufts(size, seed) {
  const spots = [[-0.4, -0.25], [0.3, -0.4], [0.4, 0.2], [-0.2, 0.4], [0, -0.05]];
  const group = g();
  for (let i = 0; i < spots.length; i++) {
    const [sx, sy] = spots[(i + seed) % spots.length];
    const x = sx * size;
    const y = sy * size;
    if (i % 2 === 0) {
      group.append(el('circle', { cx: x, cy: y, r: Math.max(1, size * 0.05), fill: '#f3d04a' }));
    } else {
      for (let blade = -1; blade <= 1; blade++) {
        group.append(el('line', {
          x1: x, y1: y, x2: x + blade * size * 0.06, y2: y - size * 0.12,
          stroke: '#3f6e1f', 'stroke-width': Math.max(1, size * 0.03),
        }));
      }
    }
  }
  return group;
}

// ---------------------------------------------------------------- carpa

const KOI_PALETTES = [
  { body: '#fffaf2', patches: ['#e8452c', '#e8452c', '#f06a3a'] },   // kohaku
  { body: '#fffaf2', patches: ['#e8452c', C.ink, '#e8452c'] },       // showa
  { body: '#f6cc4a', patches: ['#e9a21e', '#fbe08a', '#e9a21e'] },   // yamabuki
];

// una carpa che guarda verso destra, centrata in (0, 0)
export function koi(length, palette) {
  const { body, patches } = KOI_PALETTES[((palette % 3) + 3) % 3];
  const l = length;
  const clip = uid('koi-body');
  const bodyShape = { cx: l * 0.04, cy: 0, rx: l * 0.33, ry: l * 0.13 };

  const tail = el('polygon', {
    points: [[-0.26, 0], [-0.52, -0.17], [-0.42, 0], [-0.52, 0.17]].map(([x, y]) => `${(x * l).toFixed(2)},${(y * l).toFixed(2)}`).join(' '),
    fill: body, 'fill-opacity': 0.85, stroke: 'rgba(102,51,26,0.35)',
  });
  const fin = (y, angle) => el('ellipse', {
    cx: l * 0.08, cy: y, rx: l * 0.1, ry: l * 0.045, fill: body, 'fill-opacity': 0.75, stroke: 'rgba(102,51,26,0.3)',
    transform: `rotate(${angle} ${(l * 0.08).toFixed(2)} ${y.toFixed(2)})`,
  });
  const spots = g({ 'clip-path': `url(#${clip})` },
    el('ellipse', { cx: l * 0.2, cy: -l * 0.03, rx: l * 0.12, ry: l * 0.09, fill: patches[0] }),
    el('ellipse', { cx: -l * 0.06, cy: l * 0.04, rx: l * 0.1, ry: l * 0.08, fill: patches[1] }),
    el('ellipse', { cx: -l * 0.2, cy: -l * 0.03, rx: l * 0.06, ry: l * 0.05, fill: patches[2] }));
  const eye = (y) => el('circle', { cx: l * 0.3, cy: y, r: Math.max(1, l * 0.018), fill: C.ink });

  return g({ filter: 'url(#koi-shadow)' },
    el('defs', {}, [el('clipPath', { id: clip }, [el('ellipse', bodyShape)])]),
    tail, fin(-l * 0.13, -35), fin(l * 0.13, 35),
    el('ellipse', { ...bodyShape, fill: body, stroke: 'rgba(89,51,26,0.45)' }),
    spots, eye(-l * 0.05), eye(l * 0.05));
}

// ---------------------------------------------------------------- ninfea, percorso

// ninfea su una foglia rotonda con la tacca; purple = la Ninfea viola degli obiettivi
export function lily(radius, purple = false) {
  const r = radius;
  const a = 25 * Math.PI / 180;
  const pad = el('path', {
    d: `M0 0 L${r * Math.cos(a)} ${-r * Math.sin(a)} A${r} ${r} 0 1 0 ${r * Math.cos(a)} ${r * Math.sin(a)} Z`,
    fill: 'url(#lily-pad)', stroke: '#3d6e2c',
  });
  const outline = purple ? 'rgba(91,44,111,0.5)' : 'rgba(194,24,91,0.5)';
  const flower = g({ transform: 'scale(0.9)' });
  for (let i = 0; i < 8; i++) {
    flower.append(el('ellipse', {
      cx: r * 0.32, cy: 0, rx: r * 0.33, ry: r * 0.13,
      fill: purple ? 'url(#petal-purple)' : 'url(#petal-pink)', stroke: outline, transform: `rotate(${i * 45 + 22})`,
    }));
  }
  flower.append(el('circle', { cx: 0, cy: 0, r: r * 0.15, fill: '#ffd54f', stroke: '#e0a800' }));
  return g({ filter: 'url(#soft)' }, pad, flower);
}

// tessera Percorso: tre pietre piatte da guado
export function pathStones(radius) {
  const group = g({ filter: 'url(#soft)' });
  for (const [x, y, size] of [[-0.45, 0.28, 0.42], [0.08, -0.05, 0.5], [0.5, -0.4, 0.36]]) {
    group.append(el('ellipse', {
      cx: radius * x, cy: radius * y, rx: radius * size, ry: radius * size * 0.72,
      fill: 'url(#path-stone)', stroke: '#6f6a60', 'stroke-width': 1.2,
    }));
  }
  return group;
}

// ---------------------------------------------------------------- lanterna

// lanterna di pietra (toro) alta "h", con il piede in (0, h/2); la finestra accesa tremola (CSS .lantern-light)
export function lantern(h) {
  const part = (x, y, w, height) => el('rect', {
    x: x * h, y: y * h, width: w * h, height: height * h, rx: h * 0.025,
    fill: '#9aa0a0', stroke: '#6f7676',
  });
  return g({},
    part(-0.24, 0.36, 0.48, 0.12),
    part(-0.08, 0.12, 0.16, 0.26),
    part(-0.2, -0.12, 0.4, 0.26),
    el('rect', { class: 'lantern-light', x: -h * 0.1, y: -h * 0.07, width: h * 0.2, height: h * 0.16, fill: '#ffd54f' }),
    el('polygon', {
      points: [[-0.36, -0.12], [0.36, -0.12], [0.2, -0.3], [-0.2, -0.3]].map(([x, y]) => `${(x * h).toFixed(2)},${(y * h).toFixed(2)}`).join(' '),
      fill: '#6f7676',
    }),
    el('circle', { cx: 0, cy: -h * 0.36, r: h * 0.07, fill: '#6f7676' }));
}

// ---------------------------------------------------------------- monete, gettoni, sigilli

export function coin(radius) {
  return g({},
    el('circle', { r: radius, fill: 'url(#coin)', stroke: '#a07818' }),
    el('rect', { x: -radius * 0.28, y: -radius * 0.28, width: radius * 0.56, height: radius * 0.56, fill: C.paper, stroke: '#a07818' }));
}

// segnalino Fortuna: disco vermiglio con il kanji della fortuna
export function fortune(radius) {
  const sign = el('text', {
    'text-anchor': 'middle', 'dominant-baseline': 'central', y: radius * 0.04,
    'font-size': radius * 1.15, 'font-weight': 800, fill: C.paperLight, 'font-family': 'Shippori Mincho, serif',
  });
  sign.textContent = '福';
  return g({}, el('circle', { r: radius, fill: C.vermilion, stroke: '#8e1f10' }), sign);
}

// icona tonda pronta da mettere nel testo (moneta o Fortuna)
export function badgeIcon(kind, radius) {
  const r = radius + 1;
  return el('svg', { width: r * 2, height: r * 2, viewBox: `${-r} ${-r} ${r * 2} ${r * 2}`, 'aria-hidden': 'true' },
    [kind === 'coin' ? coin(radius) : fortune(radius)]);
}

// sigillo quadrato con un kanji bianco (elemento HTML)
export function seal(kanji, size, color) {
  const span = document.createElement('span');
  span.className = 'seal';
  span.textContent = kanji;
  span.style.cssText = `width:${size}px;height:${size}px;font-size:${size * 0.62}px;background:${color}`;
  return span;
}

// ---------------------------------------------------------------- icone delle tessere del magazzino

export function tileIcon(type, size) {
  const base = (fill) => hexagon(0, 0, size, { fill, stroke: C.woodDark, 'stroke-width': 1.5 });
  switch (type) {
    case 'LEAF':
      return g({}, base('url(#water)'), floatingLeaves(size, 1));
    case 'BORDER': {
      const group = g({}, base('url(#stone)'));
      for (let i = 0; i < 5; i++) {
        group.append(el('ellipse', {
          cx: -size * 0.45 + i * size * 0.22, cy: -size * 0.25 + (i % 2) * size * 0.12,
          rx: size * 0.14, ry: size * 0.1, fill: C.stoneDark,
        }));
      }
      return group;
    }
    case 'LILY':
      return g({}, base('url(#water)'), lily(size * 0.62));
    case 'LANTERN':
      return g({}, base('url(#stone)'), lantern(size * 1.25));
    case 'SMALLKOI':
    case 'MEDIUMKOI':
    case 'BIGKOI': {
      const length = size * (type === 'SMALLKOI' ? 1.3 : type === 'MEDIUMKOI' ? 1.6 : 1.85);
      const palette = type === 'BIGKOI' ? 2 : type === 'MEDIUMKOI' ? 1 : 0;
      return g({}, base('url(#water)'), g({ transform: 'rotate(-30)' }, koi(length, palette)));
    }
    default:
      return g({}, base('url(#water)'), waves(size));
  }
}
