const express = require('express');
const path = require('path');
const fs = require('fs');
const ejs = require('ejs');
const base64 = require('js-base64');
const axios = require('axios');
const qs = require('qs');

const template = `!function(){Chatbot.initialize({config:"<%= settings.config %>",server:"<%= settings.server %>",minimize:<%= settings.minimize %>,},"<%= slots %>","<%= variables %>","<%= theme %>",);}();`;
const router = express.Router();

const isDev = process.env.NODE_ENV === 'development';

router.get('/chatbot.app', async function (req, res, next) {
  const { query } = req;
  const missing = ['config'].filter((key) => !(key in query));
  if (missing.length === 0) {
    let slots = '[]';
    let variables = '[]';
    let theme = 'default';
    // 是否开启最小化功能
    let minimize = false;
    try {
      const config = qs.parse(base64.decode(query['config']));
      const { data } = await axios({
        url: `${process.env.API_SERVER}/chat/api/chat/settings`,
        method: 'get',
        headers: {
          'X-published-project-id': config.id,
          'X-published-project-token': config.token
        }
      });
      slots = qs.stringify(data?.slots || [], { indices: true });
      variables = qs.stringify(data?.variables || [], { indices: true });
      theme = data?.theme || 'default';
      minimize = data?.minimize ?? false;
    } catch (error) {
      console.log('get config is error', error);
    }
    const settings = {
      ...req.query,
      minimize: minimize,
      server: isDev ? `http://localhost:3000` : `//${req.headers.host}`
    };
    const bufferRUN = Buffer.from(
      ejs.render(template, {
        settings,
        slots: base64.encode(slots),
        variables: base64.encode(variables),
        theme
      }),
      'utf-8'
    );
    const bufferSDK = fs.readFileSync(path.resolve(__dirname, '../views/sdk.js'));
    res.set('Content-Type', 'application/javascript');
    res.send(Buffer.concat([bufferSDK, bufferRUN]));
  } else {
    res.status(400);
    res.send(`Missing key configuration: ${missing.join(', ')}`);
  }
});

module.exports = router;
