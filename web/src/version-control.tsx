import nProgress from 'nprogress';
import React, { Fragment } from 'react';
import { Prompt } from 'react-router';
import { get } from './utils/request';

const isPro = process.env.NODE_ENV === 'production';
const VersionControl = ({ children }) => {
  const handleCheck = () => {
    nProgress.start();
    isPro &&
      get('/version.json').then((data) => {
        const localVersion =
          window.localStorage.getItem('prompt.version') || '';
        if (data.version + '' !== localVersion) {
          window.localStorage.setItem('prompt.version', data.version + '');
          window.location.reload();
        }
      });
    nProgress.done();
    return true;
  };

  return (
    <Fragment>
      {children}
      <Prompt when message={handleCheck} />
    </Fragment>
  );
};

export default VersionControl;
