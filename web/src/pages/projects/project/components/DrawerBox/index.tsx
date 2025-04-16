import {
  Button,
  Drawer,
  DrawerProps,
  Form,
  FormInstance,
  FormProps,
  Modal,
  Spin,
} from '@arco-design/web-react';
import { IconClose, IconSave } from '@arco-design/web-react/icon';
import React, {
  ReactNode,
  cloneElement,
  forwardRef,
  isValidElement,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { Prompt } from 'react-router-dom';
import { assign, cloneDeep, isEmpty, isEqual } from 'lodash';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import cn from '@/utils/cn';
import { styled } from 'styled-components';

const SpinWrap = styled(Spin)`
  flex: 1;
  min-height: 0;
  & > .arco-spin-children {
    height: 100%;
    overflow-x: hidden;
    overflow-y: auto;
    padding: 1rem;
  }
`;

export type DrawerBoxProps = Pick<DrawerProps, 'visible' | 'children'>;

const DrawerBox: React.FC<DrawerBoxProps> = ({ visible, children }) => {
  return (
    <Drawer
      width={540}
      height={'calc(100vh - 51px'}
      bodyStyle={{ padding: 0 }}
      headerStyle={{ display: 'none' }}
      visible={visible}
      maskStyle={{ opacity: 0.1, cursor: 'not-allowed' }}
      maskClosable={false}
      escToExit={false}
      closable={false}
      footer={null}
      unmountOnExit
    >
      {children}
    </Drawer>
  );
};

interface DrawerFormBoxProps
  extends DrawerBoxProps,
    Omit<FormProps, 'onSubmit' | 'title' | 'children'> {
  title?: ReactNode;
  onVisibleChange?: (visible: boolean) => void;
  onFinish?: (values: any) => Promise<void>;
  loading?: boolean;
  customEqual?: (origin: any, curren: any) => boolean;
  mode?: 'create' | 'update';
  refresh: () => void;
}
export const DrawerFormBox = forwardRef<FormInstance, DrawerFormBoxProps>(
  (
    {
      title,
      visible,
      children,
      loading,
      onVisibleChange,
      onFinish,
      initialValues,
      onValuesChange,
      className,
      customEqual,
      mode = 'update',
      refresh,
      ...props
    },
    ref
  ) => {
    const t = useLocale(i18n);
    const formRef = useRef<FormInstance>();
    const cache = useRef(() => cloneDeep(initialValues));

    const [isModified, setIsModified] = useState(false);
    useEffect(() => {
      if (visible) {
        // reset cache and isModified
        cache.current = () => cloneDeep(initialValues);
        setIsModified(false);
      }
    }, [initialValues, visible]);
    const [submiting, setSubmiting] = useState(false);
    const onSubmit = useCallback(
      async (values) => {
        if (onFinish) {
          if (mode === 'update' && !isModified) {
            onVisibleChange(false);
            return;
          }
          setSubmiting(true);
          try {
            await onFinish(values);
            refresh();
          } catch (error) {
            console.error(error);
          } finally {
            setSubmiting(false);
          }
        }
        onVisibleChange(false);
      },
      [isModified, mode, onFinish, onVisibleChange, refresh]
    );
    const onClose = useCallback(() => {
      const values = formRef.current.getFieldsValue();
      if (
        isModified &&
        mode === 'update' &&
        isEmpty(formRef.current.getFieldsError())
      ) {
        Modal.confirm({
          title: t['modify.title'],
          content: t['modify.content'],
          onCancel: () => onVisibleChange(false),
          onOk: async () => await onSubmit(values),
          footer: (cancel, ok) => (
            <>
              {ok}
              {cancel}
            </>
          ),
        });
      } else {
        onVisibleChange(false);
      }
    }, [isModified, mode, onSubmit, onVisibleChange, t]);
    const modifyRef = useCallback(
      (ins) => {
        if (ref) {
          if (ref instanceof Function) {
            ref(ins);
          } else {
            (ref as any).current = ins;
          }
        }
        formRef.current = ins;
      },
      [ref]
    );
    const onValuesChangeWrap = useCallback(
      (field, values) => {
        window.requestAnimationFrame(() =>
          setIsModified(
            customEqual
              ? customEqual(
                  cache.current(),
                  assign(cloneDeep(cache.current()), values)
                )
              : !isEqual(
                  cache.current(),
                  assign(cloneDeep(cache.current()), values)
                )
          )
        );
        onValuesChange?.(field, values);
      },
      [onValuesChange, customEqual]
    );

    return (
      <DrawerBox visible={visible}>
        <Form
          ref={modifyRef}
          onSubmit={onSubmit}
          initialValues={initialValues}
          onValuesChange={onValuesChangeWrap}
          className={cn('h-full overflow-hidden', className)}
          {...props}
        >
          <div className="flex justify-between items-center p-2">
            <h3 className="text-lg font-medium">{title}</h3>
            <div className="space-x-2">
              <Button
                type="primary"
                icon={<IconSave />}
                htmlType="submit"
                loading={submiting}
              >
                Save
              </Button>
              <Button icon={<IconClose />} onClick={onClose}>
                Close
              </Button>
            </div>
          </div>
          <div className="border-b mt-1"></div>
          <SpinWrap loading={loading}>{children}</SpinWrap>
        </Form>
        <Prompt when={isModified} message={t['modify.leave.page']} />
      </DrawerBox>
    );
  }
);

export interface DrawerFormBoxTriggerProps extends DrawerFormBoxProps {
  trigger: ReactNode;
}
export const DrawerFormBoxTrigger = forwardRef<
  FormInstance,
  DrawerFormBoxTriggerProps
>(({ trigger, onVisibleChange, ...props }, ref) => {
  return (
    <>
      {isValidElement(trigger) &&
        cloneElement(trigger, { onClick: () => onVisibleChange(true) } as any)}
      <DrawerFormBox
        onVisibleChange={onVisibleChange}
        layout="vertical"
        ref={ref}
        {...props}
      />
    </>
  );
});

export default DrawerBox;
