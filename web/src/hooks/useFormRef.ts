import { FormInstance } from '@arco-design/web-react';
import { useDeepCompareEffect } from 'ahooks';
import { useRef } from 'react';

export default function useFormRef(values?: any) {
  const ref = useRef<FormInstance>();
  useDeepCompareEffect(() => {
    if (values) ref.current?.setFieldsValue(values);
  }, [values]);
  return ref;
}
