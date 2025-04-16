import React, { useMemo } from 'react';
import styled from 'styled-components';
import { IconCloseCircle } from '@arco-design/web-react/icon';
import { Divider, Space, Typography } from '@arco-design/web-react';
import { GraphNode } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const Container = styled.div`
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--color-fill-1);
  padding: 4px 8px;
  border-radius: 4px;
  max-width: 380px;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
`;

const DividerWrapper = styled(Divider)`
  margin: 4px 0;
`;

const ErrorList = styled.ul`
  padding-left: 16px;
  max-height: 100px;
  overflow-y: auto;
`;

const ErrorListItem = styled.li`
  &::marker {
    color: var(--color-text-1);
  }
`;

interface ErrorPanelProps {
  nodes: Omit<GraphNode, 'children' | 'parent'>[];
}
const ErrorPanel: React.FC<ErrorPanelProps> = ({ nodes }) => {
  const t = useLocale(i18n);
  const errors = useMemo(
    () =>
      nodes
        .filter((n) => !!n.validatorError)
        .map(({ id, validatorError }) => ({ id, validatorError })),
    [nodes]
  );
  const errorLengthComponent = useMemo(
    () =>
      `${t['error.panel.prefix']}${errors.length}${t['error.panel.subfix']}`,
    [errors, t]
  );
  if (errors.length < 1) return null;
  return (
    <Container>
      <Space size="mini">
        <Typography.Text type="error">
          <IconCloseCircle />
        </Typography.Text>
        <Typography.Text type="error">{errorLengthComponent}</Typography.Text>
      </Space>
      <DividerWrapper />
      <ErrorList>
        {errors.map(({ id, validatorError }) => (
          <ErrorListItem key={id}>
            <Typography.Text
              ellipsis={{ showTooltip: true }}
              style={{ marginBottom: 0 }}
            >
              {validatorError.errorMessage}
            </Typography.Text>
          </ErrorListItem>
        ))}
      </ErrorList>
    </Container>
  );
};

export default ErrorPanel;
