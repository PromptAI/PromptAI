module.exports = {
  apps: [
    {
      name: 'app',
      script: './bin/www',
      env: {
        NODE_ENV: 'production',
        PORT: process.env.PORT || 3123,
        API_SERVER: process.env.API_SERVER
      }
    }
  ]
};
