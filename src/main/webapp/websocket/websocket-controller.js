export class WebsocketController {
  constructor(API, $location, $websocket) {
    'ngInject';

    this.API = API;
    this.$location = $location;
    this.$websocket = $websocket;

    this.dbName = API.HOST_DB_NAME;
    this.url = this.getProtocol(this.$location) + '://' + this.$location.host() + ':' + this.$location.port() + this.API.HOST_WEBSOCKET_URL;
  }

  $onInit() {
    // Open a WebSocket connection
    this.messages = [];
  }

  $onDestroy() {
   this.disconnect();
  }

  getProtocol(location){
    return 'ws' + (location.protocol() === 'https' ? 's' : '');
  }

  connect() {
    this.dataStream = this.$websocket(this.url);
    this.dataStream.onMessage(message => {
      this.messages.push(message.data);
    }).onOpen(() => {
      this.messages.push("--- socket opened");
      this.send({API_KEY: this.API.HOST_API_KEY});
    }).onClose(() => {
      this.messages.push("--- socket closed");
    }).onError(err => {
      this.messages.push("--- error:" + err);
    });
  }

  disconnect(){
    this.dataStream.close();
  }

  send(value) {
    this.dataStream.send(value);  // JSON.stringify({action: value})
  }
}