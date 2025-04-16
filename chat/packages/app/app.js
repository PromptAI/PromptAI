const createError = require('http-errors');
const express = require('express');
const path = require('path');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');

const indexRouter = require('./routes/ava');

const app = express();

if (process.env.NODE_ENV !== 'production') {
  app.use(
    '/chat',
    createProxyMiddleware('/chat', {
      target: process.env.API_SERVER || 'http://flow2.pcc.pub:8091/',
      changeOrigin: true,
      pathRewrite: {
        '^': ''
      }
    })
  );
  app.use(
    '/api',
    createProxyMiddleware('/api', {
      target: process.env.API_SERVER || 'http://flow2.pcc.pub:8091/',
      changeOrigin: true,
      pathRewrite: {
        '^': ''
      }
    })
  );
}
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(express.static(path.join(__dirname, 'public')));

app.get('/', (req, res) => res.render('index'));
app.use('/ava', indexRouter);

// catch 404 and forward to error handler
app.use(function (req, res, next) {
  next(createError(404));
});

// error handler
app.use(function (err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;
