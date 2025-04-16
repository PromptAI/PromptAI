import GenerateExample, {
  GenerateExampleProps,
} from '@/pages/projects/project/components/GenerateExample';
import UserExt, {
  UserExtProps,
} from '@/pages/projects/project/components/UserExt';
import { FormInstance } from '@arco-design/web-react';
import { uniqBy } from 'lodash';
import React, { RefObject, useCallback } from 'react';

interface ExamplesExtraProps {
  label: string;
  formRef: RefObject<FormInstance>;
  generateProps?: Omit<GenerateExampleProps, 'onGenerated' | 'onGetParams'>;
  uploadProps?: Omit<UserExtProps, 'onFinish'>;
}
const ExamplesExtra: React.FC<ExamplesExtraProps> = ({
  label,
  formRef,
  generateProps,
  uploadProps,
}) => {
  const onUserExtChange = useCallback(
    (exts) => {
      const examples = [
        ...(formRef.current.getFieldValue('examples') || []),
        ...exts.map((text) => ({ text, marks: [] })),
      ];
      formRef.current.setFieldValue('examples', examples);
    },
    [formRef]
  );
  const onGenerated = useCallback(
    (intents: string[]) => {
      const origin = formRef.current.getFieldValue('examples') || [];
      const main = formRef.current.getFieldValue('mainExample');
      formRef.current.setFieldValue(
        'examples',
        uniqBy(
          [
            ...intents
              .filter((i) => i !== main?.text)
              .map((text) => ({ text, marks: [] })),
            ...origin,
          ],
          'text'
        )
      );
    },
    [formRef]
  );
  const onGetParams = useCallback(() => {
    const { mainExample, examples } = formRef.current.getFieldsValue([
      'mainExample',
      'examples',
    ]);
    return {
      intent: mainExample.text,
      count: 5,
      exts: uniqBy([mainExample, ...(examples || [])], 'text').map(
        ({ text }) => text
      ),
    };
  }, [formRef]);

  return (
    <div className="flex justify-between items-center w-full">
      <span>{label}</span>
      <div className="flex items-center space-x-2">
        <UserExt onFinish={onUserExtChange} {...uploadProps} />
        <GenerateExample
          onGenerated={onGenerated}
          onGetParams={onGetParams}
          {...generateProps}
        />
      </div>
    </div>
  );
};

export default ExamplesExtra;
