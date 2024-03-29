export class ApiDemoController {
  constructor(API, HostStorageService, MessageBusService) {
    'ngInject';

    this.API = API;
    this.hostStorageService = HostStorageService;
    this.messageBusService = MessageBusService;

      this.dbName = API.HOST_DB_NAME;
    this.key = "demo-key-" + Math.floor(Math.random() * Math.floor(100) + 1);
  }

  $onInit() {
    this.getPlayers();
    this.messageBusService.on("key-update", (event, data) => {
      this.getPlayers();
    });
  }

  $onDestroy() {

  }

  getPlayers(fields) {
    this.hostStorageService.get(this.key, 0, -1, fields).then(data => {
      this.error = null;
      this.players = (!Array.isArray(data) && data !== null) ? [data] : data; // players should always be an array or null
      this.countPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  setPlayers(obj) {
    const players = obj ||
      {
        name: "Mike G.",
        status: "OK"
      };
    this.hostStorageService.set(this.key, players).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  setDate() {
    const players = new Date();
    this.hostStorageService.set(this.key, players).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  addPlayers() {
    const players = [
      {
        name: "Mike G.",
        status: "OK"
      },
      {
        name: "Stephan R.",
        status: "Good"
      },
      {
        name: "Ian S.",
        status: "So-so"
      }];
    this.hostStorageService.add(this.key, players).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  deletePlayers() {
    this.hostStorageService.delete(this.key).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  countPlayers() {
    this.hostStorageService.count(this.key).then(data => {
      this.error = null;
      this.count = data;
    }).catch(err => {
      this.error = err;
    })
  }

  badRequest() {
    this.hostStorageService.update(this.key).then(data => {
      this.error = null;
    }).catch(err => {
      this.error = err;
    })
  }

  updatePlayer(index, player) {
    if(player.status === "Good") {
      player.status = "OK"
    } else if (player.status === "OK") {
      player.status = "So-so";
    } else if (player.status === "So-so") {
      player.status = "Good";
    }

    this.hostStorageService.update(this.key, index, player).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  deletePlayer(player) {
    this.hostStorageService.delete(this.key, player).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }
}