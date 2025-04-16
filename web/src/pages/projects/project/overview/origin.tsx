import { listConversations, listFaqs } from '@/api/components';
import { infoProject } from '@/api/projects';
import { Tool, useTools } from '@/components/Layout/tools-context';
import { NodeDefined } from '@/core-next/types';
import GraphNext, { sampleSelect, unSelect } from '@/graph-next';
import { GraphNode } from '@/graph-next/type';
import { Spin } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React, { useCallback, useMemo, useState } from 'react';
import build from './build';
import GraphDrawer from './drawer';
import BranchNode from './nodes/branch-node';
import ComplexNode from './nodes/complex-node';
import FallbackNode from './nodes/fallback-node';
import ProjectNode from './nodes/proejct-node';
import SampleNode from './nodes/sample-node';
import './index.css';
import useUrlParams from '../hooks/useUrlParams';
import DebugRobot from '../components/debug/robot';
import { Tool as DebugRunTool } from '../components/debug';
import DownloadRasaFile from '../components/DownloadRasaFile';

async function fetchAll(projectId: string) {
  return Promise.all([
    infoProject(projectId),
    listConversations(projectId),
    listFaqs(projectId),
  ]);
}

const Overview = () => {
  const { projectId } = useUrlParams();

  const [graph, setGraph] = useState<GraphNode[]>([]);

  const { loading } = useRequest(() => fetchAll(projectId), {
    onSuccess: ([project, flows, faqs]) => {
      setGraph(build(project, flows, faqs));
    },
  });

  const [editSelection, setEditSelection] = useState<GraphNode>(null);

  const onSelect = useCallback((node) => {
    setGraph((val) => sampleSelect(val, node));
  }, []);
  const onCanvasClick = useCallback(() => {
    setEditSelection(null);
    setGraph((val) => unSelect(val));
  }, []);
  const onDoubleClick = useCallback((node) => setEditSelection(node), []);

  const nodes = useMemo<Record<string, NodeDefined>>(() => {
    const props = {
      projectId,
      onChangeEditSelection: setEditSelection,
    };
    return {
      project: {
        component: ProjectNode,
        props,
      },
      fallback: {
        component: FallbackNode,
        props,
      },
      sample: {
        component: SampleNode,
        props,
      },
      complex: {
        component: ComplexNode,
        props,
      },
      conversation: {
        component: BranchNode,
        props,
      },
    };
  }, [projectId]);

  const tools = useMemo<Tool[]>(() => {
    return [
      {
        key: 'run',
        component: <DebugRunTool current="" disabledCurrent />,
      },
      {
        key: 'download-rasa',
        component: <DownloadRasaFile />,
      },
      ,
    ];
  }, []);
  useTools(tools);
  const componentIds = useMemo(() => graph.map((g) => g.id), [graph]);
  return (
    <div>
      <Spin loading={loading} className="w-full">
        <GraphNext
          name="overview-graph"
          width="100%"
          height="calc(100vh - 100px)"
          nodes={nodes}
          value={graph}
          onSelect={onSelect}
          onDoubleSelect={onDoubleClick}
          onCanvasClick={onCanvasClick}
          canvasClassName="overview-graph"
        />
        <GraphDrawer
          projectId={projectId}
          selection={editSelection}
          onChange={setGraph}
          onChangeEditSelection={setEditSelection}
          graph={graph}
        />
      </Spin>
      <DebugRobot projectId={projectId} componentIds={componentIds} />
    </div>
  );
};

export default Overview;
