import { useMemo } from 'react';
import { useParams } from 'react-router';

interface UrlParams {
  id: string;
  cId?: string;
}
export default function useUrlParams() {
  const params = useParams<UrlParams>();
  return useMemo(
    () => ({ projectId: params.id, flowId: params.cId }),
    [params]
  );
}
