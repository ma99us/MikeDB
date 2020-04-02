export class WebsocketController {
  constructor(API, $location, $websocket, HostStorageService, MessageBusService) {
    'ngInject';

    this.API = API;
    this.$location = $location;
    this.$websocket = $websocket;
    this.hostStorageService = HostStorageService;
    this.messageBusService = MessageBusService;

    this.dbName = API.HOST_DB_NAME;
    this.url = this.getProtocol(this.$location) + '://' + this.$location.host() + ':' + this.$location.port() + this.API.HOST_WEBSOCKET_URL;
  }

  $onInit() {
    // Open a WebSocket connection
    this.messages = [];
    this.connect();
  }

  $onDestroy() {
   this.disconnect();
  }

  getProtocol(location){
    return 'ws' + (location.protocol() === 'https' ? 's' : '');
  }

  connect() {
    if (this.dataStream) {
      return;
    }
    this.dataStream = this.$websocket(this.url);
    this.dataStream.onMessage(message => {
      this.onMessage(message.data)
    }).onOpen(() => {
      this.messages.push("--- socket opened");
      this.send({API_KEY: this.API.HOST_API_KEY});  // got to send API_KEY first thing otherwise socket will be closed
    }).onClose(() => {
      this.messages.push("--- socket closed");
      this.dataStream = null;
    }).onError(err => {
      this.messages.push("--- error:" + err);
    });
  }

  disconnect(){
    if (!this.dataStream) {
      return;
    }
    this.dataStream.close();
  }

  send(value) {
    if (!this.dataStream) {
      return;
    }
    this.dataStream.send(value);  // JSON.stringify({action: value})
  }

  onMessage(message) {
    let event = this.tryJson(message);
    if (!event || typeof event !== "object") {
      this.messages.push(message);
      return;
    }

    if (event.event === "OPENED" && event.sessionId) {
      this.hostStorageService.session = event.sessionId;
      this.messages.push("--- new session id: \"" + event.sessionId + "\"");
    } else if (this.hostStorageService.session !== event.sessionId) {
      this.messages.push(message);
      this.notify(event.key);
    }
  }

  tryJson(str) {
    try {
      return JSON.parse(str);
    } catch (e) {
      return str;
    }
  }

  notify(key) {
    this.messageBusService.broadcast('key-update', key);
  }
}