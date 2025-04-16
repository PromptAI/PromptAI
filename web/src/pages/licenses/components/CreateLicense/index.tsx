import { freeLicense, packages } from '@/api/stripe';
import { Button, ButtonProps, Message, Modal } from '@arco-design/web-react';
import React, { useEffect, useState } from 'react';
import Plan from './Plan';
import useLocale from '@/utils/useLocale';
import i18n from '../../locale';

interface CreateLicenseProps extends Omit<ButtonProps, 'onClick' | 'loading'> {
  onSuccess: () => void;
}

const CreateLicense: React.FC<CreateLicenseProps> = ({
  onSuccess,
  ...props
}) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const [data, setData] = useState([]);

  // 从官网跳过来： https://hotshots.com/license?create=true
  // 此时打开选择框
  useEffect(() => {
    const search = window.location.search;
    const params = new URLSearchParams(search);
    const create = params.get('create');
    loadData();

    if ('true' === create) {
      setVisible(true);
    }
  }, []);

  function loadData() {
    const param = {
      type: 'license',
    };

    packages(param).then((res) => {
      setData(res);
    });
  }

  function onFreeSubscribe(data) {
    const body = {
      id: data.id,
    };
    freeLicense(body)
      .then((res) => {
        Message.success(t['success.create']);
        setVisible(false);
      })
      .finally(() => {
        onSuccess();
      });
    return;
  }

  return (
    <div>
      {/* 有可以购买的package 才显示create 按钮 */}
      {data && data.length >0 && <Button
          {...props}
          onClick={() => {
            setVisible(true);
            loadData();
          }}
      />}
      <Modal
        title={t['title']}
        visible={visible}
        style={{ width: 'auto' }}
        onOk={() => setVisible(false)}
        onCancel={() => setVisible(false)}
        autoFocus={false}
        focusLock={true}
      >
        {visible && (
          <div className={'flex space-x-4 justify-center'}>
            {data.map((item, index) => {
              return (
                <Plan key={index} data={item} onSubscribe={onFreeSubscribe} />
              );
            })}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default CreateLicense;
