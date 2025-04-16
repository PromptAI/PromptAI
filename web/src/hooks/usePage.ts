import { useRequest } from 'ahooks';
import { useCallback, useRef, useState } from 'react';

const defaultPageSearch = { page: 0, size: 10 };

const usePage = (api: (params?: any) => Promise<any>, initialParams?: any) => {
  const initial = useRef({
    ...defaultPageSearch,
    ...initialParams,
  })
  const [params, setParams] = useState(initial.current);
  const otherRef = useRef<any>();
  const [total, setTotal] = useState(0)
  const [dataSource, setDataSource] = useState<any>();

  const { loading, refresh } = useRequest(() => api(params), {
    refreshDeps: [params],
    onSuccess: res => {
      setDataSource(res.data || res.contents || res);
      otherRef.current = res;
      setTotal(Number(res?.totalCount || res?.data?.totalCount || res.totalElements || 0))
    }
  });

  const onPageChange = useCallback(
    (page: number, size: number) => setParams(p => ({ ...p, page: page - 1 <= 0 ? 0 : page - 1, size })),
    []
  );

  const onSearch = useCallback(
    (searchParams?: any) => setParams(p => ({ ...p, ...searchParams, page: 0 })),
    []
  );

  const onReset = useCallback(() => setParams(initial.current), []);

  return {
    dataSource,
    onPageChange,
    params,
    total,
    setParams,
    loading,
    other: otherRef.current,
    onSearch,
    refresh,
    onReset
  }
}

export default usePage;
