// import angular from './lib/angular';

// import config constants
import {API} from "./config-constant.js";

// import global app services
import {HostStorageService} from "./host-storage-service.js";

// import app components
import {StatusComponent} from "./status/status-component.js";

export const App = angular.module('App', [])
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
  .component('status', StatusComponent)
  .run(function () {
    console.log("App started");
  })
  .name;

angular.bootstrap(document.documentElement, [App]);