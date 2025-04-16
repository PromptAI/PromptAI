import React, { useEffect } from 'react';

import PwdForm from './pwd';
import EntryLayout from '@/pages/components/entry-layout';

const Login: React.FC = () => {
  useEffect(() => {
    const state = localStorage.getItem('userStatus');
    if (state === 'login') {
      setTimeout(() => (location.href = '/'));
    }
  }, []);

  return (
    <EntryLayout>
      <PwdForm />
    </EntryLayout>
  );
};
export default Login;
