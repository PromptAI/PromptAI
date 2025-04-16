import React from 'react';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { Typography } from '@arco-design/web-react';
import { useHistory } from 'react-router-dom';

import Form from './form';
import EntryLayout from '@/pages/components/entry-layout';

const Register: React.FC = () => {
  const t = useLocale(i18n);
  const history = useHistory();
  return (
    <EntryLayout>
      <Form />
      <div style={{ marginTop: 32 }}>
        <Typography.Text>
          {t['register.signIn.placeholder']}
          <Typography.Text
            style={{
              marginLeft: 16,
              textDecorationLine: 'underline',
              cursor: 'pointer',
            }}
            type="primary"
            onClick={() => history.push('/login')}
          >
            {t['register.signIn']}
          </Typography.Text>
        </Typography.Text>
      </div>
    </EntryLayout>
  );
};

export default Register;
