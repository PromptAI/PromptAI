import {
  Button,
  Card,
  Divider,
  Space,
  Switch,
  Typography,
} from '@arco-design/web-react';
import React, { useRef, useState } from 'react';
import MonacoEditor from 'react-monaco-editor';
import GitHubDark from 'monaco-themes/themes/GitHub Dark.json';
import GitHubLight from 'monaco-themes/themes/GitHub Light.json';
import { useGlobalContext } from '@/context';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { useRequest, useSetState } from 'ahooks';
import { createComponent, listAction, updateComponent } from '@/api/components';
import useUrlParams from '../hooks/useUrlParams';
import { isEmpty } from 'lodash';
import { IconQuestionCircle } from '@arco-design/web-react/icon';
import useDocumentLinks from '@/hooks/useDocumentLinks';

const defaultValue = '';
const Action = () => {
  const isCreadtedRef = useRef<string>(undefined);
  const { theme } = useGlobalContext();
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [stateLoading, setStateLoading] = useSetState({
    enable: false,
    save: false,
  });
  const [enable, setEnable] = useState(false);
  const [data, setData] = useState(defaultValue);
  const { loading, refresh } = useRequest(() => listAction(projectId), {
    refreshDeps: [projectId],
    onSuccess: (remotes) => {
      if (isEmpty(remotes)) {
        isCreadtedRef.current = undefined;
      } else {
        isCreadtedRef.current = remotes[0].id;
        setData(remotes[0].data.code);
        setEnable(remotes[0].data.enable);
      }
    },
  });
  const handleChangeEnable = (val) => {
    setStateLoading({ enable: true });
    if (!isCreadtedRef.current) {
      createComponent(projectId, 'action', {
        data: { code: data, enable: val },
      })
        .then(refresh)
        .finally(() => setStateLoading({ enable: false }));
    } else {
      updateComponent(projectId, 'action', isCreadtedRef.current, {
        data: { code: data, enable: val },
      })
        .then(refresh)
        .finally(() => setStateLoading({ enable: false }));
    }
  };
  const hanldeSaveAction = () => {
    setStateLoading({ save: true });
    if (!isCreadtedRef.current) {
      createComponent(projectId, 'action', {
        data: { code: data, enable },
      })
        .then(refresh)
        .finally(() => setStateLoading({ save: false }));
    } else {
      updateComponent(projectId, 'action', isCreadtedRef.current, {
        data: { code: data, enable },
      })
        .then(refresh)
        .finally(() => setStateLoading({ save: false }));
    }
  };
  const docs = useDocumentLinks();
  return (
    <Card
      title={
        <Space>
          <span>Action</span>
          <a target="_blank" href={docs.botAction} rel="noreferrer">
            <IconQuestionCircle />
          </a>
        </Space>
      }
      size="small"
      bodyStyle={{
        height: 'calc(100vh - 148px)',
      }}
      loading={loading}
      extra={
        <Space size="medium">
          <Space>
            <Typography.Text>{t['action.enable']}</Typography.Text>
            <Switch
              checkedText={t['action.enable.true']}
              uncheckedText={t['action.enable.false']}
              checked={enable}
              loading={stateLoading.enable}
              onChange={handleChangeEnable}
            />
          </Space>
          <Divider type="vertical" />
          <Button
            type="primary"
            size="small"
            shape="round"
            loading={stateLoading.save}
            onClick={hanldeSaveAction}
          >
            {t['action.save']}
          </Button>
        </Space>
      }
    >
      <MonacoEditor
        width="100%"
        height="100%"
        language="python"
        theme="monokai"
        value={data}
        onChange={setData}
        className="vs-custom"
        defaultValue={defaultValue}
        editorWillMount={(monaco) => {
          monaco.editor.defineTheme(
            'monokai',
            theme == 'dark' ? (GitHubDark as any) : (GitHubLight as any)
          );
        }}
        options={{ selectOnLineNumbers: true, fontSize: 16 }}
      />
    </Card>
  );
};

export default Action;
