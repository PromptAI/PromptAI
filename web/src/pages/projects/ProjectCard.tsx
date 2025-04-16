import { deleteProject } from '@/api/projects';
import useLocale from '@/utils/useLocale';
import { Button, Message, Modal, Tooltip } from '@arco-design/web-react';
import moment from 'moment';
import React from 'react';
import { useHistory } from 'react-router';
import { IconDelete } from '@arco-design/web-react/icon';
import i18n from '@/pages/projects/locale';
import ProjectBox from '../components/ProjectBox';
import classNames from 'classnames';

interface StatusDotProps {
  label: string;
  color: string;
  pulse?: boolean;
}

const StatusDot = ({ label, color, pulse }: StatusDotProps) => {
  return (
    <div
      className={classNames('w-3 h-3 rounded-full', {
        'animate-pulse': pulse
      })}
      title={label}
      style={{
        backgroundColor: color
      }}
    />
  );
};

interface ProjectCardProps {
  project: any;
  refresh: () => void;
}

const ProjectCard = ({ project, refresh }: ProjectCardProps) => {
  const { id, name, publishedProject } = project;
  const history = useHistory();
  const t = useLocale();
  const pt = useLocale(i18n);
  const publishSate = {
    deploying: {
      text: pt['project.state.deployment'],
      color: '#F7BA1E'
    },
    running: {
      text: pt['project.state.Published'],
      color: '#009A29'
    },
    not_running: {
      text: pt['project.state.Unpublished'],
      color: '#6B7785'
    }
  };

  const onDel = (evt: Event) => {
    evt.stopPropagation();
    const modalIns = Modal.confirm({
      title: t['common.delete.confirm.title'],
      content: `${pt['project.delete.warning']}：${name}？`,
      footer: (cancel, ok) => (
        <>
          {/* 设置 ok 按钮的 type 为 warning */}
          <Button
            type="primary"
            onClick={async () => {
              await deleteProject(id);
              Message.success(t['message.delete.success']);
              refresh();
              modalIns.close();
            }}
          >
            {t['common.delete.confirm.button.ok']}
          </Button>
          <Button onClick={() => modalIns.close()}>
            {t['common.delete.confirm.button.cancel']}
          </Button>
        </>
      )
    });
  };
  return (
    <Tooltip content={pt['project.image.tooltip']}>
      <ProjectBox
        className="cursor-pointer"
        onClick={() => history.push(`/projects/${id}/tool/setting`)}
        project={project}
        extra={
          <Button
            size="small"
            shape="circle"
            icon={<IconDelete />}
            status="danger"
            onClick={onDel}
          />
        }
        footer={
          <div className="h-6 flex justify-between items-center px-2">
            <span>
              <span className="mr-1">{t['publish.update.time']}:</span>
              {publishedProject?.status === 'running' &&
              Boolean(Number(publishedProject?.properties?.updateTime)) ? (
                <span>
                  {`${moment(
                    publishedProject?.properties?.updateTime,
                    'x'
                  ).format('YYYY-MM-DD HH:mm:ss')}
                `}
                </span>
              ) : (
                '-'
              )}
            </span>
            {publishedProject ? (
              <StatusDot
                label={publishSate[publishedProject.status].text}
                color={publishSate[publishedProject.status].color}
                pulse={publishedProject.status === 'deploying'}
              />
            ) : (
              <StatusDot
                label={publishSate.not_running.text}
                color={publishSate.not_running.color}
              />
            )}
          </div>
        }
      />
    </Tooltip>
  );
};

export default ProjectCard;
