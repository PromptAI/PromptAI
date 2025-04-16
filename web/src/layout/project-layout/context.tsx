import { infoProject } from '@/api/projects';
import { Spin } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React, { createContext, useContext } from 'react';
import { useParams } from 'react-router';

interface ModalInfo {
  id: string;
  agentId: string;
  status: string;
  token: string;
}
export interface ProjectLayoutContextValue {
  id: string;
  name: string;
  locale?: 'zh' | 'en';
  image?: string;
  description?: string;
  welcome?: string;
  debugProject?: ModalInfo;
  publishedProject?: ModalInfo;
  type?: 'rasa' | 'llm';
  refresh: () => void;
}
const defaultProject: ProjectLayoutContextValue = {
  id: null,
  name: 'loading',
  refresh: () => void 0,
};
const ProjectLayoutContext =
  createContext<ProjectLayoutContextValue>(defaultProject);

export function ProjectLayoutContextProvider({ children }) {
  const { id } = useParams<{ id: string }>();
  const {
    loading,
    data = defaultProject,
    refresh,
  } = useRequest<ProjectLayoutContextValue, []>(() => infoProject(id), {
    refreshDeps: [id],
  });
  if (loading)
    return (
      <Spin
        size={32}
        className="w-full h-full justify-center items-center"
        style={{ display: 'flex' }}
      />
    );
  return (
    <ProjectLayoutContext.Provider value={{ ...data, refresh }}>
      {children}
    </ProjectLayoutContext.Provider>
  );
}
export function useProjectContext() {
  const context = useContext(ProjectLayoutContext);
  if (!context) {
    throw new Error('should be with ProjectLayoutContext');
  }
  return context;
}

export function useProjectType() {
  const { type = 'llm' } = useProjectContext();
  return type;
}
