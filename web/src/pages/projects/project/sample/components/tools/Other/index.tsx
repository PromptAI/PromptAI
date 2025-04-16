import { Button, Dropdown, Menu } from '@arco-design/web-react';
import React from 'react';
import UploadFaq from './UploadFaq';
import DownloadRasaTemlate from './DownloadRasaTemlate';
import { IconDown } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import DownloadFaqs from './DownloadFaqs';

const Other = ({ onSuccess, current, components }) => {
  const t = useLocale(i18n);
  return (
    <Dropdown
      unmountOnExit={false}
      droplist={
        <Menu>
          <Menu.Item key="template">
            <DownloadRasaTemlate title={t['sample.import.template']} />
          </Menu.Item>
          <Menu.Item key="download">
            <DownloadFaqs components={components} />
          </Menu.Item>
          <Menu.Item key="import">
            <UploadFaq onSuccess={onSuccess} faqId={current} />
          </Menu.Item>
        </Menu>
      }
      position="bl"
      trigger="click"
    >
      <Button type="text">
        {t['sample.other']} <IconDown />
      </Button>
    </Dropdown>
  );
};

export default Other;
