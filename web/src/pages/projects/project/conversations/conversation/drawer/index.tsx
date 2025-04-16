import { GraphNode } from '@/graph-next/type';
import React, {
  forwardRef,
  useMemo,
  useRef,
  useState,
  useImperativeHandle,
  useEffect,
} from 'react';
import { SelectionProps } from '../types';
import {
  Button,
  Card,
  Drawer,
  Message,
  Modal,
  Space,
} from '@arco-design/web-react';
import { IconClose, IconSave } from '@arco-design/web-react/icon';
import { ComponentHandle } from './type';
import ConversationForm from './ConversationForm';
import IntentForm from './IntentForm';
import BotForm from './BotForm';
import BreakForm from './BreakForm';
import ConditionForm from './ConditionForm';
import FormNodeForm from './FormNodeForm';
import RhetoricalForm from './RhetoricalForm';
import FieldForm from './FieldForm';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { useUpdateEffect } from 'ahooks';
import { isEqual } from 'lodash';
import { Prompt } from 'react-router-dom';
import GotoForm from './GotoForm';
import SlotsForm from './SlotsForm';

const Undefined = (props, ref) => {
  useImperativeHandle(ref, () => ({
    handle() {
      /* igonre */
    },
  }));
  return <></>;
};

interface CacheProps {
  extend: SelectionProps<GraphNode>;
  onUpdatedChange: (changed: boolean) => void;
}
const Cache = ({ extend: { selection }, onUpdatedChange }: CacheProps) => {
  const cacheRef = useRef(selection);
  useUpdateEffect(() => {
    if (selection) {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { children: c, subChildren: cs, ...cacheData } = cacheRef.current;
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { children: s, subChildren: ss, ...selectionData } = selection;
      onUpdatedChange(!isEqual(cacheData, selectionData));
    }
  }, [selection]);
  return <div />;
};

const GraphDrawer = (props: SelectionProps<GraphNode>) => {
  const isUpdated = useRef(false);
  const t = useLocale(i18n);
  const forms = useMemo(
    () => ({
      conversation: {
        title: t['drawer.title.conversation'],
        component: ConversationForm,
      },
      user: {
        title: t['drawer.title.intent'],
        component: IntentForm,
      },
      bot: {
        title: t['drawer.title.bot'],
        component: BotForm,
      },
      break: {
        title: t['drawer.title.form.out'],
        component: BreakForm,
      },
      condition: {
        title: t['drawer.title.form.condition'],
        component: ConditionForm,
      },
      form: {
        title: t['drawer.title.form'],
        component: FormNodeForm,
      },
      rhetorical: {
        title: t['drawer.title.form.rhetorical'],
        component: RhetoricalForm,
      },
      field: {
        title: t['drawer.title.form.field'],
        component: FieldForm,
      },
      goto: {
        title: t['drawer.title.goto'],
        component: GotoForm,
      },
      slots: {
        title: t['drawer.title.form.slots'],
        component: SlotsForm,
      },
      undefined: {
        title: t['drawer.title.undefined'],
        component: Undefined,
      },
    }),
    [t]
  );
  const componentRef = useRef<ComponentHandle>();
  const [submitting, setSubmitting] = useState(false);
  const handleOk = async () => {
    setSubmitting(true);
    componentRef.current
      ?.handle()
      .then(() => {
        Message.success(t['save.success']);
        props.onChangeEditSelection(null);
        props.refresh();
      })
      .catch((error) => {
        if (typeof error === 'string') {
          Message.error(error);
        }
      })
      .finally(() => {
        setSubmitting(false);
      });
  };
  const message = useMemo(() => t['conversation.drawer.leave.message'], [t]);
  useEffect(() => {
    const handle = (evt) => {
      if (isUpdated.current) {
        evt.preventDefault();
        evt.returnValue = message;
        return message;
      }
    };
    window.addEventListener('beforeunload', handle);
    return () => {
      window.removeEventListener('beforeunload', handle);
    };
  }, [message]);
  const handleCancel = () => {
    if (isUpdated.current) {
      Modal.confirm({
        title: t['drawer.data.change.warning'],
        okText: t['drawer.data.change.okText'],
        cancelText: t['drawer.data.change.cancelText'],
        onOk() {
          return handleOk();
        },
        onCancel() {
          props.onChangeEditSelection(null);
          props.refresh();
        },
        footer: (cancel, ok) => (
          <>
            {ok}
            {cancel}
          </>
        ),
      });
    } else {
      props.onChangeEditSelection(null);
      props.refresh();
    }
  };
  const Component = useMemo(
    () =>
      forms[props.selection?.type]?.component
        ? forwardRef(forms[props.selection?.type].component)
        : null,
    [forms, props.selection?.type]
  );
  const componentPropsProxy = useMemo(
    () => ({
      ...props,
      onChangeEditSelection: (value: React.SetStateAction<GraphNode>) => {
        props.onChangeEditSelection(value);
        props.onChangeSelection(value);
      },
    }),
    [props]
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
        style={{
          height: '100%',
          background: 'var(--color-bg-1)',
        }}
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
          <Component ref={componentRef} {...componentPropsProxy} />
        </div>
      </Card>
      <Cache
        extend={props}
        onUpdatedChange={(changed) => (isUpdated.current = changed)}
      />
      <Prompt when={isUpdated.current} message={message} />
    </Drawer>
  );
};

export default GraphDrawer;
