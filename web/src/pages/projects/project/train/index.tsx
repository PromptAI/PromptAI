import useLocale from '@/utils/useLocale';
import {
  Card,
  Divider,
  Message,
  Typography,
  Tag,
  Button,
  Tooltip,
  Radio,
  Tabs,
  Space,
} from '@arco-design/web-react';
import {
  IconCode,
  IconMobile,
  IconDownload,
  IconExperiment,
  IconThunderbolt,
} from '@arco-design/web-react/icon';
import React, { useMemo, useState } from 'react';
import { StopReleaseTrigger } from '../../components/trigger';
import i18n from './locale';
import { startPublish, updateProject } from '@/api/projects';
import '@/core-next/index.css';
import TrainState from './TrainState';
import {
  useProjectContext,
  useProjectType,
} from '@/layout/project-layout/context';
import { Tool, useTools } from '@/components/Layout/tools-context';
import { usePollingPublish, useTrainData, useTrainMetaData } from './hooks';
import { debounce, isEmpty, maxBy } from 'lodash';
import { downloadRasaFile } from '@/api/components';
import { downloadFile } from '@/utils/downloadObject';
import PublishRobot from '../components/publish-robot';
import { useUpdateEffect } from 'ahooks';
import QrcodeForPreview from './qrcodeForPreview';
import BotSettingsModal from './components/BotSettingsModal';
import ReleaseButton from './components/ReleaseButton';
import ModuleSelector from './components/ModuleSelector';
import moment from 'moment';

const RASR_VERSION = process.env.REACT_APP_RASA_VERSION || '3.2.0';
const RadioGroup = Radio.Group;

const omitSettingProps = [
  'welcome',
  'locale',
  'icon',
  'survey',
  'upload',
  'name',
  'slots',
  'variables',
  'theme',
  'minimize',
  'viewable',
  'introduction',
  'schedule',
];
const parseChatbotSettings = (project, defaultChatbotSettings) => {
  if (project.chatBotSettings) {
    return {
      ...project.chatBotSettings,
      welcome: project.welcome || defaultChatbotSettings.welcome,
      locale: project.locale,
      theme: project.chatBotSettings?.theme || defaultChatbotSettings.theme,
      introduction: project.introduction,
      viewable: project.viewable,
      schedule: project.schedule,
    };
  }
  return defaultChatbotSettings;
};
const debounceUpdateSettings = debounce(async (project, settings) => {
  project.chatBotSettings = settings;
  project.welcome = settings.welcome;
  project.locale = settings.locale;
  project.introduction = settings.introduction;
  project.viewable = settings.viewable;
  project.schedule = settings.schedule;
  await updateProject(project);
  await project.refresh();
  Message.success('success');
}, 400);
const useChatbotSettings = (project) => {
  const t = useLocale(i18n);
  const defaultChatbotSettings = useMemo(
    () => ({
      survey: true,
      name: 'PromptAI',
      upload: true,
      welcome: t['project.from.welcome.placeholder'],
      theme: 'default',
    }),
    [t]
  );
  const initialValues = useMemo(
    () => parseChatbotSettings(project, defaultChatbotSettings),
    [defaultChatbotSettings, project]
  );
  const [settings, setSettings] = useState(initialValues);
  useUpdateEffect(() => {
    debounceUpdateSettings(project, settings);
  }, [settings, project]);
  return [settings, setSettings, initialValues] as const;
};

const Train = () => {
  const t = useLocale(i18n);

  const statusTextMap = useMemo(
    () => ({
      not_running: t['train.status.notRunning'],
      running: t['train.status.running'],
      deploying: t['train.status.deploying'],
      unknown: t['train.status.unknown'],
    }),
    [t]
  );

  const type = useProjectType();
  const project = useProjectContext();
  const [settings, setSettings, initialValues] = useChatbotSettings(project);
  const meta = useTrainMetaData(project);
  const [scene, setScene] = useState<'publish_db' | 'publish_snapshot'>(
    'publish_db'
  );
  const { status, run, stop } = usePollingPublish(
    project,
    settings,
    omitSettingProps,
    scene
  );
  const {
    loading,
    data = [],
    refresh,
    setData,
  } = useTrainData(meta, status.publishLoding, status.publishComponentIds);
  useUpdateEffect(() => {
    if (status.current !== 'running') {
      meta.refreshAsync();
    }
  }, [status.current, meta.refreshAsync]);
  const [downloading, setDownloading] = useState(false);
  const [embedType, setEmbedType] = useState<'web' | 'mobile'>('web');

  const [startPublishLoading, setStartPublishLoading] = useState(false);
  const tools = useMemo<Tool[]>(() => {
    const comIds = data
      .filter((f) => f.checked && f.disabled === false)
      .map((c) => c.id);
    const download = () => {
      if (isEmpty(comIds)) return;
      setDownloading(true);
      downloadRasaFile({ componentIds: comIds, projectId: project.id })
        .then((res) => downloadFile(res, comIds.join()))
        .finally(() => setDownloading(false));
    };
    return [
      {
        key: 'status',
        component: (
          <Tag>{statusTextMap[status.status] || statusTextMap.unknown}</Tag>
        ),
      },
      {
        key: 'task-list',
        component: <TrainState data={status.recentRecords} />,
      },
      !status.stopDisabled && {
        key: 'stop',
        component: (
          <StopReleaseTrigger
            loading={status.stopLoading}
            onClick={stop}
            status="warning"
          >
            {t['train.releases.stop']}
          </StopReleaseTrigger>
        ),
      },
       {
        key: 'download',
        component: (
          <div>
            <Button
              icon={<IconDownload />}
              onClick={download}
              loading={downloading}
              type="text"
              disabled={isEmpty(comIds)}
            >
              {t['train.releases.download']}
            </Button>
          </div>
        ),
       },
    ];
  }, [
    data,
    statusTextMap,
    status.status,
    status.recentRecords,
    status.stopLoading,
    status.stopDisabled,
    stop,
    t,
    downloading,
    project.id,
    type
  ]);
  useTools(tools);
  const startPublishRequest = () => {
    const comIds = data
      .filter((f) => f.checked && f.disabled === false)
      .map((c) => c.id);
    if (isEmpty(comIds)) {
      Message.warning(t['train.publish.select']);
      return;
    }
    setStartPublishLoading(true);
    startPublish(project.id, [...comIds])
      .then((res) => {
        res.properties.runModel = res.properties.runModel || 'rasa';
        if (
          res.recentTasks?.[0]?.status === 5 ||
          res.properties.runModel !== 'rasa'
        ) {
          Message.success(t['train.publish.success']);
        }
        if (res.recentTasks?.[0]?.status === 4) {
          Message.error(t['releases.Publishing.failed']);
        }
        run();
      })
      .finally(() => setStartPublishLoading(false));
  };
  const releasedTime = useMemo(() => {
    if (!status.deployUrl) {
      return '';
    }
    const task = maxBy(status.recentRecords, (r) =>
      Number(r.properties.createTime)
    );
    if (task && task.status === 'success') {
      return moment(Number(task.properties.createTime)).format(
        'YYYY-MM-DD HH:mm:ss'
      );
    }
    return '';
  }, [status.deployUrl, status.recentRecords]);
  return (
    <div>
      <Card
        size="small"
        title={t['train.title']}
        className="train"
        loading={meta.loading}
      >
        <div
          style={{
            display: 'flex',
            flexDirection: 'row',
            justifyContent: 'space-around',
            gap: 16,
          }}
        >
          <Card bordered={false}>
            <ReleaseButton
              onClick={startPublishRequest}
              loading={startPublishLoading || status.publishLoding}
              disabled={
                status.stopLoading ||
                status.publishLoding ||
                startPublishLoading
              }
              releasedTime={releasedTime}
            />
          </Card>
          <Tabs
            size="small"
            defaultActiveTab="publish_db"
            className="flex-1"
            onChange={(key) => setScene(key as any)}
            style={{ maxWidth: 720 }}
          >
            <Tabs.TabPane
              key="publish_db"
              title={
                <Space size="small">
                  <IconExperiment />
                  {t['train.script.test']}
                </Space>
              }
            >
              <Card
                bordered={false}
                title={
                  <RadioGroup
                    type="button"
                    name="lang"
                    value={embedType}
                    onChange={(value) => setEmbedType(value)}
                    style={{ marginTop: 10, marginBottom: 10 }}
                  >
                    <Radio color="green" value="web">
                      <IconCode style={{ marginRight: 5 }} />
                      {t['train.insert.web']}(javascript)
                    </Radio>
                    <Radio color="green" value="mobile">
                      <IconMobile style={{ marginRight: 5 }} />
                      {t['train.insert.mobile']}
                    </Radio>
                  </RadioGroup>
                }
                extra={
                  <BotSettingsModal
                    type={embedType}
                    initialValues={initialValues}
                    onSettingsChange={setSettings}
                    mobileUrl={status.mobileUrl}
                  />
                }
              >
                {embedType === 'web' && (
                  <Typography.Paragraph copyable={!!status.deployUrl}>
                    {status.deployUrl
                      ? status.deployUrl
                      : t['train.insert.nopublish']}
                  </Typography.Paragraph>
                )}
                {embedType === 'mobile' && (
                  <Typography.Paragraph copyable={!!status.deployUrl}>
                    {status.mobileUrl
                      ? status.mobileUrl
                      : t['train.insert.nopublish']}
                  </Typography.Paragraph>
                )}
              </Card>
            </Tabs.TabPane>
            <Tabs.TabPane
              key="publish_snapshot"
              title={
                <Space size="small">
                  <IconThunderbolt />
                  {t['train.script.production']}
                </Space>
              }
            >
              <Card
                bordered={false}
                title={
                  <RadioGroup
                    type="button"
                    name="lang"
                    value={embedType}
                    onChange={(value) => setEmbedType(value)}
                    style={{ marginTop: 10, marginBottom: 10 }}
                  >
                    <Radio color="green" value="web">
                      <IconCode style={{ marginRight: 5 }} />
                      {t['train.insert.web']}(javascript)
                    </Radio>
                    <Radio color="green" value="mobile">
                      <IconMobile style={{ marginRight: 5 }} />
                      {t['train.insert.mobile']}
                    </Radio>
                  </RadioGroup>
                }
                extra={
                  <BotSettingsModal
                    type={embedType}
                    initialValues={initialValues}
                    onSettingsChange={setSettings}
                    mobileUrl={status.mobileUrl}
                  />
                }
              >
                {embedType === 'web' && (
                  <Typography.Paragraph copyable={!!status.deployUrl}>
                    {status.deployUrl
                      ? status.deployUrl
                      : t['train.insert.nopublish']}
                  </Typography.Paragraph>
                )}
                {embedType === 'mobile' && (
                  <Typography.Paragraph copyable={!!status.deployUrl}>
                    {status.mobileUrl
                      ? status.mobileUrl
                      : t['train.insert.nopublish']}
                  </Typography.Paragraph>
                )}
              </Card>
            </Tabs.TabPane>
          </Tabs>
          <Card
            bordered={false}
            style={{ minWidth: 0 }}
            title={t['train.insert.ScanningCodePreview']}
          >
            {status.mobileUrl ? (
              <QrcodeForPreview
                src={`/api/qrcode?${new URLSearchParams({
                  content: status.mobileUrl,
                }).toString()}`}
                alt="mobile link qrcode"
                width={150}
                height={150}
              />
            ) : (
              <Typography.Paragraph>
                {t['train.insert.noQr']}
              </Typography.Paragraph>
            )}
          </Card>
        </div>
        <Divider />
        <ModuleSelector
          title={t['train.flows']}
          loading={loading}
          refresh={refresh}
          value={data}
          onChange={setData}
          released={!!releasedTime}
        />
      </Card>
      <PublishRobot disabled={status.status !== 'running'} />
    </div>
  );
};

export default Train;
