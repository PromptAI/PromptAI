import { listConversations, listFaqs } from '@/api/components';
import { useSetState } from 'ahooks';
import { useEffect } from 'react';
import { DebugData, DebugOption } from './types';

export async function fetchData(projectId: string) {
  return Promise.all([listFaqs(projectId), listConversations(projectId)]).then(
    ([faqs, flows]) => [
      ...faqs.map((f) => ({
        label: f.data.name,
        value: f.id,
        disabled: f.data.isReady === undefined ? false : !f.data.isReady,
        type: f.type,
      })),
      ...flows.map((f) => ({
        label: f.data.name,
        value: f.id,
        disabled: f.data.isReady === undefined ? false : !f.data.isReady,
        type: f.type,
      })),
    ]
  );
}

export default function useDebugData(
  projectId: string,
  manual = true,
  onSuccess?: (options: DebugOption[]) => void
) {
  const [data, setData] = useSetState<DebugData>({
    loading: false,
    options: [],
  });
  useEffect(() => {
    if (manual) {
      setData({ loading: true });
      fetchData(projectId)
        .then((options) => {
          setData({ options });
          onSuccess?.(options);
        })
        .finally(() => setData({ loading: false }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, manual]);

  return data;
}
