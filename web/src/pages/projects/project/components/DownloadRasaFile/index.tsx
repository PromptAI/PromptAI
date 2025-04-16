import { downloadRasaFile } from '@/api/components';
import { listDownloadItem } from '@/api/download-rasa';
import i18n from './locale';
import { downloadFile } from '@/utils/downloadObject';
import useLocale from '@/utils/useLocale';
import { Button, Modal, Tabs, Tooltip } from '@arco-design/web-react';
import { IconDownload } from '@arco-design/web-react/icon';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import useUrlParams from '../../hooks/useUrlParams';
import TabContent from './components/TabContent';
import useModel from '@/hooks/useModel';

const RASA_VERSION = process.env.REACT_APP_RASA_VERSION || '3.2.0';
const { TabPane } = Tabs;
const DownloadRasaFile = ({ flowId }: { flowId?: string }) => {
  const { projectId } = useUrlParams();
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(0);
  const [data, setData] = useState([]);
  useEffect(() => {
    if (visible) {
      listDownloadItem(projectId).then((res) =>
        setData(
          res.map((d) => ({
            label: d.name,
            value: d.id,
            disabled: !d.isReady,
            tooltip: Number(d.error?.length || 0),
            type: d.type,
          }))
        )
      );
    }
  }, [projectId, visible]);
  const current = useMemo(
    () => data.filter((d) => d.value === flowId),
    [data, flowId]
  );

  const initialSelections = useMemo(() => [flowId].filter(Boolean), [flowId]);
  const [selections, setSelections] = useModel(initialSelections);

  const initialActionTab = useRef(flowId ? '1' : '2');
  const [actionTab, setActionTab] = useState(initialActionTab.current);
  const handler = async () => {
    return downloadRasaFile({
      projectId,
      componentIds: selections,
      type: visible === 2 ? '_internal_json' : void 0,
    }).then((res) => {
      downloadFile(res, selections.join());
    });
  };
  const handleTabChange = (key) => {
    let selectionKeys = [];
    if (key === '1') {
      selectionKeys = data
        .filter((d) => !d.disabled && d.value === flowId)
        .map((d) => d.value);
    }
    if (key === '3') {
      selectionKeys = data.filter((d) => !d.disabled).map((d) => d.value);
    }
    setSelections(selectionKeys);
    setActionTab(key);
  };
  const onModalClose = () => {
    setSelections([flowId].filter(Boolean));
    setActionTab(initialActionTab.current);
    setVisible(0);
  };
  return (
    <>
      <Tooltip
        content={`${t['component.download.rasa.fiel.version']}${RASA_VERSION}`}
      >
        <Button
          type="text"
          size="small"
          onClick={() => setVisible(1)}
          icon={<IconDownload />}
        >
          {t['component.download.rasa.file.title']}
        </Button>
      </Tooltip>
      <Modal
        title={
          t[`component.download.rasa.${visible === 1 ? 'file' : 'json'}.title`]
        }
        visible={visible !== 0}
        unmountOnExit
        onCancel={onModalClose}
        onOk={handler}
        style={{ width: '45%' }}
      >
        <Tabs
          activeTab={actionTab}
          type="card-gutter"
          destroyOnHide
          onChange={handleTabChange}
        >
          {flowId && (
            <TabPane key="1" title={t['component.download.rasa.file.current']}>
              <TabContent
                data={current}
                value={selections}
                onChange={setSelections}
              />
            </TabPane>
          )}
          <TabPane key="2" title={t['component.download.rasa.file.combine']}>
            <TabContent
              data={data}
              value={selections}
              onChange={setSelections}
            />
          </TabPane>
          <TabPane key="3" title={t['component.download.rasa.file.all']}>
            <TabContent
              data={data}
              value={selections}
              onChange={setSelections}
            />
          </TabPane>
        </Tabs>
      </Modal>
    </>
  );
};

export default DownloadRasaFile;
