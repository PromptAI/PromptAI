import { GraphNode } from '@/graph-next/type';
import React, { forwardRef, useMemo, useRef, useImperativeHandle } from 'react';
import { Button, Card, Drawer, Message, Space } from '@arco-design/web-react';
import { IconClose, IconSave } from '@arco-design/web-react/icon';
import { ComponentHandle } from './type';
import useLocale from '@/utils/useLocale';
import { SelectionProps } from '../types';
import i18n from './locale';
import ProjectForm from './forms/project-form';
import SampleForm from './forms/sample-form';
import FallbackForm from './forms/fallback-form';
import { useSafeState } from 'ahooks';
import BranchForm from './forms/branch-form';

const Undefined = (_, ref) => {
  useImperativeHandle(ref, () => ({
    handle: () => void 0,
  }));
  return <></>;
};

const GraphDrawer = (props: SelectionProps<GraphNode>) => {
  const t = useLocale(i18n);
  const dt = useLocale();
  const componentRef = useRef<ComponentHandle>();
  const forms = useMemo(
    () => ({
      project: {
        title: t['project.title'],
        component: ProjectForm,
      },
      sample: {
        title: t['sample.form.name'],
        component: SampleForm,
      },
      fallback: {
        title: t['fallback.title'],
        component: FallbackForm,
      },
      undefined: {
        title: t['unknown'],
        component: Undefined,
      },
      conversation: {
        title: t['branch.title'],
        component: BranchForm,
      },
    }),
    [t]
  );
  const [submitting, setSubmitting] = useSafeState(false);
  const handleOk = async () => {
    setSubmitting(true);
    try {
      await componentRef.current.handle();
      Message.success(dt['message.update.success']);
      props.onChangeEditSelection(null);
    } catch (error) {
      // no something
      console.error(error);
    }
    setSubmitting(false);
  };
  const handleCancel = () => {
    props.onChangeEditSelection(null);
  };
  const Component = useMemo(
    () =>
      forms[props.selection?.type]?.component
        ? forwardRef(forms[props.selection?.type].component)
        : null,
    [forms, props.selection?.type]
  );
  const title = forms[props.selection?.type]?.title;
  if (!Component) return null;
  return (
    <Drawer
      width={540}
      height={'calc(100vh - 51px'}
      bodyStyle={{ padding: 0 }}
      headerStyle={{ display: 'none' }}
      visible={!!props.selection}
      maskStyle={{ opacity: 0.1, cursor: 'not-allowed' }}
      maskClosable={false}
      escToExit={false}
      closable={false}
      footer={null}
      unmountOnExit
    >
      <Card
        title={title}
        style={{ height: '100%', background: 'var(--color-bg-1)' }}
        bordered={false}
        headerStyle={{
          borderBottom: '1px solid #ccc',
          padding: '0 10px',
          height: 51,
        }}
        extra={
          <Space>
            <Button
              type="primary"
              icon={<IconSave />}
              loading={submitting}
              onClick={handleOk}
            >
              {t['save']}
            </Button>
            <Button icon={<IconClose />} onClick={handleCancel}>
              {t['close']}
            </Button>
          </Space>
        }
      >
        <div style={{ height: 'calc(100vh - 100px)', overflowY: 'auto' }}>
          <Component ref={componentRef} {...props} />
        </div>
      </Card>
    </Drawer>
  );
};

export default GraphDrawer;
