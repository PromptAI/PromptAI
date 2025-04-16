import useLocale from '@/utils/useLocale';
import { Divider, Typography } from '@arco-design/web-react';
import { IconInfoCircle } from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import i18n from '../locale';
import Content from './Content';

const TabContent = ({ data, value, onChange }) => {
  const t = useLocale(i18n);
  const visibleData = useMemo(() => data.filter((d) => !d.disabled), [data]);
  const unVisibleData = useMemo(() => data.filter((d) => d.disabled), [data]);
  return (
    <div style={{ padding: '0 16px 16px 16px' }}>
      <Content
        title={t['component.download.rasa.file.tab.ready']}
        items={visibleData}
        value={value}
        onChange={onChange}
      />
      <Divider />
      <Content
        title={t['component.download.rasa.file.tab.unReady']}
        subTitle={
          <Typography.Text type="warning">
            <IconInfoCircle />
            {t['component.download.rasa.file.tab.unReady.description']}
          </Typography.Text>
        }
        items={unVisibleData}
        value={value}
        onChange={onChange}
      />
    </div>
  );
};

export default TabContent;
