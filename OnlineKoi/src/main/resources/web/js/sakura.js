// petali di ciliegio che cadono ondeggiando sopra la pagina; non intercettano i clic
export function startSakura(canvas, count = 12) {
  const context = canvas.getContext('2d');
  const petal = new Path2D('M0 0 C3 -6 11 -7 15 -2 L12 0 L15 2 C11 7 3 6 0 0 Z');   // con la tipica tacca in punta
  const petals = [];
  let width = 0;
  let height = 0;
  let last = 0;

  const resize = () => {
    const ratio = window.devicePixelRatio || 1;
    width = canvas.clientWidth;
    height = canvas.clientHeight;
    canvas.width = width * ratio;
    canvas.height = height * ratio;
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
  };
  const reset = (p, anywhere) => {
    p.x = Math.random() * width;
    p.y = anywhere ? Math.random() * height : -20 - Math.random() * 60;
    p.speed = 18 + Math.random() * 26;
    p.sway = 12 + Math.random() * 22;
    p.phase = Math.random() * Math.PI * 2;
    p.spin = (Math.random() < 0.5 ? 1 : -1) * (20 + Math.random() * 60);
  };

  resize();
  window.addEventListener('resize', resize);
  for (let i = 0; i < count; i++) {
    const p = { angle: Math.random() * 360, scale: 1 + Math.random() * 0.9, alpha: 0.7 + Math.random() * 0.3 };
    reset(p, true);
    petals.push(p);
  }
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return;
  }

  const gradient = context.createLinearGradient(0, 0, 15, 0);
  gradient.addColorStop(0, '#ffd6e2');
  gradient.addColorStop(1, '#ec7fa3');

  const frame = (now) => {
    const dt = last ? Math.min((now - last) / 1000, 0.05) : 0;
    last = now;
    const time = now / 1000;
    context.clearRect(0, 0, width, height);
    for (const p of petals) {
      p.y += p.speed * dt;
      p.x += Math.sin(time * 0.9 + p.phase) * p.sway * dt + 6 * dt;   // un filo di vento verso destra
      p.angle += p.spin * dt;
      if (p.y > height + 20 || p.x > width + 30) {
        reset(p, false);
      }
      context.save();
      context.translate(p.x, p.y);
      context.rotate(p.angle * Math.PI / 180);
      context.scale(p.scale, p.scale);
      context.translate(-7.5, 0);
      context.globalAlpha = p.alpha;
      context.fillStyle = gradient;
      context.fill(petal);
      context.strokeStyle = 'rgba(209,102,139,0.6)';
      context.lineWidth = 1 / p.scale;
      context.stroke(petal);
      context.restore();
    }
    requestAnimationFrame(frame);
  };
  requestAnimationFrame(frame);
}

// sfondo seigaiha (onde a scaglie del mare), il motivo tradizionale giapponese, come immagine da ripetere
export function seigaiha(background, line, radius = 30) {
  // si disegna un'area piu' grande e si ritaglia un periodo intero (2R x R) dal mezzo,
  // cosi' le scaglie che sbordano si raccordano con le piastrelle vicine
  const canvas = document.createElement('canvas');
  canvas.width = radius * 6;
  canvas.height = radius * 4;
  const context = canvas.getContext('2d');
  context.fillStyle = background;
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.strokeStyle = line;
  context.lineWidth = 1.2;
  let row = 0;
  for (let y = -radius; y < canvas.height + radius; y += radius / 2, row++) {
    const offset = row % 2 === 0 ? 0 : radius;
    for (let x = -radius + offset; x < canvas.width + radius; x += radius * 2) {
      context.fillStyle = background;
      context.beginPath();
      context.moveTo(x, y);
      context.arc(x, y, radius, Math.PI, 0);
      context.fill();
      for (let r = radius; r > 4; r -= radius / 4) {
        context.beginPath();
        context.arc(x, y, r, Math.PI, 0);
        context.stroke();
      }
    }
  }
  const tile = document.createElement('canvas');
  tile.width = radius * 2;
  tile.height = radius;
  tile.getContext('2d').drawImage(canvas, radius * 2, radius * 2, radius * 2, radius, 0, 0, radius * 2, radius);
  return `url(${tile.toDataURL()})`;
}
