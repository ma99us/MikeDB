//import './lib/angular/angular.js';
//import './lib/angular-websocket/dist/angular-websocket.js';

// import config constants
import {API} from "./config-constant.js";

// import global app services
import {HostStorageService} from "./services/host-storage-service.js";
import {MessageBusService} from "./services/message-bus-service.js";

// import app components
import {ApiDemoComponent} from "./api-demo/api-demo-component.js";
import {WebsocketComponent} from "./websocket/websocket-component.js";

export const App = angular.module('App', ['ngWebSocket'])
  .config(function ($sceDelegateProvider) {
    'ngInject';

    $sceDelegateProvider.resourceUrlWhitelist([
      // Allow same origin resource loads.
      'self'
      // Allow loading from our assets domain.  Notice the difference between * and **.
    ]);
  })
  .constant('API', API)
  .service('HostStorageService', HostStorageService)
  .service('MessageBusService', MessageBusService)
  .component('apiDemo', ApiDemoComponent)
  .component('websocket', WebsocketComponent)
  .run(function () {
    console.log("App started");
  })
  .name;

angular.bootstrap(document.documentElement, [App]);