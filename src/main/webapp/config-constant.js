export const API = new function() {
  this.HOST_LOCATION = '/mike-db/api';
  this.HOST_API_KEY = '5up3r53cr3tK3y';
  this.HOST_DB_NAME = ':memory:testDB';
  this.HOST_STORAGE_URL = this.HOST_LOCATION + '/' + this.HOST_DB_NAME + '/'
};