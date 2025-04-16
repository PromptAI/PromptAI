import TextAnnotation from '@/components/TextAnnotation';
import { Example } from '@/graph-next/type';
import React, { useMemo } from 'react';
import EnterBlurInput from '@/pages/projects/project/components/EnterBlurInput';

interface TextMarkProps {
  disabled?: boolean;
  canMark?: boolean;
  value?: Example;
  onChange?: (val: Example) => void;
  placeholder?: string;
}

const TextMark = ({
  disabled,
  canMark,
  value,
  onChange,
  placeholder,
}: TextMarkProps) => {
  const component = useMemo(
    () =>
      canMark ? (
        <TextAnnotation
          disabled={disabled}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
        />
      ) : (
        <EnterBlurInput
          disabled={disabled}
          placeholder={placeholder}
          value={value?.text}
          autoFocus
          onChange={(text) => onChange({ ...value, text })}
        />
      ),
    [canMark, disabled, onChange, placeholder, value]
  );
  return component;
};

export default TextMark;
