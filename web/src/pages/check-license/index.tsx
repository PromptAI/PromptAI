import { getLicense, setLicense } from '@/api/licenses';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Form,
  FormInstance,
  Input,
  Link,
  Table,
  TableColumnProps,
  Typography,
} from '@arco-design/web-react';
import { IconQuestionCircle } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import moment from 'moment';
import React, { useMemo, useRef, useState } from 'react';
import styled from 'styled-components';
import { useHistory } from 'react-router';

const Container = styled.section`
  width: 100%;
  max-width: 1124px;
  margin: 0 auto;
  padding: 96px 16px 0;
  font-size: 16px;
`;
const Title = styled.div`
  text-align: center;
  margin-bottom: 32px;
`;
const ButtonWrap = styled(Button)`
  margin-top: 32px;
  font-weight: bold;
`;
const LicenseImageContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
`;
const LinkWrap = styled(Link)`
  text-decoration: underline;
`;
const TableWrap = styled(Table)`
  margin-top: 32px;
`;

const CheckLicense = () => {
  const t = useLocale(i18n);
  const nav = useHistory();
  const formRef = useRef<FormInstance>();
  const { loading, data, refresh } = useRequest(() => getLicense(), {
    onSuccess: (values) => formRef.current.setFieldsValue(values),
  });
  const [submiting, setSubmiting] = useState(false);
  const onSubmit = async ({ license }) => {
    setSubmiting(true);
    try {
      await setLicense({ license });
      refresh();
    } catch (e) {
      //
    } finally {
      setSubmiting(false);
    }
  };
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        dataIndex: 'authorization.name',
        title: t['table.name'],
        align: 'center',
      },
      {
        dataIndex: 'authorization.flowInProject',
        title: t['table.flowInProject'],
        align: 'center',
      },
      {
        dataIndex: 'authorization.project',
        title: t['table.project'],
        align: 'center',
      },
      {
        dataIndex: 'authorization.expiredAt',
        title: t['table.expireAt'],
        align: 'center',
        render: (_, row) => (
          <Typography.Text>
            {moment(Number(row.authorization.expiredAt)).format(
              'YYYY-MM-DD HH:mm:ss'
            )}
          </Typography.Text>
        ),
      },
      // {
      //   dataIndex: 'authorization.validDay',
      //   title: t['table.validDay'],
      //   align: 'center',
      // },
    ],
    [t]
  );
  return (
    <Container>
      <Title>
        <Typography.Title heading={1}>{t['title']}</Typography.Title>
        <Typography.Paragraph
          style={{ fontSize: 16, fontWeight: 'bold' }}
          type={data?.authorization.status === 'valid' ? 'success' : 'error'}
        >
          {`${t['prefix']}${data?.lastCheckResult || '-'}`}
        </Typography.Paragraph>
      </Title>

      <Form layout="vertical" ref={formRef} onSubmit={onSubmit}>
        <Card loading={loading}>
          <Form.Item
            label={t['form.license']}
            field="license"
            rules={[{ required: true }]}
            extra={
              <LinkWrap
                href="https://doc.promptai.us/docs/local_deployment/#how-to-get-a-free-license"
                target="_blank"
              >
                <IconQuestionCircle style={{ marginRight: 4 }} />
                {t['form.license.link']}
              </LinkWrap>
            }
          >
            <Input />
          </Form.Item>
        </Card>
        {data?.authorization.status === 'valid' && (
          <TableWrap
            columns={columns}
            data={[data]}
            size="small"
            rowKey="license"
            loading={loading}
            pagination={false}
          />
        )}
        <ButtonWrap
          htmlType="submit"
          type="primary"
          long
          size="large"
          loading={submiting}
        >
          {t['submitting']}
        </ButtonWrap>
        {data?.authorization.status === 'valid' && (
          <ButtonWrap
            type="primary"
            long
            size="large"
            status="success"
            onClick={() => nav.replace('/projects')}
          >
            {t['valided']}
          </ButtonWrap>
        )}
      </Form>
      <LicenseImageContainer>
        <img src="/license.avif" alt="license-image" />
      </LicenseImageContainer>
    </Container>
  );
};

export default CheckLicense;
