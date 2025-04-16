import useForm from "@arco-design/web-react/es/Form/useForm";
import { useMemoizedFn } from "ahooks";
import { Dispatch, SetStateAction, useEffect } from "react";

export default function useSearch(setParams: Dispatch<SetStateAction<any>>, onResetAfter?: () => void, initialSearch?: any) {
  const [form] = useForm();
  useEffect(() => {
    if (initialSearch) form.setFieldsValue(initialSearch);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const onSubmit = useMemoizedFn((params) => setParams(p => ({ ...p, ...params })));
  const onReset = useMemoizedFn(() => {
    form.resetFields();
    onResetAfter?.();
  })
  return {
    form,
    onSubmit,
    onReset
  }
}