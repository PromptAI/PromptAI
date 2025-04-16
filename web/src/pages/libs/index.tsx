import React, { useMemo } from 'react';
import { Button, Card } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import { getPublics } from '@/api/lib';
import ProjectBox from '../components/ProjectBox';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import ImportCard from './ImportCard';
import { IconCloudDownload } from '@arco-design/web-react/icon';

const Libs = () => {
  const t = useLocale(i18n);
  const { loading, data: respnse } = useRequest(() => getPublics());

  const dataSource = useMemo(() => {
    if (respnse) {
      return respnse.data.map((c) => {
        return { ...c, ...c.properties };
      });
    }
    return [];
  }, [respnse]);

  return (
    <Card
      bordered={false}
      loading={loading}
      headerStyle={{ borderBottom: '1px solid rgb(var(--gray-2))' }}
    >
      <div className="flex flex-wrap">
        {dataSource.map((item) => (
          <ProjectBox
            key={item.id}
            project={item}
            extra={
              <ImportCard
                initialValues={item}
                trigger={
                  <Button
                    size="small"
                    type="primary"
                    icon={<IconCloudDownload />}
                    style={{ marginLeft: 8 }}
                  >
                    {t['lib.import']}
                  </Button>
                }
              />
            }
          />
        ))}
      </div>
    </Card>
  );
};

export default Libs;
