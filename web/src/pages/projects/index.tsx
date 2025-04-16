import React from 'react';
import { Card, Empty } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import { listProjects } from '@/api/projects';
import { Link } from 'react-router-dom';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import ProjectCard from './ProjectCard';
import CreateProject from './create-project';
import ImportYamlProject from '@/pages/projects/import-yaml-project';

const defaultResponse = { contents: [] };
const Projects = () => {
  const t = useLocale(i18n);
  const {
    loading,
    data: { contents } = defaultResponse,
    refresh,
  } = useRequest(() => listProjects());

  return (
    <Card
      bordered={false}
      loading={loading}
      extra={<div className={"flex flex-row gap-2"}>
        <CreateProject />
        <ImportYamlProject />
      </div>}
      headerStyle={{ borderBottom: '1px solid rgb(var(--gray-2))' }}
    >
      <div className="flex flex-wrap">
        {contents.map((item) => (
          <ProjectCard key={item.id} project={item} refresh={refresh} />
        ))}
        {!contents.length && (
          <Empty description={<Link to="/libs">{t['project.import']}</Link>} />
        )}
      </div>
    </Card>
  );
};

export default Projects;
