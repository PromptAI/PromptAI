import MarkAnnotation, {
  MarkAnnotationProps,
} from '@/components/MarkAnnotation';
import React, { useCallback } from 'react';
import TextArea, { TextAreaProps } from '../../../../../components/TextArea';
import { Example as ExampleType } from '@/graph-next/type';

export interface ExampleProps
  extends MarkAnnotationProps,
    Omit<ExampleTextAreaProps, 'value' | 'onChange'> {
  markEnable?: boolean;
}
const Example: React.FC<ExampleProps> = ({ markEnable, ...props }) => {
  if (markEnable) return <MarkAnnotation {...props} />;
  return <ExampleTextArea {...props} />;
};

export interface ExampleTextAreaProps
  extends Omit<TextAreaProps, 'value' | 'onChange'> {
  value?: ExampleType;
  onChange?: (value: ExampleType) => void;
}

export const ExampleTextArea: React.FC<ExampleTextAreaProps> = ({
  value,
  onChange,
  ...props
}) => {
  const onChangeWrap = useCallback(
    (text: string) => onChange?.({ text, marks: [] }),
    [onChange]
  );
  return <TextArea {...props} value={value?.text} onChange={onChangeWrap} />;
};

export default Example;
