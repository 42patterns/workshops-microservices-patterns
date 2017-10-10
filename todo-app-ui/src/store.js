import {emptyItemQuery, Item, ItemList, ItemQuery, ItemUpdate} from "./item";
const {recorder} = require('./zipkinjs/recorder')

export default class Store {
    /**
     * @param {!string} name Database name
     * @param {function()} [callback] Called when the Store is ready
     */
    constructor(name, callback) {
        // setup tracer
        const {Tracer, ExplicitContext, HttpHeaders} = require('zipkin');

        const tracer = new Tracer({
            ctxImpl: new ExplicitContext(),
            recorder
        });

        // instrument fetch
        const wrapFetch = require('zipkin-instrumentation-fetch');
        const zipkinFetch = wrapFetch(fetch, {tracer, serviceName: 'browser'});

        const url = '/api/todos';

        (() => {
            zipkinFetch(url).then(function (response) {
                var contentType = response.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    return response.json();
                }
                console.error("Oops, we haven't got JSON!");
                return JSON.parse('[]');
            }).then(function (json) {
                liveTodos = json;
                window.dispatchEvent(new Event('data-loaded'));
            });
        })();

        /**
         * @type {ItemList}
         */
        let liveTodos;

        /**
         * Read the local ItemList from localStorage.
         *
         * @returns {ItemList} Current array of todos
         */
        this.getLocalStorage = () => {
            return liveTodos || JSON.parse('[]');
        };

        /**
         * Write the local ItemList to localStorage.
         *
         * @param {ItemList} todos Array of todos to write
         */
        this.setLocalStorage = (todos) => {
            liveTodos = todos;
        };

        /**
         * Triger update operation to server
         *
         * @param {ItemUpdate} todo A todo to update
         */
        this.triggerUpdate = (todo) => {
            let headers = new Headers();
            headers.append("Content-Type", "application/json");
            headers.append(HttpHeaders.TraceId, tracer.id.traceId);
            headers.append(HttpHeaders.SpanId, tracer.id.spanId);
            headers.append(HttpHeaders.Sampled, tracer.id.spanId);

            zipkinFetch(url, {
                method: 'PUT',
                body: JSON.stringify(todo),
                headers: {
                    "Content-type": "application/json"
                }
            }).then(function (resp) {
                console.log("TODO updated: " + resp.status);
            });
        }

        /**
         * Triger insert operation to server
         *
         * @param {Item} todo A todo to insert
         */
        this.triggerInsert = (todo) => {
            zipkinFetch(url, {
                method: 'POST',
                body: JSON.stringify(todo),
                headers: {
                    "Content-type": "application/json"
                }
            }).then(function (resp) {
                console.log("TODO crated: " + resp.headers.get('location'));
            });
        }

        /**
         * Triger remove operation to server
         *
         * @param {Item} todo A todo to remove
         */
        this.triggerDelete = (todo) => {
            zipkinFetch(url + '/'+ todo.id, {
                method: 'DELETE'
            }).then(function (resp) {
                console.log("TODO removed: " + resp.status);
            });
        }


        if (callback) {
            callback();
        }
    }

    /**
     * Find items with properties matching those on query.
     *
     * @param {ItemQuery} query Query to match
     * @param {function(ItemList)} callback Called when the query is done
     *
     * @example
     * db.find({completed: true}, data => {
	 *	 // data shall contain items whose completed properties are true
	 * })
     */
    find(query, callback) {
        const todos = this.getLocalStorage();
        let k;

        callback(todos.filter(todo => {
            for (k in query) {
                if (query[k] !== todo[k]) {
                    return false;
                }
            }
            return true;
        }));
    }

    /**
     * Update an item in the Store.
     *
     * @param {ItemUpdate} update Record with an id and a property to update
     * @param {function()} [callback] Called when partialRecord is applied
     */
    update(update, callback) {
        const id = update.id;
        const todos = this.getLocalStorage();
        let i = todos.length;
        let k;

        while (i--) {
            if (todos[i].id === id) {
                for (k in update) {
                    todos[i][k] = update[k];
                }
                this.triggerUpdate(update);
                break;
            }
        }

        this.setLocalStorage(todos);

        if (callback) {
            callback();
        }
    }

    /**
     * Insert an item into the Store.
     *
     * @param {Item} item Item to insert
     * @param {function()} [callback] Called when item is inserted
     */
    insert(item, callback) {
        const todos = this.getLocalStorage();
        todos.push(item);
        this.triggerInsert(item);
        this.setLocalStorage(todos);

        if (callback) {
            callback();
        }
    }

    /**
     * Remove items from the Store based on a query.
     *
     * @param {ItemQuery} query Query matching the items to remove
     * @param {function(ItemList)|function()} [callback] Called when records matching query are removed
     */
    remove(query, callback) {
        let k;

        const todos = this.getLocalStorage().filter(todo => {
            for (k in query) {
                if (query[k] !== todo[k]) {
                    return true;
                }
            }
            this.triggerDelete(todo);
            return false;
        });

        this.setLocalStorage(todos);

        if (callback) {
            callback(todos);
        }
    }

    /**
     * Count total, active, and completed todos.
     *
     * @param {function(number, number, number)} callback Called when the count is completed
     */
    count(callback) {
        this.find(emptyItemQuery, data => {
            const total = data.length;

            let i = total;
            let completed = 0;

            while (i--) {
                completed += (data[i].completed?1:0);
            }

            callback(total, total - completed, completed);
        });
    }
}
