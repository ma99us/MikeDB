# MikeDB

## Simple no-nonsense No-SQL database with a REST API. 

So... I was board while self-isolating, and wrote my own No-SQL database.
It is basically stores Key => Value pairs where Key is a string and Value could be any object, collections of objects, or primitive.
It has a REST API which you can use from your Javascript apps (CORS disabled!). 
APPLICATION/JSON Media Type is used for all requests (except when working with plain string, then TEXT/PLAIN is used).

#### Usage is super simple: 
To add objects assosiated with a key to your own database do a PUT request similar to this: 
##### `<your host>/mike-db/testDB/key1`
where `testDB` - is the name of your dynamically created database, and `key1` is any string key.
Send any json object as payload of the request.
To retrieve stored value do a GET request to the same url.
To delete a key send it with DELETE request.
To get a number of items associated with a single key, send a HEAD request and check response Content-Length header.

Don't forget to set a super secret API KEY header to all your requests:
`API_KEY: 5up3r53cr3tK3y`