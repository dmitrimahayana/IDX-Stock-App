var express = require('express');
var router = express.Router();

/* testAPI segment. */
router.get('/', function(req, res, next) {
    res.send('Express API is working properly...');
});

module.exports = router;