import { infoProject } from '@/api/projects';
import {hasTaskRunning, listenRecord, listenTask, publishRecordRunning} from '@/api/rasa';
import useLocale from '@/utils/useLocale';
import { Message } from '@arco-design/web-react';
import { useMemoizedFn, useRequest, useSetState } from 'ahooks';
import { isEmpty } from 'lodash';
import { useRef, useState } from 'react';
import i18n from './locale';

const stepTypeInTrain = {
  DOWNLOAD: 0,
  MOVE_FILE_TO_DIR: 1,
  TRAIN_MODEL: 1,
  UPLOAD: 2,
  REPLACE_MODEL: 2,
  CHECK_MODEL: 2,
};
const stepTypeInReplace = {
  DOWNLOAD: 0,
  MOVE_FILE_TO_DIR: 2,
  REPLACE_MODEL: 2,
  CHECK_MODEL: 2,
};
const defaultRecords = [
  {
    content: 'Wait start',
    createTime: Date.now(),
    elapsed: '0',
    index: 0,
    ok: true,
    step: 'DOWNLOAD',
  },
];
const defaultTaskProcess = [
  { index: 0, ok: true, stageName: 'STAGE_PREPARE_DATA', stepPercent: '1%' },
];

const defaultMessage = "发布中";
const checkProjectCurrent = (project: any, current: string) => {
  const { debugProject } = project;
  if (debugProject) {
    const {
      properties: { componentIds },
    } = debugProject;
    return (componentIds || []).includes(current);
  } else {
    // first and no train debug module
    return false;
  }
};
const filterRecords = (records?: any[]) => {
  if (isEmpty(records)) {
    return defaultRecords;
  }
  return records.filter((r) => r.index !== null);
};
const defaultReadyData = {
  visible: false,
  stages: [],
  authentication: null,
  message: null,
  totalTime: 120 * 1000,
  status: null,
  disabled: false,
};
export function useDebugState(
  current: string,
  projectId: string,
  componentIds?: string[]
) {
  const t = useLocale(i18n);
  const [readyData, setReadyData] = useSetState(defaultReadyData);
  const [message, setMessage] = useState<any>("");

  const [checkLoading, setCheckLoading] = useState(false);

  const recordId = useRef<string>();

  const { runAsync: startRecordListen, cancel: cancelRecordListen } = useRequest(
    () => listenRecord(recordId.current),
    {
      manual: true,
      pollingInterval: 1000,
      onSuccess: (record) => {
        if (record) {
          const stages = record.properties.stages;
          switch (record.status) {
            case "running":
              // cancel
              setReadyData({
                stages,
                authentication: null,
                message: "进行中",
                disabled: false,
                status: "running",
              });
              cancelRecordListen();
              break;
            case "cancel":
              // cancel
              setReadyData({
                stages: stages,
                authentication: null,
                message: t['debug.robot.canceled'],
                disabled: false,
                status: "cancel",
              });
              cancelRecordListen();
              break;
            case "failed":
              setReadyData({
                stages: stages,
                authentication: null,
                message: t['debug.robot.train.error'],
                disabled: false,
                status: "failed",
              });
              break;
            case "success":
              // finish and success
              infoProject(projectId)
                  .then((project) => {
                    const { debugProject } = project;
                    if (debugProject) {
                      const { id, token } = debugProject;
                      setReadyData({
                        stages,
                        authentication: {
                          'X-published-project-id': id,
                          'X-published-project-token': token,
                          'X-project-id': projectId,
                        },
                        message: "完成",
                        disabled: false,
                        status: "success",
                      });
                    }
                  })
                  .catch(() => Message.error(t['debug.robot.unknown.error']));
              cancelRecordListen();
              break;
            default:
              break;
          }
        }
      },
    }
  );

  const open = useMemoizedFn(async () => {
    setCheckLoading(true);
    const runningRecords = await publishRecordRunning();
    if (isEmpty(runningRecords?.[0])) {
      // not running
      const project = await infoProject(projectId);
      if (
        (current === undefined &&
          project.debugProject?.properties.componentIds?.some((c) =>
            componentIds?.includes(c)
          )) ||
        checkProjectCurrent(project, current)
      ) {
        // open model and start conversation
        const { id, token } = project.debugProject;
        setReadyData({
          visible: true,
          authentication: {
            'X-published-project-id': id,
            'X-published-project-token': token,
            'X-project-id': projectId,
          },
          stages: [],
          totalTime: 120 * 1000,
          disabled: false,
        });
      } else {
        Message.warning(t['debug.robot.isCurrent.false']);
        setReadyData({
          disabled: true,
        });
        setCheckLoading(false);
        return Promise.reject();
      }
    } else {
      // is running
      const record = runningRecords[0];
      if (
        (current === undefined &&
          record.properties.publishRoots?.some((c) =>
            componentIds?.includes(c)
          )) ||
        (record.properties.publishRoots || []).includes(current)
      ) {
        recordId.current = record.id;
        // open model and process
        setReadyData({
          visible: true,
          stages: record.properties.stages,
          totalTime: 120 * 1000,
          disabled: false,
        });
        recordId.current = record.id;
        startRecordListen();
      } else {
        Message.warning(t['debug.robot.isCurrent.false.running']);
        setReadyData({
          disabled: true,
        });
        setCheckLoading(false);
        return Promise.reject();
      }
    }
    setCheckLoading(false);
  });

  const close = useMemoizedFn(() => {
    cancelRecordListen();
    recordId.current = null;
  });

  const cancel = useMemoizedFn(() => {
    if (recordId.current) {
      cancelRecordListen();
      recordId.current = null;
      setReadyData({
        ...defaultReadyData,
      });
    }
  });

  return {
    checkLoading,
    readyData,
    open,
    close,
    recordId: recordId.current,
    cancel,
  };
}
