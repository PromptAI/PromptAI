import { Tag } from '@arco-design/web-react';
import React from 'react';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { useHistory } from 'react-router';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

export const linkMap = {
  'faq-root': {
    link: (projectId) => `/projects/${projectId}/overview/knowledge/sample`,
    color: 'green',
    i18n: 'link.faq',
  },
  faq: {
    link: (projectId) => `/projects/${projectId}/overview/knowledge/sample`,
    color: 'green',
    i18n: 'link.faq',
  },
  conversation: {
    link: (projectId, src) =>
      `/projects/${projectId}/overview/complexs/${src.id}/branch/complex`,
    color: 'blue',
    i18n: 'link.flow',
  },
  fallback: {
    link: (projectId) => `/projects/${projectId}/overview`,
    color: 'orange',
    i18n: 'link.fallback',
  },
  text: {
    link: (projectId) => `/projects/${projectId}/overview`,
    color: 'orange',
    i18n: 'link.fallback.text',
  },
  // talk2bits: {
  //   link: (projectId) => `/projects/${projectId}/overview`,
  //   color: 'orange',
  //   i18n: 'link.fallback.talk2bits',
  // },
  action: {
    link: (projectId) => `/projects/${projectId}/overview`,
    color: 'orange',
    i18n: 'link.fallback.action',
  },
  webhook: {
    link: (projectId) => `/projects/${projectId}/overview`,
    color: 'orange',
    i18n: 'link.fallback.webhook',
  },
  kbqa: {
    link: (projectId) => `/projects/${projectId}/overview/knowledge/sample`,
    color: 'purple',
    i18n: 'link.kbqa',
  },
  llm: {
    link: (projectId) => `/projects/${projectId}/overview/knowledge/sample`,
    color: 'purple',
    i18n: 'link.llm',
  },
};
const SourceColumn = ({ item }) => {
  const { projectId } = useUrlParams();
  const history = useHistory();
  const t = useLocale(i18n);
  return (
    <div style={{ display: 'inline-flex', flexWrap: 'wrap' }}>
      {(item.rootComponents?.length || 0) > 0 ? (
        item.rootComponents.map((src, index) => (
          <Tag
            key={index}
            color={linkMap[src.type]?.color}
            style={{
              cursor: 'pointer',
              margin: '0 5px 5px 0',
              maxWidth: 180,
            }}
            onClick={() =>
              linkMap[src.type] &&
              history.push(linkMap[src.type].link(projectId, src))
            }
          >
            {`${src.name || '-'}(${t[`${linkMap[src.type]?.i18n}`] || '-'})`}
          </Tag>
        ))
      ) : (
        <span>-</span>
      )}
    </div>
  );
};

export default SourceColumn;
