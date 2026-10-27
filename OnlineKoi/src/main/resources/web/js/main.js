// il client web: schermate, messaggi del server e interazioni della stanza
import { installDefs } from './svg.js';
import { Connection } from './connection.js';
import { Pond, DQ, DR } from './pond.js';
import { Board, h, renderObjectives, renderPlayerPanel, scoreTable, summary } from './panels.js';
import { seal } from './art.js';
import { seigaiha, startSakura } from './sakura.js';

const $ = (selector, root = document) => root.querySelector(selector);

// ================================================================= impostazioni (restano nel browser)

const settings = {
  get(name, fallback) {
    try {
      return localStorage.getItem('koi.' + name) ?? fallback;
    } catch {
      return fallback;
    }
  },
  set(name, value) {
    try {
      localStorage.setItem('koi.' + name, value);
    } catch {
      // modalita' privata o archivio pieno: l'impostazione vale solo per questa visita
    }
  },
  get nickname() { return this.get('nickname', '').trim(); },
  get skipIntro() { return this.get('skipIntro', 'false') === 'true'; },
};

const MAX_NAME = 20;
const MAX_ROOM_NAME = 30;

// ================================================================= stato

const state = {
  tileNames: {},
  everConnected: false,
  rooms: [],
  selectedRoom: null,
  room: null,            // { id, name } della stanza in cui si e' entrati
  view: null,            // ultima GameView ricevuta
  ranking: null,         // classifica finale: null finche' la partita non e' finita
  selected: null,        // tessera scelta ("WATER", "MOVE:3"...), in attesa del clic su un esagono evidenziato
  rotationIndex: 0,      // quale delle rotazioni valide si sta guardando (tasto R)
  matches: [],
};

const nameOf = (type) => state.tileNames[type] || String(type).toLowerCase();

const connection = new Connection({
  onMessage: handle,
  onOpen: () => {
    const wasOffline = !$('.offline').hidden;
    $('.offline').hidden = true;
    if (wasOffline && state.room) {
      // il server non ricorda le connessioni: la stanza e' persa, si riparte dal menu
      state.room = null;
      showScreen('title');
      toast('Connessione ritrovata, ma la stanza è andata persa: rientra dal menu');
    } else if (wasOffline) {
      toast('Connessione ritrovata');
    }
    state.everConnected = true;
    if ($('#lobby').classList.contains('active')) {
      connection.send('ListRooms');
    }
  },
  onClose: () => {
    if (state.everConnected) {
      $('.offline').hidden = false;
    }
  },
});

// ================================================================= messaggi del server

function handle(message) {
  switch (message.type) {
    case 'Welcome':
      state.tileNames = message.tileNames;
      break;
    case 'RoomList':
      showRooms(message.rooms);
      break;
    case 'Joined':
      enterRoom(message.roomId, message.roomName);
      break;
    case 'Info':
      show(message.text, false);
      break;
    case 'ErrorMessage':
      show(message.text, true);
      break;
    case 'GameUpdate':
      showGame(message.view);
      break;
    case 'GameOver':
      showGameOver(message.ranking);
      break;
    case 'History':
      showHistory(message.matches);
      break;
    default:
      break;
  }
}

// messaggi e errori: nel registro della stanza, nella riga di stato di lobby e storico, altrove in un avviso
function show(text, error) {
  if ($('#room').classList.contains('active')) {
    const line = h('p', error ? 'error' : null, text);
    const log = $('.room-log');
    log.append(line);
    log.scrollTop = log.scrollHeight;
  } else if ($('#lobby').classList.contains('active')) {
    $('.lobby-status').textContent = text;
  } else if ($('#history').classList.contains('active')) {
    $('.history-status').textContent = text;
  } else {
    toast(text);
  }
}

let toastTimer;
function toast(text) {
  const node = $('.toast');
  node.textContent = text;
  node.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.classList.remove('show'), 4000);
}

function showScreen(id) {
  for (const screen of document.querySelectorAll('.screen')) {
    screen.classList.toggle('active', screen.id === id);
  }
  closeDialog();
}

// ================================================================= schermata iniziale

function setupTitle() {
  const title = $('#title');
  const refreshName = () => {
    $('.player-name', title).textContent = settings.nickname || 'Imposta il tuo nome';
  };
  refreshName();

  // nero -> laghetto, zoom lento, poi menu: un clic o un tasto la porta alla fine
  if (!settings.skipIntro) {
    title.classList.add('intro');
    const skip = () => title.classList.remove('intro');
    title.addEventListener('pointerdown', skip, { once: true });
    window.addEventListener('keydown', skip, { once: true });
    setTimeout(skip, 7500);
  }

  title.addEventListener('click', (event) => {
    const action = event.target.closest('[data-action]')?.dataset.action;
    switch (action) {
      case 'play':
        withNickname(openLobby);
        break;
      case 'create':
        withNickname(askRoomName);
        break;
      case 'history':
        openHistory();
        break;
      case 'settings':
        openSettings();
        break;
      case 'nickname':
        askNickname(null);
        break;
      default:
        break;
    }
  });

  // prima di giocare serve un nome: se manca lo si chiede e poi si prosegue con l'azione scelta
  function withNickname(action) {
    if (settings.nickname) {
      action();
    } else {
      askNickname(action);
    }
  }

  function askNickname(then) {
    const field = textField(settings.nickname, 'Il tuo nome', MAX_NAME);
    openDialog('Il tuo nome', 'Gli altri giocatori ti vedranno con questo nome.', [field], 'Salva', (error) => {
      const name = field.value.trim();
      if (!name) {
        return error('Scrivi un nome');
      }
      settings.set('nickname', name);
      refreshName();
      closeDialog();
      then?.();
      return true;
    });
  }

  function askRoomName() {
    const field = textField(`Stagno di ${settings.nickname}`.slice(0, MAX_ROOM_NAME), 'Nome della stanza', MAX_ROOM_NAME);
    openDialog('Crea Stanza', 'Gli altri potranno entrare dalla lista delle stanze.', [field], 'Crea e entra', (error) => {
      const name = field.value.trim();
      if (!name) {
        return error('Scrivi un nome per la stanza');
      }
      if (!connection.send('CreateAndJoinRoom', { name, nickname: settings.nickname })) {
        return error('Non sei collegato al server');
      }
      closeDialog();
      return true;
    });
    field.select();
  }

  function openSettings() {
    const check = h('input');
    check.type = 'checkbox';
    check.checked = settings.skipIntro;
    const label = h('label', 'check', check, "Salta l'animazione iniziale");
    openDialog('Impostazioni', null, [label], 'Salva', () => {
      settings.set('skipIntro', String(check.checked));
      closeDialog();
      return true;
    });
  }
}

function textField(value, placeholder, maxLength) {
  const field = h('input');
  field.type = 'text';
  field.value = value;
  field.placeholder = placeholder;
  field.maxLength = maxLength;
  field.autocomplete = 'off';
  return field;
}

// finestra di dialogo con il loto sull'angolo: onConfirm riceve error(text) e chiude da sola se va tutto bene
function openDialog(title, subtitle, content, confirmLabel, onConfirm) {
  const error = h('p', 'error');
  const showError = (text) => {
    error.textContent = text;
    return false;
  };
  const cancel = h('button', 'wood-button', 'Annulla');
  cancel.type = 'button';
  cancel.addEventListener('click', closeDialog);
  const confirm = h('button', 'wood-button', confirmLabel);
  const form = h('form', 'koi-panel', h('h2', null, title), subtitle ? h('p', 'muted', subtitle) : null,
    ...content, error, h('div', 'buttons', cancel, confirm));
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    onConfirm(showError);
  });
  const pad = h('img', 'lilypad');
  pad.src = 'assets/ui/lilypad.png';
  pad.alt = '';
  const lotus = h('img', 'lotus');
  lotus.src = 'assets/ui/lotus.png';
  lotus.alt = '';

  const layer = $('.dialog-layer');
  layer.replaceChildren(h('div', 'dialog', form, pad, lotus));
  layer.hidden = false;
  layer.onclick = (event) => {
    if (event.target === layer) {
      closeDialog();
    }
  };
  (content.find((node) => node.tagName === 'INPUT') || confirm).focus();
}

function closeDialog() {
  const layer = $('.dialog-layer');
  layer.hidden = true;
  layer.replaceChildren();
}

// ================================================================= lista delle stanze

function openLobby() {
  $('.lobby-player').textContent = `Giochi come ${settings.nickname}`;
  $('.lobby-status').textContent = '';
  state.selectedRoom = null;
  showScreen('lobby');
  connection.send('ListRooms');
}

function setupLobby() {
  const lobby = $('#lobby');
  lobby.addEventListener('click', (event) => {
    const action = event.target.closest('[data-action]')?.dataset.action;
    if (action === 'menu') {
      showScreen('title');
    } else if (action === 'refresh') {
      connection.send('ListRooms');
    } else if (action === 'join') {
      joinSelected();
    }
  });
  const list = $('.room-list', lobby);
  list.addEventListener('click', (event) => {
    const item = event.target.closest('li[data-id]');
    if (item) {
      // solo l'evidenziazione: ricostruire l'elenco qui impedirebbe il doppio clic
      state.selectedRoom = item.dataset.id;
      list.querySelectorAll('li').forEach((li) => li.classList.toggle('selected', li === item));
    }
  });
  list.addEventListener('dblclick', (event) => {
    if (event.target.closest('li[data-id]')) {
      joinSelected();
    }
  });
}

function showRooms(rooms) {
  state.rooms = rooms;
  if (!rooms.some((room) => room.id === state.selectedRoom)) {
    state.selectedRoom = rooms.find((room) => room.open)?.id ?? null;
  }
  renderRooms();
}

function renderRooms() {
  const list = $('.room-list');
  if (!state.rooms.length) {
    list.replaceChildren(h('li', 'empty', 'Nessuna stanza: creane una dal menu'));
    return;
  }
  list.replaceChildren(...state.rooms.map((room) => {
    const players = room.players.length ? room.players.join(', ') : 'nessuno';
    const item = h('li', (room.id === state.selectedRoom ? 'selected ' : '') + (room.open ? '' : 'closed'),
      h('div', 'room-name', room.name),
      h('div', 'muted', `${players}${room.open ? '' : '  ·  partita in corso'}`));
    item.dataset.id = room.id;
    return item;
  }));
}

function joinSelected() {
  const room = state.rooms.find((r) => r.id === state.selectedRoom);
  if (!room) {
    $('.lobby-status').textContent = 'Scegli una stanza dalla lista';
  } else if (!room.open) {
    $('.lobby-status').textContent = 'In questa stanza la partita è già cominciata';
  } else {
    connection.send('JoinRoom', { roomId: room.id, nickname: settings.nickname });
  }
}

// ================================================================= storico

function openHistory() {
  state.matches = [];
  $('.match-list').replaceChildren();
  $('.match-details').replaceChildren();
  $('.history-status').textContent = 'Carico lo storico…';
  showScreen('history');
  connection.send('ListHistory');
}

function setupHistory() {
  const history = $('#history');
  history.addEventListener('click', (event) => {
    if (event.target.closest('[data-action="menu"]')) {
      showScreen('title');
      return;
    }
    const item = event.target.closest('.match-list li[data-index]');
    if (item) {
      selectMatch(Number(item.dataset.index));
    }
  });
}

function showHistory(matches) {
  state.matches = matches;
  $('.history-status').textContent = matches.length ? `${matches.length} ${matches.length === 1 ? 'partita' : 'partite'}` : '';
  const list = $('.match-list');
  if (!matches.length) {
    list.replaceChildren(h('li', 'empty', 'Nessuna partita finita'));
    $('.match-details').replaceChildren(h('p', 'muted', 'Quando una partita finisce, la trovi qui con punteggi e stagni.'));
    return;
  }
  list.replaceChildren(...matches.map((match, i) => {
    const item = h('li', null, h('div', 'match-name', match.roomName),
      h('div', 'muted', `${match.endedAt}  ·  vince ${match.ranking[0].name}`));
    item.dataset.index = i;
    return item;
  }));
  selectMatch(0);
}

function selectMatch(index) {
  const match = state.matches[index];
  document.querySelectorAll('.match-list li').forEach((item, i) => item.classList.toggle('selected', i === index));
  const winner = match.ranking[0];
  const cards = h('div', 'pond-cards');
  for (const player of match.players) {
    const total = match.ranking.find((sheet) => sheet.name === player.name)?.total ?? 0;
    const pond = new Pond({ maxScale: 0.6, nameOf });
    cards.append(h('div', 'pond-card', h('strong', null, `${player.name}  ·  ${total} punti`), pond.svg));
    pond.show(player.pond);
  }
  $('.match-details').replaceChildren(
    h('div', null, h('h3', null, match.roomName),
      h('p', 'muted', `${match.endedAt}  ·  vince ${winner.name} con ${winner.total} punti`)),
    scoreTable(match.ranking, null),
    h('h3', 'panel-title', 'Gli stagni finali'),
    cards);
}

// ================================================================= stanza

const room = {
  board: null,
  pond: null,
  opponents: new Map(),   // nome -> Pond, riusati tra un aggiornamento e l'altro
};

function setupRoom() {
  const screen = $('#room');
  room.board = new Board($('.board', screen), (position) => {
    select(null);
    connection.send('DrawCard', { position });
  });
  room.pond = new Pond({ interactive: true, animated: true, nameOf });
  room.pond.onHexClicked = placeSelected;
  room.pond.onKoiClicked = (id) => select(`MOVE:${id}`);
  room.pond.ghostShape = ghostCells;
  $('.pond-host', screen).append(room.pond.svg);
  renderObjectives($('.objectives', screen), []);

  screen.addEventListener('click', (event) => {
    const action = event.target.closest('[data-action]')?.dataset.action;
    switch (action) {
      case 'start':
        connection.send('StartGame');
        break;
      case 'end-turn':
        select(null);
        connection.send('EndTurn');
        break;
      case 'scores':
        openScores();
        break;
      case 'home':
        goHome();
        break;
      case 'quit':
        quitRoom();
        break;
      case 'rotate':
        rotate();
        break;
      case 'cancel':
        select(null);
        break;
      case 'reset-view':
        room.pond.resetView();
        break;
      default:
        break;
    }
  });

  window.addEventListener('keydown', (event) => {
    if (!screen.classList.contains('active') || event.target.tagName === 'INPUT') {
      return;
    }
    if (event.key === 'r' || event.key === 'R') {
      rotate();
    } else if (event.key === 'Escape') {
      select(null);
    }
  });
}

function enterRoom(id, name) {
  state.room = { id, name };
  state.view = null;
  state.ranking = null;
  state.selected = null;
  room.opponents.clear();
  room.pond.previousBases = null;
  room.pond.previousTops = null;
  room.pond.resetView();
  room.pond.show([], []);
  room.board.previous = [];
  $('.board').replaceChildren(h('p', 'muted', 'Il tabellone compare quando inizia la partita.'));
  $('.player-panel').replaceChildren(h('div', 'me-name', settings.nickname), h('p', 'muted', 'In attesa degli altri giocatori.'));
  $('.opponents').replaceChildren();
  renderObjectives($('.objectives'), []);
  $('.room-log').replaceChildren();
  $('.room-title').textContent = name;
  $('.turn-status').textContent = 'In attesa che tutti premano Inizia';
  $('[data-action="start"]').hidden = false;
  $('[data-action="end-turn"]').hidden = false;
  $('[data-action="home"]').hidden = true;
  $('[data-action="scores"]').hidden = true;
  $('.question-layer').hidden = true;
  $('.scores-layer').hidden = true;
  showScreen('room');
  updateTools();
  updateHint();
}

// fine partita: si esce dalla stanza (la connessione resta) e si torna al menu
function goHome() {
  connection.send('Leave');
  state.room = null;
  showScreen('title');
}

function quitRoom() {
  const playing = state.view && !state.ranking;
  if (!playing) {
    goHome();
    return;
  }
  // il server non lascia uscire a partita in corso: chiudere la connessione blocca la partita per gli altri
  if (window.confirm('La partita è in corso: se esci gli altri non potranno continuare. Uscire comunque?')) {
    state.room = null;
    connection.restart();   // una connessione nuova: per il server e' un altro client, fuori da ogni stanza
    showScreen('title');
  }
}

const me = (view) => view.players.find((player) => player.name === view.viewer) || view.players[0];
const positionsFor = (type) => state.view?.playablePositions[type] || [];
const rotationsAt = (type, anchor) => state.view?.rotations[type]?.[`${anchor.q},${anchor.r}`] || [];
const isMoveKey = (key) => typeof key === 'string' && key.startsWith('MOVE:');
const koiIdOf = (key) => Number(key.slice(5));
const isMyTurn = (view) => view.viewer === view.activePlayer;

function showGame(view) {
  const turnChanged = !state.view || state.view.activePlayer !== view.activePlayer;
  if (view.question !== state.view?.question) {
    state.rotationIndex = 0;   // domanda nuova: si riparte dalla prima rotazione
  }
  state.view = view;
  $('[data-action="start"]').hidden = true;   // la partita e' cominciata
  if (state.selected && (!positionsFor(state.selected).length || placementQuestion().length)) {
    state.selected = null;
  }
  $('.turn-status').textContent = state.ranking ? 'Partita finita'
    : isMyTurn(view) ? 'Tocca a te' : `Tocca a ${view.activePlayer}`;
  room.board.update(view.board, view.canDraw);
  refreshPlayerPanel();
  renderObjectives($('.objectives'), view.objectives);
  refreshPond();
  refreshOpponents(view);
  refreshQuestion(view);
  updateHint();
  updateTools();
  if (turnChanged && view.activePlayer && !state.ranking) {
    showBanner(isMyTurn(view) ? 'Tocca a te' : `Tocca a ${view.activePlayer}`);
  }
}

function refreshPlayerPanel() {
  if (!state.view) {
    return;
  }
  renderPlayerPanel($('.player-panel'), me(state.view), new Set(Object.keys(state.view.playablePositions)), state.selected, {
    onSelect: select,
    onSpendLucky: () => connection.send('SpendLuckyCard'),
    canSpendLucky: isMyTurn(state.view) && !state.view.question && !state.ranking,
    nameOf,
  });
}

function refreshPond() {
  if (!state.view) {
    return;
  }
  const question = placementQuestion();
  // una domanda "dove piazzi?" ha la precedenza sulla selezione
  const spots = question.length ? uniqueAnchors(question) : state.selected ? positionsFor(state.selected) : [];
  // le carpe con almeno una destinazione si possono cliccare per spostarle
  const movable = question.length ? [] : Object.keys(state.view.playablePositions).filter(isMoveKey).map(koiIdOf);
  room.pond.setMovableKoi(movable, isMoveKey(state.selected) ? koiIdOf(state.selected) : -1);
  room.pond.show(me(state.view).pond, spots);
}

// la domanda aperta si risponde cliccando lo stagno (obiettivi, Percorso, Pioggia): un piazzamento per opzione,
// con l'esagono da cliccare, la rotazione e gli esagoni che la tessera occuperebbe. Vuota per le altre domande
function placementQuestion() {
  return state.view?.placements ?? [];
}

const sameHex = (a, b) => a.q === b.q && a.r === b.r;

function uniqueAnchors(placements) {
  const anchors = [];
  for (const placement of placements) {
    if (!anchors.some((anchor) => sameHex(anchor, placement.anchor))) {
      anchors.push(placement.anchor);
    }
  }
  return anchors;
}

// il piazzamento mostrato su quell'esagono: R (o "Ruota") passa alla rotazione successiva
function currentPlacement(anchor) {
  const here = placementQuestion().filter((placement) => sameHex(placement.anchor, anchor));
  return here.length ? here[((state.rotationIndex % here.length) + here.length) % here.length] : null;
}

const placementRotates = () => {
  const placements = placementQuestion();
  return uniqueAnchors(placements).length < placements.length;
};

function refreshOpponents(view) {
  const others = view.players.filter((player) => player.name !== view.viewer);
  $('.opponents').replaceChildren(...others.map((player) => {
    let pond = room.opponents.get(player.name);
    if (!pond) {
      pond = new Pond({ maxScale: 0.55, nameOf });
      room.opponents.set(player.name, pond);
    }
    const box = h('div', 'opponent', summary(player), pond.svg);
    pond.show(player.pond);
    return box;
  }));
}

function refreshQuestion(view) {
  const layer = $('.question-layer');
  // nessuna domanda, oppure una domanda di piazzamento che si risolve cliccando lo stagno: niente finestra
  if (!view.question || !view.options.length || placementQuestion().length) {
    layer.hidden = true;
    layer.replaceChildren();
    return;
  }
  const options = view.options.map((option, i) => {
    const button = h('button', null, option);
    button.addEventListener('click', () => {
      layer.hidden = true;
      connection.send('Choose', { option: i });
    });
    return button;
  });
  layer.replaceChildren(h('div', 'panel question',
    h('div', 'question-top', seal('問', 36, '#c8341f'), h('span', null, view.question)),
    h('div', 'question-options', ...options)));
  layer.hidden = false;
}

function updateHint() {
  const view = state.view;
  let text;
  if (state.ranking) {
    text = 'La partita è finita: "Classifica" mostra di nuovo i punteggi.';
  } else if (!view) {
    text = 'Premi Inizia quando ci sono tutti.';
  } else if (!isMyTurn(view)) {
    text = view.question ?? `Tocca a ${view.activePlayer}…`;
  } else if (placementQuestion().length) {
    text = `${view.question}   ·   clicca un esagono evidenziato` + (placementRotates() ? '   ·   R per ruotare' : '');
  } else if (view.question) {
    text = view.question;
  } else if (state.selected) {
    const action = isMoveKey(state.selected) ? 'spostare: ' : 'piazzare: ';
    const shape = shapeOf(state.selected);
    text = `Clicca un esagono evidenziato per ${action}${nameOf(shape)}`
      + (rotatable(shape) ? '   ·   R per ruotare' : '') + '   ·   Esc per annullare';
  } else {
    text = 'Medita: prendi una carta dal tabellone.   Lavora: scegli una tessera dal magazzino, o clicca una carpa per spostarla.   Poi passa il turno.';
  }
  $('.hint').textContent = text;
}

// pulsanti sopra lo stagno, per chi non ha la tastiera (telefono, tablet)
function updateTools() {
  $('[data-action="rotate"]').hidden = placementQuestion().length ? !placementRotates()
    : !(state.selected && rotatable(shapeOf(state.selected)));
  $('[data-action="cancel"]').hidden = !state.selected;
}

function showBanner(text) {
  const banner = $('.banner');
  banner.textContent = text;
  banner.classList.remove('show');
  void banner.offsetWidth;   // fa ripartire l'animazione
  banner.classList.add('show');
}

function showGameOver(ranking) {
  state.ranking = ranking;
  $('.turn-status').textContent = 'Partita finita';
  $('[data-action="start"]').hidden = true;
  $('[data-action="end-turn"]').hidden = true;
  $('[data-action="home"]').hidden = false;
  $('[data-action="scores"]').hidden = false;
  select(null);
  openScores();
}

function openScores() {
  const ranking = state.ranking;
  if (!ranking) {
    return;
  }
  const viewer = state.view?.viewer ?? settings.nickname;
  const winner = ranking[0];
  const layer = $('.scores-layer');
  const look = h('button', 'wood-button', 'Guarda lo stagno');
  look.addEventListener('click', () => { layer.hidden = true; });
  const home = h('button', 'wood-button', 'Menu');
  home.addEventListener('click', goHome);
  layer.replaceChildren(h('div', 'koi-panel scores',
    h('div', 'scores-top', h('span', 'kanji', '勝'),
      h('div', null, h('h2', null, winner.name === viewer ? 'Hai vinto!' : `Vince ${winner.name}!`), h('p', 'muted', `${winner.total} punti`))),
    scoreTable(ranking, viewer),
    h('p', 'muted note', "In caso di parità vince chi ha più monete, poi chi giocava dopo nell'ordine di turno."),
    h('div', 'buttons', look, home)));
  layer.hidden = false;
}

// ================================================================= selezione e piazzamento

function select(type) {
  state.selected = (type == null || type === state.selected) ? null : type;
  state.rotationIndex = 0;
  refreshPlayerPanel();
  refreshPond();
  updateHint();
  updateTools();
}

function rotate() {
  if (!state.selected && !placementRotates()) {
    return;
  }
  state.rotationIndex++;
  room.pond.refreshGhost();
}

// tipo di tessera di cui disegnare la forma: per uno spostamento, la taglia della carpa scelta
function shapeOf(type) {
  if (!isMoveKey(type) || !state.view) {
    return type;
  }
  const id = koiIdOf(type);
  return me(state.view).pond.find((cell) => cell.topId === id)?.top ?? 'SMALLKOI';
}

const rotatable = (type) => ['BORDER', 'MEDIUMKOI', 'BIGKOI'].includes(type);

function currentRotation(anchor) {
  const rotations = rotationsAt(state.selected, anchor);
  return rotations.length ? rotations[((state.rotationIndex % rotations.length) + rotations.length) % rotations.length] : 0;
}

const neighbour = (coord, direction) => {
  const d = ((direction % 6) + 6) % 6;
  return { q: coord.q + DQ[d], r: coord.r + DR[d] };
};

// stesse forme di Pond: sponda = centro + ali in 4 e 0; carpa media = +0; carpa grande = +0 e +1 (ruotate)
function ghostCells(anchor) {
  if (placementQuestion().length) {
    return currentPlacement(anchor)?.cells ?? [anchor];   // forma calcolata dal server
  }
  if (!state.selected || !state.view) {
    return [anchor];
  }
  const rotation = currentRotation(anchor);
  switch (shapeOf(state.selected)) {
    case 'BORDER':
      return [anchor, neighbour(anchor, 4 + rotation), neighbour(anchor, rotation)];
    case 'MEDIUMKOI':
      return [anchor, neighbour(anchor, rotation)];
    case 'BIGKOI':
      return [anchor, neighbour(anchor, rotation), neighbour(anchor, rotation + 1)];
    default:
      return [anchor];
  }
}

function placeSelected(coord) {
  // domanda di piazzamento aperta: il clic sceglie l'opzione con quell'esagono e la rotazione in anteprima
  if (placementQuestion().length) {
    const placement = currentPlacement(coord);
    if (placement) {
      state.rotationIndex = 0;
      connection.send('Choose', { option: placement.option });
    }
    return;
  }
  if (!state.selected || !state.view) {
    return;
  }
  const type = state.selected;
  const { q, r } = coord;
  const angle = currentRotation(coord);
  select(null);
  if (isMoveKey(type)) {
    connection.send('MoveKoi', { koiId: koiIdOf(type), q, r, angle });
    return;
  }
  switch (type) {
    case 'WATER': connection.send('PlaceWater', { q, r }); break;
    case 'LEAF': connection.send('PlaceLeafs', { q, r }); break;
    case 'LILY': connection.send('PlaceLily', { q, r }); break;
    case 'LANTERN': connection.send('PlaceLantern', { q, r }); break;
    case 'BORDER': connection.send('PlaceBorder', { q, r, angle }); break;
    case 'SMALLKOI': connection.send('PlaceKoi', { size: 0, q, r, angle: 0 }); break;
    case 'MEDIUMKOI': connection.send('PlaceKoi', { size: 1, q, r, angle }); break;
    case 'BIGKOI': connection.send('PlaceKoi', { size: 2, q, r, angle }); break;
    default: break;
  }
}

// ================================================================= avvio

installDefs();
document.documentElement.style.setProperty('--seigaiha', seigaiha('#f6efdc', 'rgba(108,195,207,0.15)'));
startSakura($('.sakura'));
setupTitle();
setupLobby();
setupHistory();
setupRoom();
window.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && !$('.dialog-layer').hidden) {
    closeDialog();
  }
});
connection.connect();
