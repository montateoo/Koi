// il WebSocket verso il server, sullo stesso indirizzo da cui arriva la pagina (wss:// se la pagina e' https).
// Ogni messaggio e' un oggetto JSON con il nome del record nel campo "type", come in messages.Json.
// Se la connessione cade riprova da sola, aspettando un po' di piu' a ogni tentativo
export class Connection {

  constructor({ onMessage, onOpen, onClose }) {
    this.onMessage = onMessage;
    this.onOpen = onOpen;
    this.onClose = onClose;
    this.retry = 0;
    this.socket = null;
  }

  connect() {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${location.host}/ws`);
    this.socket = socket;
    socket.addEventListener('open', () => {
      this.retry = 0;
      this.onOpen();
    });
    socket.addEventListener('message', (event) => {
      let message;
      try {
        message = JSON.parse(event.data);
      } catch {
        return;
      }
      this.onMessage(message);
    });
    socket.addEventListener('close', () => {
      if (this.socket !== socket) {
        return;
      }
      this.onClose();
      const delay = Math.min(10000, 500 * 2 ** this.retry++);
      setTimeout(() => this.connect(), delay);
    });
  }

  // chiude di proposito e riapre subito: niente avviso di connessione persa
  restart() {
    const old = this.socket;
    this.connect();
    old?.close();
  }

  get open() {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  // send('PlaceWater', { q: 1, r: -2 })  ->  {"type":"PlaceWater","q":1,"r":-2}
  send(type, fields = {}) {
    if (!this.open) {
      return false;
    }
    this.socket.send(JSON.stringify({ type, ...fields }));
    return true;
  }
}
