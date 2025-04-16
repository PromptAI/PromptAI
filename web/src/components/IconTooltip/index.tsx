import useLocale from '@/utils/useLocale';
import { Button, Tooltip } from '@arco-design/web-react';
import {
  IconCheck,
  IconClose,
  IconCommon,
  IconDragArrow,
  IconExperiment,
  IconMore,
  IconPlayArrow,
  IconPlus,
  IconStar,
  IconThunderbolt,
  IconUpload,
} from '@arco-design/web-react/icon';
import { FcFaq } from 'react-icons/fc';
import React from 'react';
import i18n from './locale';

interface ClickProps {
  className?: string;
  onClick?: () => void;
}
interface IconTooltipProps {
  onClick?: () => void;
  icon: React.ReactNode;
  status?: 'default' | 'success' | 'warning' | 'danger';
  type?: 'default' | 'primary' | 'secondary' | 'dashed' | 'text' | 'outline';
  content?: React.ReactNode;
  className?: string;
}
export const IconTooltip = ({
  onClick,
  icon,
  status = 'default',
  content,
  type = 'secondary',
  className,
}: IconTooltipProps) => {
  return (
    <Tooltip content={content}>
      <Button
        className={className}
        size="small"
        type={type}
        shape="circle"
        status={status}
        icon={icon}
        onClick={onClick}
      />
    </Tooltip>
  );
};

export const PlusToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      type="primary"
      onClick={onClick}
      icon={<IconPlus />}
      content={t['component.icon-tooltip.plus']}
    />
  );
};
export const CheckToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      icon={<IconCheck />}
      status="success"
      content={t['component.icon-tooltip.check']}
    />
  );
};
export const CloseToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      icon={<IconClose />}
      status="danger"
      content={t['component.icon-tooltip.close']}
    />
  );
};
export const ThunderboltToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      icon={<IconThunderbolt />}
      content={t['component.icon-tooltip.thunderbolt']}
    />
  );
};
export const DragMoveTootip = ({ className }: ClickProps) => {
  const t = useLocale(i18n);
  return (
    <IconTooltip
      icon={<IconDragArrow />}
      className={className || 'move-pointer'}
      content={t['component.icon-tooltip.dragdot']}
    />
  );
};
export const UploadToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      icon={<IconUpload />}
      status="warning"
      content={t['component.icon-tooltip.upload']}
    />
  );
};
export const CommonToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      type="primary"
      icon={<IconCommon />}
      content={t['component.icon-tooltip.common']}
    />
  );
};
export const ExperimentToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      status="warning"
      type="primary"
      icon={<IconExperiment />}
      content={t['component.icon-tooltip.experiment']}
    />
  );
};
export const PlayArrowToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      status="success"
      type="primary"
      icon={<IconPlayArrow />}
      content={t['component.icon-tooltip.playArrow']}
    />
  );
};
export const FaqToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      status="success"
      type="text"
      icon={<FcFaq />}
      content={t['component.icon-tooltip.faq']}
    />
  );
};
export const MoreToolip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      type="text"
      icon={<IconMore />}
      content={t['component.icon-tooltip.more']}
    />
  );
};
export const StarTooltip = (props: ClickProps) => {
  const { onClick } = props;
  const t = useLocale(i18n);
  return (
    <IconTooltip
      onClick={onClick}
      icon={<IconStar />}
      content={t['component.icon-tooltip.star']}
    />
  );
};
export default IconTooltip;
