import React from 'react';
import { isEmpty } from 'lodash';
import { useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router';
import Token from '@/utils/token';

const OAuthComponent = () => {
  const history = useHistory();
  const location = useLocation();
  const [error, setError] = useState('');
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    if (searchParams.has('error') && !isEmpty(searchParams.has('error'))) {
      setError(searchParams.get('error') || 'OAuth Error');
    }
    if (searchParams.has('token') && !isEmpty(searchParams.get('token'))) {
      Token.setToken(searchParams.get('token') as string);
      localStorage.setItem('userStatus', 'login');
      history.replace('/');
    }
  }, [history, location.search]);
  return (
    <div className="flex justify-center items-center h-screen">
      <h1>{error}</h1>
    </div>
  );
};

export default OAuthComponent;
