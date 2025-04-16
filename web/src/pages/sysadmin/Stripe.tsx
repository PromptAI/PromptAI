import * as React from 'react';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  Checkbox,
  Form,
  Input,
  Message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  TableColumnProps,
  Tooltip,
} from '@arco-design/web-react';
import {
  addStripe,
  deleteStripe,
  stripeList,
  updateStripe,
} from '@/api/projects';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import {
  IconDelete,
  IconEdit,
  IconPlus,
  IconSync,
} from '@arco-design/web-react/icon';
import { useMutation } from 'react-query';
import styled from 'styled-components';
import formatMoney from '@/utils/CurrencyUtil';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
const { Item } = Form;

const StyledItem = styled(Item)`
  & .arco-form-label-item {
    width: 60px;
    flex-basis: 60px;
  }
`;

const CreatePackage = ({ data, refresh }) => {
  const t = useLocale(i18n);
  const rules = useRules();
  const [visible, setVisible, form] = useModalForm();
  const [loading, setLoading] = useState(false);
  const [type, setType] = useState(null);
  const [onSales, setOnSales] = useState(true);

  const onOk = () => {
    form
      .validate()
      .then((res) => {
        const newPackage =
          type === 'token'
            ? {
                ...res,
                onSales: onSales,
                token: {
                  token: res.token,
                  description: res.description,
                },
              }
            : {
                ...res,
                onSales: onSales,
                license: {
                  project: res.project,
                  flowInProject: res.flowInProject,
                  description: res.description,
                  validityPeriod: res.validityPeriod,
                },
              };

        addStripe(newPackage)
          .then(() => {
            Message.success(t['success']);
            setVisible(false);

            // 刷新界面
            refresh();
          })
          .finally(() => setLoading(false));
      })
      .catch((e) => {
        console.log(e);
      });
  };

  return (
    <>
      <div className={'flex items-center justify-end'}>
        <Button
          key="refresh"
          icon={<IconSync />}
          type="primary"
          className={
            'arco-btn arco-btn-secondary arco-btn-size-default arco-btn-shape-square'
          }
          style={{ marginRight: 10 }}
          onClick={() => {
            refresh();
          }}
        >
          Refresh
        </Button>
        <Button
          key="add"
          icon={<IconPlus />}
          type="primary"
          style={{ marginRight: 10 }}
          onClick={() => {
            setVisible(true);
          }}
        >
          Add
        </Button>
      </div>
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        title={t['sysadmin.stripe.create']}
        confirmLoading={loading}
      >
        <Form
          layout={'vertical'}
          form={form}
          labelCol={{ span: 7 }}
          wrapperCol={{ span: 17 }}
        >
          <StyledItem layout="horizontal" label={'OnSales'} field={'onSales'}>
            <Checkbox
              defaultChecked={true}
              onChange={(checked) => {
                setOnSales(checked);
              }}
            />
          </StyledItem>
          <Item
            label={t['sysadmin.stripe.name']}
            field={'name'}
            rules={[
              {
                validator: async (value, callback) => {
                  return new Promise((resolve) => {
                    if (!value) {
                      callback(t['sysadmin.stripe.name.required']);
                      resolve();
                      return;
                    }
                    if (data.find((item) => item.name === value)) {
                      callback(t['sysadmin.stripe.name.exists']);
                      resolve();
                      return;
                    }
                    if (value.length > 20) {
                      callback(t['sysadmin.stripe.name.max.length']);
                      resolve();
                      return;
                    }

                    resolve();
                  });
                },
              },
              ...rules,
            ]}
          >
            <Input
              autoFocus
              placeholder={t['sysadmin.stripe.name.placeholder']}
            />
          </Item>
          <Item
            label={
              type === 'license'
                ? t['sysadmin.stripe.price.not.edit']
                : t['sysadmin.stripe.price']
            }
            field={'salePrice'}
            rules={rules}
          >
            <Input
              autoFocus
              placeholder={t['sysadmin.stripe.price.placeholder']}
              type={'number'}
              min={1}
            />
          </Item>
          <Item
            label={
              type === 'license'
                ? t['sysadmin.stripe.origin.price.not.edit']
                : t['sysadmin.stripe.origin.price']
            }
            field={'originalPrice'}
            rules={rules}
          >
            <Input
              autoFocus
              placeholder={t['sysadmin.stripe.origin.price.placeholder']}
              type={'number'}
              min={1}
            />
          </Item>
          <Item label={t['sysadmin.stripe.type']} field={'type'} rules={rules}>
            <Select
              placeholder={t['sysadmin.stripe.type.placeholder']}
              allowClear
              onChange={(value) => {
                setType(value);
              }}
            >
              <Select.Option value="license">{'License'}</Select.Option>
              <Select.Option value="token">{'Token'}</Select.Option>
            </Select>
          </Item>

          {type === 'license' && (
            <>
              <Item
                field={'project'}
                label={t['sysadmin.stripe.project']}
                rules={rules}
              >
                <Input
                  type={'number'}
                  min={-1}
                  placeholder={t['sysadmin.stripe.project.placeholder']}
                  autoFocus
                />
              </Item>
              <Item
                field={'flowInProject'}
                label={t['sysadmin.stripe.flow']}
                rules={rules}
              >
                <Input
                  type={'number'}
                  min={-1}
                  placeholder={t['sysadmin.stripe.flow.placeholder']}
                />
              </Item>
              <Item
                field={'validityPeriod'}
                label={t['sysadmin.stripe.validity']}
                rules={[
                  {
                    validator: async (value, callback) => {
                      return new Promise((resolve) => {
                        if (value < 1) {
                          callback(t['sysadmin.stripe.validity.min']);
                          resolve();
                          return;
                        }
                        resolve();
                      });
                    },
                  },
                  ...rules,
                ]}
              >
                <Input
                  type={'number'}
                  min={1}
                  placeholder={t['sysadmin.stripe.validity.placeholder']}
                />
              </Item>
              <Item
                field={'freeToken'}
                label={t['sysadmin.stripe.free.token']}
                rules={[
                  {
                    validator: async (value, callback) => {
                      return new Promise((resolve) => {
                        if (value < 0) {
                          callback(t['sysadmin.stripe.free.token.min']);
                          resolve();
                          return;
                        }
                        resolve();
                      });
                    },
                  },
                  ...rules,
                ]}
              >
                <Input
                  type={'number'}
                  min={0}
                  placeholder={t['sysadmin.stripe.free.token.placeholder']}
                />
              </Item>
              <Item
                field={'description'}
                label={t['sysadmin.stripe.remark']}
                rules={rules}
              >
                <Input.TextArea
                  placeholder={t['sysadmin.stripe.remark.placeholder']}
                />
              </Item>
            </>
          )}

          {type === 'token' && (
            <>
              <Item
                field={'token'}
                label={'Token'}
                rules={[
                  {
                    validator: async (value, callback) => {
                      return new Promise((resolve) => {
                        if (value < -1) {
                          callback(t['sysadmin.stripe.token.min']);
                          resolve();
                        }
                        resolve();
                      });
                    },
                  },
                  ...rules,
                ]}
              >
                <Input
                  type={'number'}
                  placeholder={t['sysadmin.stripe.token.placeholder']}
                />
              </Item>

              <Item
                field={'description'}
                label={t['sysadmin.stripe.remark']}
                rules={rules}
              >
                <Input.TextArea
                  placeholder={t['sysadmin.stripe.remark.placeholder']}
                />
              </Item>
            </>
          )}
        </Form>
      </Modal>
    </>
  );
};

const UpdatePackage = ({ initialValues, dataRef, refresh }) => {
  const t = useLocale(i18n);
  const rules = useRules();
  const [visible, setVisible, form] = useModalForm(initialValues);
  const [loading, setLoading] = useState(false);
  const [type, setType] = useState(initialValues.type);

  const onOk = () => {
    try {
      form.validate().then((res) => {
        const body = {
          ...res,
          id: initialValues.id,
        };
        updateStripe(body)
          .then(() => {
            Message.success(t['success']);
            setVisible(false);
            // 刷新界面
            refresh();
          })
          .finally(() => setLoading(false));
      });
    } catch (e) {
      console.log(e);
    }
  };
  return (
    <>
      <Tooltip content={t['sysadmin.stripe.edit']}>
        <Button
          icon={<IconEdit />}
          type="text"
          size="small"
          onClick={() => setVisible(true)}
        />
      </Tooltip>
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        title={t['sysadmin.stripe.update']}
        confirmLoading={loading}
      >
        <Form
          layout={'vertical'}
          form={form}
          labelCol={{ span: 7 }}
          wrapperCol={{ span: 17 }}
        >
          <Item label={'OnSales'} field={'onSales'} rules={rules}>
            <Checkbox defaultChecked={true} />
          </Item>
          <Item
            label={t['sysadmin.stripe.name']}
            field={'name'}
            rules={[
              {
                validator: async (value, callback) => {
                  return new Promise((resolve) => {
                    if (!value) {
                      callback(t['sysadmin.stripe.name.required']);
                      resolve();
                      return;
                    }

                    if (value.length > 20) {
                      callback(t['sysadmin.stripe.name.max.length']);
                      resolve();
                      return;
                    }

                    resolve();
                  });
                },
              },
              ...rules,
            ]}
          >
            <Input
              autoFocus
              placeholder={t['sysadmin.stripe.name.placeholder']}
            />
          </Item>
          <Item
            label={
              type === 'license'
                ? t['sysadmin.stripe.price.not.edit']
                : t['sysadmin.stripe.price']
            }
            field={'salePrice'}
            rules={rules}
          >
            <Input
              disabled={type === 'license'}
              autoFocus
              placeholder={t['sysadmin.stripe.price.placeholder']}
              type={'number'}
              min={1}
            />
          </Item>
          <Item
            label={
              type === 'license'
                ? t['sysadmin.stripe.origin.price.not.edit']
                : t['sysadmin.stripe.origin.price']
            }
            field={'originalPrice'}
            rules={rules}
          >
            <Input
              disabled={type === 'license'}
              autoFocus
              placeholder={t['sysadmin.stripe.origin.price.placeholder']}
              type={'number'}
              min={1}
            />
          </Item>
          <Item label={t['sysadmin.stripe.type']} field={'type'} rules={rules}>
            <Select
              disabled
              placeholder={t['sysadmin.stripe.type.placeholder']}
              allowClear
              onChange={(value) => {
                setType(value);
              }}
            >
              <Select.Option value="license">{'License'}</Select.Option>
              <Select.Option value="token">{'Token'}</Select.Option>
            </Select>
          </Item>

          {type === 'license' && (
            <>
              <Item
                field={'license.project'}
                label={t['sysadmin.stripe.project']}
                rules={rules}
              >
                <Input
                  type={'number'}
                  min={-1}
                  placeholder={t['sysadmin.stripe.project.placeholder']}
                  autoFocus
                />
              </Item>
              <Item
                field={'license.flowInProject'}
                label={t['sysadmin.stripe.flow']}
                rules={rules}
              >
                <Input
                  type={'number'}
                  min={-1}
                  placeholder={t['sysadmin.stripe.flow.placeholder']}
                />
              </Item>
              <Item
                field={'license.validityPeriod'}
                label={t['sysadmin.stripe.validity']}
                rules={[
                  {
                    validator: async (value, callback) => {
                      return new Promise((resolve) => {
                        if (value < 1) {
                          callback(t['sysadmin.stripe.validity.min']);
                        }
                        resolve();
                      });
                    },
                  },
                  ...rules,
                ]}
              >
                <Input
                  type={'number'}
                  min={1}
                  placeholder={t['sysadmin.stripe.validity.placeholder']}
                />
              </Item>
              <Item
                field={'license.freeToken'}
                label={t['sysadmin.stripe.free.token']}
                rules={[
                  {
                    validator: async (value, callback) => {
                      return new Promise((resolve) => {
                        if (value < 0) {
                          callback(t['sysadmin.stripe.free.token.min']);
                        }
                        resolve();
                      });
                    },
                  },
                  ...rules,
                ]}
              >
                <Input
                  type={'number'}
                  min={0}
                  placeholder={t['sysadmin.stripe.free.token.placeholder']}
                />
              </Item>
              <Item
                field={'license.description'}
                label={t['sysadmin.stripe.remark']}
                rules={rules}
              >
                <Input.TextArea
                  placeholder={t['sysadmin.stripe.remark.placeholder']}
                />
              </Item>
            </>
          )}

          {type === 'token' && (
            <>
              <Item
                field={'token.token'}
                label={'Token'}
                rules={[
                  {
                    validator: async (value, callback) => {
                      return new Promise((resolve) => {
                        if (value < -1) {
                          callback(t['sysadmin.stripe.token.min']);
                          resolve();
                        }
                        resolve();
                      });
                    },
                  },
                  ...rules,
                ]}
              >
                <Input
                  type={'number'}
                  placeholder={t['sysadmin.stripe.token.placeholder']}
                />
              </Item>

              <Item
                field={'token.description'}
                label={t['sysadmin.stripe.remark']}
                rules={rules}
              >
                <Input.TextArea
                  placeholder={t['sysadmin.stripe.remark.placeholder']}
                />
              </Item>
            </>
          )}
        </Form>
      </Modal>
    </>
  );
};

interface EnableColumnProps {
  row: any;
  onSuccess: () => void;
}

const EnableColumn: React.FC<EnableColumnProps> = ({ row, onSuccess }) => {
  const t = useLocale(i18n);
  const { isLoading, mutate } = useMutation(updateStripe, {
    onSuccess: (data) => {
      Message.success(t['success']);
      onSuccess();
    },
  });
  return (
    <Spin loading={isLoading}>
      <Checkbox
        disabled={isLoading}
        checked={row.onSales}
        onChange={(enable) => mutate({ ...row, onSales: enable })}
      />
    </Spin>
  );
};

const Stripe = () => {
  const t = useLocale(i18n);
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const dataRef = useRef(null);

  function handleDelete(item) {
    setLoading(true);
    const id = item.id;
    deleteStripe(id)
      .then(() => {
        Message.success(t['success']);
        // 刷新界面
        loadData();
      })
      .finally(() => setLoading(false));
  }

  function loadData() {
    setLoading(true);
    stripeList()
      .then((res) => {
        setData(res);
        dataRef.current = res;
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  //  from - column
  const columns = useMemo<TableColumnProps[]>(() => {
    return [
      {
        title: 'OnSales',
        dataIndex: 'onSales',
        width: 60,
        align: 'center',
        render: (_, row) => <EnableColumn row={row} onSuccess={loadData} />,
      },
      {
        title: t['sysadmin.stripe.name'],
        dataIndex: 'name',
      },
      {
        title: t['sysadmin.stripe.price'],
        dataIndex: 'salePrice',
        render: (_, item) => {
          return formatMoney(item.salePrice / 100);
        },
      },
      {
        title: t['sysadmin.stripe.origin.price'],
        dataIndex: 'originalPrice',
        render: (_, item) => {
          return formatMoney(item.originalPrice / 100);
        },
      },
      {
        title: t['sysadmin.stripe.type'],
        dataIndex: 'type',
        render: (_, item) => {
          switch (item.type) {
            case 'license':
              return 'License';
            case 'token':
              return 'Token';
          }
        },
      },
      {
        title: t['sysadmin.stripe.operation'],
        width: 80,
        align: 'center',
        render: (_, item) => (
          <Space size={'mini'}>
            <UpdatePackage
              initialValues={item}
              dataRef={dataRef}
              refresh={loadData}
            />
            <Tooltip content={'Delete'}>
              <Popconfirm
                position={'tr'}
                title={t['sysadmin.stripe.confirm.delete']}
                icon={<IconDelete style={{ color: '#F53F3F' }} />}
                onOk={() => {
                  handleDelete(item);
                }}
                okText={t['sysadmin.stripe.delete']}
              >
                <Button status={'danger'} type="text" icon={<IconDelete />} />
              </Popconfirm>
            </Tooltip>
          </Space>
        ),
      },
    ];
  }, []);

  return (
    <>
      <Space direction={'vertical'} className={'w-full'}>
        <CreatePackage data={data} refresh={loadData} />
        <Table
          size="small"
          loading={loading}
          columns={columns}
          data={data}
          rowKey="id"
        />
      </Space>
    </>
  );
};

export default Stripe;
