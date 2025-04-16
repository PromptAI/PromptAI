import { regEmail } from '@/utils/regex';
import useLocale from '@/utils/useLocale';
import { RulesProps } from '@arco-design/web-react';
import { useMemo } from 'react';

const PhoneReg = new RegExp('^[1][3,5,7,8][0-9]{9}$');

export default (): RulesProps[] => {
  const t = useLocale();
  return useMemo(() => [{ required: true, message: t['rules.required'] }], [t]);
};
export function useEmailRules() {
  const t = useLocale();
  return useMemo(
    () => [
      { required: true, message: t['rules.required'] },
      {
        validator(value, callback) {
          if (!regEmail.test(value) && !PhoneReg.test(value)) {
            callback(t['rules.email']);
          }
        },
      },
    ],
    [t]
  );
}

export function useMobileRules() {
  const t = useLocale();
  return useMemo(
    () => [
      { required: true, message: t['rules.required'] },
      {
        validator(value, callback) {
          if (!PhoneReg.test(value)) {
            callback(t['rules.mobile']);
          }
        },
      },
    ],
    [t]
  );
}
