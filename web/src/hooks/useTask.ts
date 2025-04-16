import { useRequest, useSafeState, useUpdateEffect } from 'ahooks';
import { isEmpty } from 'lodash';
import { useCallback } from 'react';

export type Task = {
  api: (params: any) => Promise<any>;
  params?: any;
  success: (response: any) => boolean;
  fail: (response: any) => boolean;
};
const defaultResult = { loading: false, isSuccess: false, result: null };

export default ({ api, params, success, fail }: Task, delay = 2000) => {
  const [result, setResult] = useSafeState(defaultResult);
  const { loading, data, runAsync, cancel } = useRequest(() => api(params), {
    pollingInterval: delay,
    pollingWhenHidden: false,
    manual: true,
    refreshDeps: [params],
  });
  useUpdateEffect(() => {
    if (!loading && !isEmpty(data)) {
      if (success(data)) {
        cancel();
        setResult({ loading: false, isSuccess: true, result: data });
      }
      if (fail(data)) {
        cancel();
        setResult({ loading: false, isSuccess: false, result: data });
      }
    }
  }, [loading, data]);

  const runTask = useCallback(() => {
    setResult({ loading: true, isSuccess: false, result: null });
    runAsync();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const cancelTask = useCallback(() => {
    setResult(defaultResult);
    cancel();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return [result, runTask, cancelTask] as const;
};
