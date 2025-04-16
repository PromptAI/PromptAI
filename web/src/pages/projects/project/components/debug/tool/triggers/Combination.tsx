import useModalForm from '@/hooks/useModalForm';
import { useProjectContext } from '@/layout/project-layout/context';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Checkbox,
  Divider,
  Empty,
  Form,
  Modal,
  Space,
  Spin,
  Typography,
} from '@arco-design/web-react';
import { IconExclamationCircle } from '@arco-design/web-react/icon';
import { useSetState } from 'ahooks';
import React, { ReactNode, useImperativeHandle, useRef } from 'react';
import useDebugData from './fetch-data';
import { DebugOption, DebugRunProps } from './types';
import { isEmpty } from 'lodash';
import styled from 'styled-components';
import { useHistory } from 'react-router';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocalMemoried from '@/hooks/useLocalMemoried';

interface CheckComponentProps {
  value?: any;
  onChange?: (value?: any) => void;
  options: DebugOption[];
  title?: ReactNode;
  disabled?: boolean;
  type?: 'default' | 'link';
}
interface CheckComponentRef {
  selectAll: () => void;
  reverse: () => void;
}

const CheckGroupWrapper = styled(Checkbox.Group)`
  & > label {
    cursor: pointer !important;
    & > .arco-checkbox-text {
      color: var(--color-text-1);
      &:hover {
        color: var(--color-primary-light-4);
        text-decoration: underline;
      }
    }
  }
`;

const CheckComponent: React.ForwardRefRenderFunction<
  CheckComponentRef,
  CheckComponentProps
> = ({ value, onChange, options, title, disabled, type = 'default' }, ref) => {
  const params = useUrlParams();
  const history = useHistory();
  useImperativeHandle(ref, () => ({
    selectAll: () =>
      onChange(options.filter((o) => !o.disabled).map((o) => o.value)),
    reverse: () =>
      onChange(
        options
          .filter((o) => !o.disabled)
          .filter((o) => !value?.some((v) => v === o.value))
          .map((o) => o.value)
      ),
  }));
  const handleChange = (vals) => {
    onChange(vals);
  };
  return (
    <>
      {title}
      {options.length > 0 &&
        (type === 'default' ? (
          <Checkbox.Group
            value={value}
            onChange={handleChange}
            disabled={disabled}
            options={options}
          />
        ) : (
          <CheckGroupWrapper onChange={handleChange} disabled={disabled}>
            {options.map(({ label, value, type }) => (
              <Checkbox key={value} disabled>
                <span
                  onClick={() =>
                    history.push(
                      type === 'conversation'
                        ? `/projects/${params.projectId}/overview/complexs/${value}/branch/complex`
                        : `/projects/${params.projectId}/overview/knowledge/sample`
                    )
                  }
                >
                  {label}
                </span>
              </Checkbox>
            ))}
          </CheckGroupWrapper>
        ))}
      {!options.length && <Empty />}
    </>
  );
};
const CheckComp = React.forwardRef(CheckComponent);

const DebugCombination = ({ title, current, icon, start }: DebugRunProps) => {
  const t = useLocale(i18n);
  const { id: projectId } = useProjectContext();
  const [visible, setVisible, form] = useModalForm();

  const [selected, setSelected] = useLocalMemoried<string[]>(current, []);

  const [temp, setTemp] = useSetState({ available: [], unavailable: [] });
  const data = useDebugData(projectId, visible, (ops) => {
    setTemp({
      available: ops.filter((o) => !o.disabled),
      unavailable: ops
        .filter((o) => o.disabled)
        .map((o) => ({ ...o, disabled: false })),
    });
    form.setFieldsValue({
      available: ops.some((o) => o.value === current && o.disabled === false)
        ? [current, ...selected]
        : selected,
      unavailable: [],
    });
  });

  const handleOk = () => {
    if (data.loading) return;
    form.validate().then(({ available, unavailable }) => {
      const componentIds = [...available, ...unavailable];
      if (!isEmpty(componentIds)) {
        setSelected(componentIds);
        start(componentIds, projectId);
      }
      setVisible(false);
    });
  };
  const availableRef = useRef<CheckComponentRef>();
  const unavailableRef = useRef<CheckComponentRef>();

  return (
    <div>
      <Button type="text" onClick={() => setVisible(true)} icon={icon}>
        {title}
      </Button>
      <Modal
        style={{ width: 720 }}
        visible={visible}
        title={t['debug.combine.module.run.title']}
        onOk={handleOk}
        onCancel={() => setVisible(false)}
        unmountOnExit
      >
        <Spin loading={data.loading} className="w-full">
          <Form layout="vertical" form={form}>
            <Form.Item
              label={
                <div className="flex justify-between">
                  <Typography.Text>
                    {t['debug.combine.module.run.form.componentIds']}
                  </Typography.Text>
                  {temp.available.length > 0 && (
                    <Space>
                      <Button
                        size="mini"
                        type="outline"
                        onClick={() => availableRef.current.selectAll()}
                      >
                        {t['debug.combine.module.run.option.true']}
                      </Button>
                      <Button
                        size="mini"
                        type="outline"
                        onClick={() => availableRef.current.reverse()}
                      >
                        {t['debug.combine.module.run.option.false']}
                      </Button>
                    </Space>
                  )}
                </div>
              }
              field="available"
              style={{ marginBottom: 0 }}
            >
              <CheckComp ref={availableRef} options={temp.available} />
            </Form.Item>
            <Divider />
            <Form.Item
              label={
                <div className="flex justify-between">
                  <Typography.Text>
                    {t['debug.combine.module.run.form.unabled']}
                  </Typography.Text>
                </div>
              }
              field="unavailable"
              style={{ marginBottom: 0 }}
            >
              <CheckComp
                ref={unavailableRef}
                options={temp.unavailable}
                disabled
                type="link"
                title={
                  <Typography.Text type="warning">
                    <IconExclamationCircle />
                    {t['debug.combine.module.run.form.unabled.help']}
                  </Typography.Text>
                }
              />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>
    </div>
  );
};

export default DebugCombination;
