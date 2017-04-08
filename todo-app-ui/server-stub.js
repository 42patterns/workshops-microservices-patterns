var express = require('express')
var path = require('path')
var bodyParser = require('body-parser')
var app = express()

app.use(bodyParser.json())
app.use(express.static(path.join(__dirname, process.env.NODE_ENV || './dist')));

app.get('/*', function (req, res) {
    console.log('GET req: ' + req.protocol + '://' + req.get('host') + req.originalUrl)
    console.log('Headers: ' + JSON.stringify(req.headers, null, ' '));
    console.log('------');
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(
        [
            {"id": 1, "title": "First todo", "completed": false},
            {"id": 2, "title": "Second todo - which is done", "completed": true},
            {"id": 3, "title": "Not done todo", "completed": false}
        ]
    ));
})

app.post('/*', function (req, res) {
    console.log(req.method + ' req: ' + req.protocol + '://' + req.get('host') + req.originalUrl)
    console.log('Headers: ' + JSON.stringify(req.headers, null, ' '));
    console.log("  request body: " + JSON.stringify(req.body));
    console.log('------');
    var echo = req.body
    echo.id = Math.floor(Math.random() * 100) + 1
    res.setHeader("Location", "http://localhost:3000/api/todos/"+echo.id);
    res.send(echo)
    res.status(201)
    res.end()
});

app.all('/*', function (req, res) {
    console.log(req.method + ' req: ' + req.protocol + '://' + req.get('host') + req.originalUrl)
    console.log('Headers: ' + JSON.stringify(req.headers, null, ' '));
    console.log("  request body: " + JSON.stringify(req.body));
    console.log('------');
    var echo = req.body
    res.send(echo)
    res.status(201)
    res.end()
});

var server = app.listen(3000, function () {
    var host = server.address().address
    var port = server.address().port

    console.log('Example app listening at http://%s:%s', host, port)

})
