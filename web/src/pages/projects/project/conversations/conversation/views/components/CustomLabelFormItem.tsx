import { Form } from '@arco-design/web-react';
import { styled } from 'styled-components';

const CustomLableFormItem = styled(Form.Item)`
  & > .arco-form-label-item > label {
    display: flex;
    align-items: center;
    gap: 4px;
  }
`;
export default CustomLableFormItem;
