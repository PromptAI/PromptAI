import useLocale from '@/utils/useLocale';
import { Tag } from '@arco-design/web-react';
import classNames from 'classnames';
import React, { ComponentProps, ReactNode } from 'react';
import i18n from './i18n';

interface ProjectBoxProps extends ComponentProps<'div'> {
  project: any;
  extra?: ReactNode;
  footer?: ReactNode;
}
const ProjectBox: React.FC<ProjectBoxProps> = ({
  project,
  extra,
  footer,
  className,
  ...props
}) => {
  const { image, description, name, type } = project;
  const imageUrl = image == undefined ? "favicon.jpg" : image;
  const t = useLocale(i18n);
  return (
    <div className="w-1/4 p-2">
      <div
        className={classNames(
          'p-1 pb-2 space-y-4 rounded border border-[var(--color-border-2)] hover:shadow',
          className
        )}
        {...props}
      >
        <div className="aspect-[16/9] overflow-hidden">
          <img
            src={imageUrl}
            alt="project_image"
            className="object-cover h-full w-full"
          />
        </div>
        <div className="px-2 flex justify-between items-center">
          <div className="flex items-center gap-2">
            {type !== 'rasa' && (
              <Tag color="blue" size="small">
                {t['project.type.llm']}
              </Tag>
            )}
            <div className="font-bold max-w-[120px] truncate" title={name}>
              {name}
            </div>
          </div>
          {extra}
        </div>
        <div title={description} className="h-6 truncate px-2">
          {description}
        </div>
        {footer}
      </div>
    </div>
  );
};

export default ProjectBox;
