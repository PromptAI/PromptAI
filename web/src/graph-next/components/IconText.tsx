import { Typography } from '@arco-design/web-react';
import { IconSend, IconUser } from '@arco-design/web-react/icon';
import { GrRadialSelected } from 'react-icons/gr';
import React from 'react';

const IconText = ({
  icon,
  type = 'secondary',
  children,
  color,
  ...rest
}: {
  icon: React.ReactNode;
  color?: string;
  type?: 'secondary' | 'primary' | 'success' | 'error' | 'warning';
  children: string;
  rows?: number;
  onMouseEnter?: any;
  onMouseLeave?: any;
}) => {
  return (
    <div className="flex items-center gap-1" {...rest}>
      <Typography.Text type={type} className="flex justify-center items-center">
        {icon}
      </Typography.Text>
      <div
        style={{
          maxWidth: '220px',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          color: color || 'var(--color-text-1)',
          userSelect: 'none',
        }}
        title={children as string}
      >
        {children}
      </div>
    </div>
  );
};

export const RobotText = ({ children }: { children: string }) => (
  <IconText icon={<IconSend className="app-icon" />}>{children}</IconText>
);
export const UserText = ({
  children,
  type = 'secondary',
}: {
  children: string;
  type?: 'secondary' | 'primary' | 'success' | 'error' | 'warning';
}) => (
  <IconText icon={<IconUser className="app-icon" />} type={type}>
    {children}
  </IconText>
);
export const OptionText = ({ children }: { children: string }) => (
  <IconText icon={<GrRadialSelected className="app-icon" />} type="primary">
    {children}
  </IconText>
);

export default IconText;
