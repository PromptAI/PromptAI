import { Card, Tabs } from '@arco-design/web-react';
import React, { useState } from 'react';
import Accounts from './Accounts';
import RegisterCodes from './RegisterCodes';
import RegisterApply from './RegisterApply';
import WxQrCode from './WxQrCode';
import Configs from './Configs';
import Stripe from './Stripe';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

export default function SysAdmin() {
  const [tabsKey, setTabsKey] = useState('admin');
  const t = useLocale(i18n);
  return (
    <Card size="small">
      <Tabs activeTab={tabsKey} destroyOnHide onChange={setTabsKey}>
        <Tabs.TabPane key="admin" title={t['account']}>
          <Accounts />
        </Tabs.TabPane>
        <Tabs.TabPane key="register_code" title={t['code']}>
          <RegisterCodes />
        </Tabs.TabPane>
        <Tabs.TabPane key="register_apply" title={t['apply']}>
          <RegisterApply />
        </Tabs.TabPane>
        <Tabs.TabPane key="update_qrcode" title={t['qrcode']}>
          <WxQrCode />
        </Tabs.TabPane>
        <Tabs.TabPane key="config" title={t['config']}>
          <Configs />
        </Tabs.TabPane>
        <Tabs.TabPane key="Strip" title={t['stripe']}>
          <Stripe />
        </Tabs.TabPane>
      </Tabs>
    </Card>
  );
}
