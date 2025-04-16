import { getLoginSmsCode, getSmsCode, resetPwdSmsCode } from '@/api/auth';
import useLocale from '@/utils/useLocale';
import { Button, Input } from '@arco-design/web-react';
import { IconCode } from '@arco-design/web-react/icon';
import { useCountDown } from 'ahooks';
import React, { useState } from 'react';
import i18n from '@/pages/entry/register/locale/index';
import { getForgotPwdCodeByEmail } from '@/api/resePwd';

interface SmsCodeProps {
  type: 'login' | 'register' | 'resetPwd' | 'forgot';
  use?: 'email' | 'sms';
  username: string;
  disabled?: boolean;
  value?: string;
  onChange?: (val: string) => void;
}
const apis = {
  login: getLoginSmsCode,
  register: getSmsCode,
  resetPwd: resetPwdSmsCode,
  forgot: getForgotPwdCodeByEmail,
};
const SecurityCode = ({
  value,
  onChange,
  disabled = false,
  username,
  type = 'login',
  use = 'sms',
}: SmsCodeProps) => {
  const t = useLocale(i18n);

  const [loading, setLoading] = useState(false);
  const [targetDate, setTargetDate] = useState<number>();
  const [countdown] = useCountDown({
    targetDate,
  });
  const handleGetSms = () => {
    if (countdown === 0) {
      setLoading(true);
      apis[type](String(username), use)
        .then(() => {
          setTargetDate(Date.now() + 60000);
        })
        .finally(() => setLoading(false));
    }
  };
  return (
    <div style={{ display: 'flex' }}>
      <Input
        style={{ flex: 1, marginRight: 10 }}
        value={value}
        onChange={onChange}
        prefix={<IconCode />}
        placeholder={t['register.form.code.placeholder']}
      />
      <Button
        type="outline"
        disabled={countdown !== 0 || disabled}
        onClick={handleGetSms}
        loading={loading}
      >
        {countdown === 0
          ? t['register.sendCode']
          : `${t['register.resendCode']} ${(countdown / 1000).toFixed(0)}s`}
      </Button>
    </div>
  );
};

export default SecurityCode;
