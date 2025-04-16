/* eslint-disable @typescript-eslint/no-var-requires */
const path = require('path');
const {
  override,
  addWebpackModuleRule,
  addWebpackPlugin,
  addWebpackAlias,
  overrideDevServer,
} = require('customize-cra');
const ArcoWebpackPlugin = require('@arco-plugins/webpack-react');
const addLessLoader = require('customize-cra-less-loader');
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
const setting = require('./src/settings.json');

const MONACO_DIR = path.resolve(__dirname, './node_modules/monaco-editor');

module.exports = {
  webpack: override(
    addLessLoader({
      lessLoaderOptions: {
        lessOptions: {},
      },
    }),
    addWebpackModuleRule({
      test: /\.svg$/,
      loader: '@svgr/webpack',
    }),
    addWebpackModuleRule({
      test: /\.css$/,
      include: MONACO_DIR,
      use: ['style-loader', 'css-loader'],
    }),
    addWebpackPlugin(
      new ArcoWebpackPlugin({
        theme: '@arco-themes/react-arco-pro',
        modifyVars: {
          'arcoblue-6': setting.themeColor,
        },
      })
    ),
    addWebpackPlugin(new MonacoWebpackPlugin()),
    addWebpackAlias({
      '@': path.resolve(__dirname, 'src'),
      '@pcom': path.resolve(__dirname, 'src/pages/projects/project/components'),
    })
  ),
  devServer: overrideDevServer((config) => ({
    ...config,
    client: { overlay: false },
  })),
};
