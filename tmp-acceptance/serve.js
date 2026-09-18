const http = require('http');
const fs = require('fs');
const path = require('path');
const root = __dirname + '\\site';
const types = { '.html': 'text/html; charset=utf-8' };
http.createServer((req, res) => {
  const name = req.url === '/' ? 'index.html' : req.url.replace(/^\//, '');
  const file = path.join(root, name);
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); res.end('not found'); return; }
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  });
}).listen(8123, () => console.log('serving on 8123'));
