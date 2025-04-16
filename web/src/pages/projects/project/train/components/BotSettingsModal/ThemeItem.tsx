import { Checkbox, CheckboxProps } from '@arco-design/web-react';
import * as React from 'react';
import styled from 'styled-components';

export interface ThemeItemProps
  extends Pick<CheckboxProps, 'checked' | 'onChange'> {
  primary: string;
  borderRadius: number;
  name: string;
  description: string;
}
const Container = styled.div<{ checked?: boolean }>`
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  border-color: ${({ checked }) =>
    checked ? 'var(--color-border-2)' : 'transparent'};
  cursor: pointer;
  &:hover {
    border-color: var(--color-border-2);
  }
`;
const Box = styled.div<{ borderRadius: number; primary: string }>`
  width: 52px;
  height: 52px;
  background: ${({ primary }) => primary};
  border-radius: ${({ borderRadius }) => borderRadius + 'px'};
`;
const Content = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
`;
const Title = styled.span`
  line-height: 1;
  font-weight: 500;
`;
const Description = styled.p`
  margin: 0;
  font-size: 12px;
  font-weight: 300;
`;

const ThemeItem: React.FC<ThemeItemProps> = ({
  primary,
  borderRadius,
  name,
  description,
  ...props
}) => {
  return (
    <Container role="button" checked={props.checked}>
      <Box borderRadius={borderRadius} primary={primary}></Box>
      <Content>
        <Title>{name}</Title>
        <Description>{description}</Description>
      </Content>
      <Checkbox {...props} />
    </Container>
  );
};

export default ThemeItem;
