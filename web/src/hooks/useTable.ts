import { useMemoizedFn, useRequest } from 'ahooks';
import { useRef, useState } from 'react';

interface Params {
  page: number;
  size: number;
}
const defaultInitialParams: Params = { page: 0, size: 10 };
export default function useTable(
  promise: (params: any) => Promise<any>,
  initialParams?: Params | Record<string, any>,
  dataKey = 'contents',
  totalKey = 'totalElements'
) {
  const initialParamsRef = useRef({
    ...defaultInitialParams,
    ...initialParams,
  });
  const [params, changeParams] = useState(initialParamsRef.current);
  const { loading, data, refresh } = useRequest(() => promise(params), {
    refreshDeps: [params],
  });
  const onPageChange = useMemoizedFn((current: number, size: number) =>
    changeParams((p) => ({ ...p, page: Math.max(current - 1, 0), size }))
  );
  const reset = useMemoizedFn(() => changeParams(initialParamsRef.current));
  const setParams = useMemoizedFn((t) =>
    changeParams((p) => {
      const temp = { ...p, ...initialParamsRef.current };
      return { ...temp, ...(typeof t === 'function' ? t(temp) : t) };
    })
  );

  return {
    loading,
    data: data?.[dataKey],
    total: data?.[totalKey],
    origin: data,
    refresh,
    setParams,
    params,
    onPageChange,
    reset,
  };
}
