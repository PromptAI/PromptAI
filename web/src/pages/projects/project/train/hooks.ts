import {listAgents, listConversations, listFaqs} from '@/api/components';
import { infoProject, publish, stopPublish } from '@/api/projects';
import { ProjectLayoutContextValue } from '@/layout/project-layout/context';
import useLocale from '@/utils/useLocale';
import { Message } from '@arco-design/web-react';
import { useRequest, useSetState } from 'ahooks';
import { debounce, isEmpty, omit } from 'lodash';
import { useCallback, useEffect, useMemo, useState } from 'react';
import i18n from './locale';
import { encodeChatScript, encodeChatUrl } from '@/utils/chatsdk';
import { Settings } from './type';

type TrainMetaData = {
  loading: boolean;
  data: any;
  id: string;
  refreshAsync: () => Promise<any>;
};
export function useTrainMetaData({
  id,
}: ProjectLayoutContextValue): TrainMetaData {
  // sync project data
  const { loading, data, refreshAsync } = useRequest(() => infoProject(id), {
    refreshDeps: [id],
  });
  return { loading, data, id, refreshAsync };
}
export function useSelectorData(
  meta: TrainMetaData,
  api: (projectId: string) => Promise<any>,
  publishing: boolean,
  publishComponentIds: string[]
) {
  const [data, setData] = useState([]);

  const { loading, refresh, run } = useRequest(() => api(meta.id), {
    refreshDeps: [meta.id],
    manual: true,
    onSuccess: (res) => {
      if (meta.data) {
        let components = [];
        if (publishing) {
          components = publishComponentIds;
        } else {
          components =
            meta.data?.publishedProject?.properties?.componentIds || [];
        }

        setData(
          res
            .map(({ id, data: { name, description, isReady }, type }) => {
              return {
                id,
                name,
                description,
                checked: components.some((x) => x === id),
                disabled: isReady === undefined ? false : !isReady,
                type,
              };
            })
            .filter((m) => !m.disabled)
        );
      }
    },
  });
  useEffect(() => {
    if (!meta.loading) {
      run();
    }
  }, [meta.loading, run]);
  useEffect(() => {
    if (publishing) {
      run();
    }
  }, [publishing, run]);
  return { loading, refresh, data, setData };
}
const fetchModules = async (projectId: string) => {
  const faqs = await listFaqs(projectId);
  const flows = await listConversations(projectId);
  const agents = await listAgents(projectId);
  return [...faqs, ...flows, ...agents];
};
export function useTrainData(
  meta: TrainMetaData,
  publishing: boolean,
  publishComponentIds: string[]
) {
  return useSelectorData(meta, fetchModules, publishing, publishComponentIds);
}

export function usePollingPublish(
  project: ProjectLayoutContextValue,
  settings: Settings,
  omitSettingsProps: string[],
  scene: 'publish_db' | 'publish_snapshot'
) {
  const t = useLocale(i18n);

  const [status, setStatus] = useSetState({
    taskLoading: false,
    publishLoding: false,
    recentRecords: [],
    deployUrl: null,
    mobileUrl: null,
    stopLoading: false,
    stopDisabled: false,
    current: 'not_running',
    status: '',
    id: null,
    token: null,
    publishComponentIds: [],
  });
  const { cancel, run, runAsync } = useRequest(() => publish(project.id), {
    pollingInterval: 5000,
    pollingWhenHidden: true,
    retryInterval: 3,
    onSuccess: (res) => {
      if (isEmpty(res)) return;
      const { id, token, recentRecords = [], status, properties } = res;
      const { publishingIds } = properties;
      let taskLoading = false;
      let publishLoding = false;
      let deployUrl = null;
      let mobileUrl = null;
      let current = 'not_running';
      let publishComponentIds = [];
      if (!isEmpty(recentRecords)) {
        const { status: first } = recentRecords[0];
        publishComponentIds = publishingIds || [];
        current = first;
        taskLoading = first === 'running';
        if (first === 'running' && status === 'deploying') {
          publishLoding = true;
        }
        if (first === 'failed') {
          // Message.error(t['releases.Publishing.failed']);
          cancel();
          publishLoding = false;
        }
        if (first === 'success') {
          cancel();
          publishLoding = false;
        }
      } else {
        // deploying 状态，但是没有任务
        publishLoding = false;
      }
      if (status === 'running') {
        // Message.success(t['train.publish.success']);
        const params: any = {
          ...settings,
          id,
          token,
          project: project.id,
        };
        deployUrl = encodeChatScript(
          omit({ ...params, scene }, ...omitSettingsProps) as any
        );
        mobileUrl = encodeChatUrl(
          omit({ ...params, scene }, 'resetSlots', ...omitSettingsProps) as any,
          { theme: settings.theme || 'default' }
        );
      }
      setStatus({
        publishLoding,
        taskLoading,
        recentRecords,
        deployUrl,
        mobileUrl,
        stopDisabled: status === 'not_running' || isEmpty(status),
        current,
        status,
        id,
        token,
        publishComponentIds,
      });
    },
  });

  const debounceChange = useMemo(
    () =>
      debounce(
        ({ id, settings, scene }) =>
          setStatus((s) => {
            if (s.status === 'running') {
              const params: any = {
                ...settings,
                id: s.id,
                token: s.token,
                project: id,
              };
              const deployUrl = encodeChatScript(
                omit({ ...params, scene }, ...omitSettingsProps) as any
              );
              const mobileUrl = encodeChatUrl(
                omit(
                  { ...params, scene },
                  'resetSlots',
                  ...omitSettingsProps
                ) as any,
                { theme: settings.theme || 'default' }
              );
              return {
                ...s,
                deployUrl,
                mobileUrl,
              };
            }
            return s;
          }),
        300
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );
  useEffect(() => {
    debounceChange({ id: project.id, settings, scene });
  }, [debounceChange, project.id, settings, scene]);

  const stop = useCallback(() => {
    setStatus({ stopLoading: true });
    stopPublish(project.id)
      .then(() => {
        Message.success(t['train.releases.stop.success']);
        setStatus({
          publishLoding: false,
          taskLoading: false,
          deployUrl: null,
          stopLoading: false,
          current: 'not_running',
        });
        runAsync().then(() => {
          cancel();
        });
      })
      .catch(() => {
        setStatus({ stopLoading: false });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project.id, t]);
  return { status, run, stop, setStatus };
}
