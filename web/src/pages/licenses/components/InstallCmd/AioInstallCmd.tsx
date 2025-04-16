import useLocale from '@/utils/useLocale';
import { Button, Message, Tabs, Typography } from '@arco-design/web-react';
import React from 'react';
import { FaApple, FaLinux } from 'react-icons/fa';
import i18n from './locale';
import { IconCopy } from '@arco-design/web-react/icon';
import { CopyToClipboard } from 'react-copy-to-clipboard';

interface TabContentProps {
  install: string;
  requirements: number[];
}
interface CopyButtonProps {
  text: string;
}
export const CopyButton: React.FC<CopyButtonProps> = ({ text }) => {
  const t = useLocale(i18n);
  const onCopy = (_, result) => {
    if (result) {
      Message.success(t['install.copy.success']);
    }
  };
  return (
    <CopyToClipboard text={text} onCopy={onCopy}>
      <Button type="text" icon={<IconCopy />} />
    </CopyToClipboard>
  );
};

const TabContent: React.FC<TabContentProps> = ({ install, requirements }) => {
  const t = useLocale(i18n);
  return (
    <div style={{ padding: '0px 16px 16px' }}>
      <Typography.Title heading={5}>
        {t['install.requirements.title']}
      </Typography.Title>
      <div style={{ padding: '8px', backgroundColor: 'rgb(var(--gray-2))' }}>
        <Typography.Paragraph
          style={{
            display: 'inline-block',
            width: '100%',
            margin: 0,
          }}
        >
          <ol>
            {requirements.map((_, i) => (
              <li key={i}>{t[`install.requirements.${i}`]}</li>
            ))}
          </ol>
        </Typography.Paragraph>
      </div>
      <Typography.Title heading={5} style={{ marginTop: 16 }}>
        {t['install.it']}
      </Typography.Title>
      <div className="flex items-center">
        <Typography.Paragraph
          style={{
            display: 'flex',
            width: '100%',
            margin: 0,
            flex: 1,
            lineHeight: 1,
            alignItems: 'center',
          }}
          className="normal-code-font-size"
          code
        >
          {install}
        </Typography.Paragraph>
        <div
          style={{
            backgroundColor: 'rgb(var(--gray-2))',
            marginLeft: 8,
            height: '100%',
          }}
        >
          <CopyButton text={install} />
        </div>
      </div>
    </div>
  );
};
interface TitleProps {
  icon: React.ReactNode;
  title: string;
}
const Title: React.FC<TitleProps> = ({ icon, title }) => {
  return (
    <div className="flex flex-col items-center" style={{ width: 60 }}>
      {React.cloneElement(icon as any, { style: { fontSize: 28 } })}
      {title}
    </div>
  );
};

interface AioInstallCommandProps {
  cmd: string;
}
const linuxRequirements = new Array(4).fill(0);
const macOSRequirements = new Array(4).fill(0);
const AioInstallCommand: React.FC<AioInstallCommandProps> = ({ cmd }) => {
  return (
    <Tabs defaultActiveTab="linux">
      <Tabs.TabPane
        key="linux"
        title={<Title icon={<FaLinux />} title="Linux" />}
      >
        <TabContent requirements={linuxRequirements} install={cmd} />
      </Tabs.TabPane>
      <Tabs.TabPane
        key="macos"
        title={<Title icon={<FaApple />} title="Mac OS" />}
      >
        <TabContent requirements={macOSRequirements} install={cmd} />
      </Tabs.TabPane>
    </Tabs>
  );
};

export default AioInstallCommand;
