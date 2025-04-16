import { Checkbox, Space, Typography } from '@arco-design/web-react';
import React, { ReactNode } from 'react';
import styles from './content.module.less';

interface Item {
  label: string;
  value: string;
  tooltip: number;
  type: 'faq-root' | 'conversation';
  disabled?: boolean;
}
interface ContentProps {
  title: ReactNode;
  subTitle?: ReactNode;
  items: Item[];
  value: string[];
  onChange: (value: string[]) => void;
}
const Content = ({
  title,
  subTitle,
  items,
  value: values,
  onChange,
}: ContentProps) => {
  const handleChange = (checked, val) => {
    onChange(
      checked ? [...new Set([...values, val])] : values.filter((v) => v !== val)
    );
  };
  return (
    <Space className="w-full" direction="vertical">
      <div>{title}</div>
      <div>
        {subTitle}
        <div className={styles.content}>
          {items.map(({ label, value, disabled }) => (
            <div key={value} className={styles.contentItem}>
              {disabled ? (
                <Checkbox disabled>{label}</Checkbox>
              ) : (
                <Checkbox
                  checked={values.includes(value)}
                  onChange={(checked) => handleChange(checked, value)}
                  className={styles.checkbox}
                >
                  <Typography.Text
                    style={{
                      maxWidth: 140,
                      margin: 0,
                      display: 'inline-block',
                      lineHeight: 1,
                    }}
                    ellipsis={{ showTooltip: true }}
                  >
                    {label}
                  </Typography.Text>
                </Checkbox>
              )}
            </div>
          ))}
        </div>
      </div>
    </Space>
  );
};

export default Content;
