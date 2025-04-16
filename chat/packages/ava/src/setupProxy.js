const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
  app.use(
    createProxyMiddleware('/api', {
      target: process.env.API_SERVER || 'http://flow2.pcc.pub:8091/',
      changeOrigin: true,
      pathRewrite: {
        '^': ''
      }
    })
  );
  app.use(
    createProxyMiddleware('/chat', {
      target: process.env.API_SERVER || 'http://flow2.pcc.pub:8091/',
      changeOrigin: true,
      pathRewrite: {
        '^': ''
      }
    })
  );
};
