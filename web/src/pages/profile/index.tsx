import { UserState, useSelectorStore } from '@/store';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Grid,
  Image,
  Message,
  Space,
  Tabs,
  Typography,
  Upload,
} from '@arco-design/web-react';
import { IconPhone, IconEmail } from '@arco-design/web-react/icon';
import React, { useEffect, useState } from 'react';
import UpdatePwd from './UpdatePwd';
import i18n from './locale';
import { useCreation } from 'ahooks';
import Token from '@/utils/token';
import { UploadItem } from '@arco-design/web-react/es/Upload';

const { Row, Col } = Grid;
const Profile = () => {
  const t = useLocale(i18n);
  const user = useSelectorStore<UserState>('user');

  const [avatarSrc, setAvatarSrc] = useState<any>();
  useEffect(() => {
    if (user?.avatar) {
      if (user.avatar.startsWith('http')) {
        setAvatarSrc(user.avatar);
      } else {
        setAvatarSrc(`/api/blobs/get/${user?.avatar}`);
      }
    }
  }, [user?.avatar]);
  const headers = useCreation(() => ({ Authorization: Token.get() }), []);
  const onFileChange = async (_, upload: UploadItem) => {
    if (upload.status === 'done') {
      // const { id } = (upload.response as any) || {};
      const avatar = `/api/blobs/get/${upload.response}`;
      setAvatarSrc(avatar);
      window.location.reload();
    }
    if (upload.status === 'error') {
      Message.error(t['graph.bot.panel.image.response.error']);
    }
  };

  return (
    <Row style={{ marginTop: 32 }}>
      <Col offset={4} span={20}>
        <Space direction="vertical" className="w-full">
          <Row gutter={24} align="center">
            <Col flex="128px" className="flex items-center flex-col">
              <div
                style={{
                  width: 70,
                  height: 70,
                  overflow: 'hidden',
                  borderRadius: '50%',
                  display: 'inline-block',
                }}
              >
                <Image
                  width={'100%'}
                  height={'100%'}
                  alt="avatar"
                  src={user.avatar ? avatarSrc : '/user.png'}
                />
              </div>
              <Upload
                key="upload-image"
                accept="image/*"
                action="/api/settings/users/updateAvatar"
                showUploadList={false}
                headers={headers}
                onChange={onFileChange}
              >
                <Button type="text" size="mini">
                  {t['profile.Avatar.buttonText']}
                </Button>
              </Upload>
            </Col>
            <Col flex="auto" className="flex flex-col">
              <Typography.Title heading={2} style={{ marginTop: 42 }}>
                {t['profile.hello']}, {user.username}
              </Typography.Title>
              {user.mobile && (
                <Space>
                  <IconPhone />
                  <Typography.Text>{user.mobile}</Typography.Text>
                </Space>
              )}
              {user.email && (
                <Space>
                  <IconEmail />
                  <Typography.Text>{user.email}</Typography.Text>
                </Space>
              )}
            </Col>
          </Row>
          <Tabs defaultActiveTab="1" type="rounded">
            <Tabs.TabPane key="1" title={t['profile.tabPane_security.title']}>
              <UpdatePwd
                defaultPwd={user.initPass}
                sms={user.mobile}
                email={user.email}
              />
            </Tabs.TabPane>
          </Tabs>
        </Space>
      </Col>
    </Row>
  );
};

export default Profile;
