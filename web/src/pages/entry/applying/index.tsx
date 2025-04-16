import React, { useEffect, useMemo } from 'react';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { Link } from '@arco-design/web-react';
import { useHistory } from 'react-router-dom';

import Form from './form';
import { IconEmail } from '@arco-design/web-react/icon';
import EntryLayout from '@/pages/components/entry-layout';

function Applying() {
  const t = useLocale(i18n);
  const history = useHistory();
  const types = useMemo(
    () => [
      {
        type: 'applying',
        name: t['applying.form.sub'],
        icon: <IconEmail />,
      },
    ],
    [t]
  );
  useEffect(() => {
    const state = localStorage.getItem('userStatus');

    if (state === 'login') {
      setTimeout(() => (location.href = '/'));
    }
  }, []);
  return (
    <EntryLayout
      title={t['applying.form.title']}
      types={types}
      value="applying"
    >
      <Form />
      <div style={{ textAlign: 'center', marginTop: 16, width: '100%' }}>
        <span>{t['applying.applyingFor.placeholder']}</span>
        <span style={{ margin: '0 5px', whiteSpace: 'nowrap' }}>
          <Link onClick={() => history.push('/register')}>
            {t['applying.applyingFor.register']}
          </Link>
          <span style={{ margin: '0 5px' }}>
            {t['applying.applyingFor.concat']}
          </span>
          <Link onClick={() => history.push('/login')}>
            {t['applying.applyingFor.signIn']}
          </Link>
        </span>
      </div>
    </EntryLayout>
  );
}

Applying.displayName = 'Applying';

export default Applying;
