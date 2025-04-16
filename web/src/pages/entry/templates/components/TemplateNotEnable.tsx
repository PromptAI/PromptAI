import React from 'react';
import useLocale from '@/utils/useLocale';
import i18n from '../locale';

const TemplateNotEnable = () => {
  const t = useLocale(i18n);
  return (
    <>
      <div className={'flex justify-center '}>
        <div>
          <div> {t['template.not.enable']}</div>
        </div>
      </div>
    </>
  );
};

export default TemplateNotEnable;
