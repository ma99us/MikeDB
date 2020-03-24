export class HostStorageService {
  constructor($http, $q, API) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;
    this.API = API;

    $http.defaults.headers.common.API_KEY = this.API.HOST_API_KEY;
  }

  /**
   * Validates HTTP result code, and resolves async request promise.
   * (not for public use)
   */
  validateResponse(deferred, response) {
    if (response && (response.status == 200 || response.status == 201)) {
      deferred.resolve(response.data);  // resource exists or was created
    }
    else if (response && (response.status == 204)) {
      deferred.resolve(null);   // resource is empty
    }
    else if (response) {
      let message = 'Http error: (' + response.status + ') ' + response.statusText;
      deferred.reject(message);
    }
    else {
      deferred.reject('No response');
    }
  }

  /**
   * Select appropriate request Media Type header based on Value type
   * (not for public use)
   */
  prepareHeaders(value) {
    if (typeof value === 'string' && value !== null) {
      return {
        headers: {
          'Content-Type': 'text/plain',
        }
      };
    } else {
      return {
        headers: {
          'Content-Type': 'application/json',
        }
      };
    }
  }

  /**
   * Stores new key=>value pair
   * @returns {*} status code 201 when inserted successfully
   */
  set(key, value) {
    const deferred = this.$q.defer();
    this.$http.put(this.API.HOST_STORAGE_DB + key, value, this.prepareHeaders(value)).then(response => {
      this.validateResponse(deferred, response);
    }, err => {
      this.validateResponse(deferred, err);
    });
    return deferred.promise;
  }

  /**
   * Stores new key=>value pair, or adds to existing value if it is a collection
   * @returns {*} status code 201 when inserted successfully
   */
  add(key, value) {
    const deferred = this.$q.defer();
    if (!Array.isArray(value)) {
      value = [value];
    }
    this.$http.post(this.API.HOST_STORAGE_DB + key, value, this.prepareHeaders(value)).then(response => {
      this.validateResponse(deferred, response);
    }, err => {
      this.validateResponse(deferred, err);
    });
    return deferred.promise;
  }

  /**
   * Retrive Object, Primitive or a Collection assosiated with given Key
   * @param key
   * @param firstResult (optional) index of the first element in resulting collection to retrieve
   * @param maxResults (optional) number of elements from resulting collection to retrieve
   * @returns {*} 200 if record retrieved or status code 204 when no such record
   */
  get(key, firstResult = 0, maxResults = -1) {
    const deferred = this.$q.defer();
    this.$http.get(this.API.HOST_STORAGE_DB + key, {params: {firstResult: firstResult, maxResults: maxResults}}).then(response => {
      this.validateResponse(deferred, response);
    }, err => {
      this.validateResponse(deferred, err);
    });
    return deferred.promise;
  }

  /**
   * Count how many items associated with given Key
   * @param key
   * @returns {*} 1- for simple key=>value pairs, collection size for key=>[collection]
   */
  count(key) {
    const deferred = this.$q.defer();
    this.$http.head(this.API.HOST_STORAGE_DB + key).then(response => {
      response.data = response.headers('Content-Length');
      this.validateResponse(deferred, response);
    }, err => {
      this.validateResponse(deferred, err);
    });
    return deferred.promise;
  }

  /**
   * Delete a record with given Key
   */
  delete(key) {
    const deferred = this.$q.defer();
    this.$http.delete(this.API.HOST_STORAGE_DB + key).then(response => {
      this.validateResponse(deferred, response);
    }, err => {
      this.validateResponse(deferred, err);
    });
    return deferred.promise;
  }

  /**
   * Method Not Allowed
   */
  patch(key) {
    const deferred = this.$q.defer();
    this.$http.patch(this.API.HOST_STORAGE_DB + key).then(response => {
      this.validateResponse(deferred, response);
    }, err => {
      this.validateResponse(deferred, err);
    });
    return deferred.promise;
  }
}