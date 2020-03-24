export class StatusController {
  constructor(HostStorageService) {
    'ngInject';

    this.hostStorageService = HostStorageService;
  }

  $onInit() {
    this.getPlayers();
  }

  $onDestroy() {

  }

  getPlayers() {
    this.hostStorageService.get("players").then(data => {
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
    this.hostStorageService.set("players", players).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  setDate() {
    const players = new Date();
    this.hostStorageService.set("players", players).then(data => {
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
    this.hostStorageService.add("players", players).then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  deletePlayers() {
    this.hostStorageService.delete("players").then(data => {
      this.error = null;
      this.getPlayers();
    }).catch(err => {
      this.error = err;
    })
  }

  countPlayers() {
    this.hostStorageService.count("players").then(data => {
      this.error = null;
      this.count = data;
    }).catch(err => {
      this.error = err;
    })
  }

  badRequest() {
    this.hostStorageService.patch("players").then(data => {
      this.error = null;
      this.count = data;
    }).catch(err => {
      this.error = err;
    })
  }
}