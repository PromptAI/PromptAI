import { createWebLib, parseChildLinks } from '@/api/text/web';
import useModalForm from '@/hooks/useModalForm';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Checkbox,
  Empty,
  Form,
  Input,
  Message,
  Modal,
  Spin,
  Switch,
} from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import React, { useEffect, useMemo, useState } from 'react';
import useUrlParams from '../../../hooks/useUrlParams';
import i18n from './locale';
import { useMutation } from 'react-query';
import { uniq } from 'lodash';
import bv from 'b-validate';

interface CreateTextLibProps {
  onSuccess: () => void;
}
const CreateWebLib = ({ onSuccess }: CreateTextLibProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form] = useModalForm();
  const { projectId } = useUrlParams();

  const [isEnableChildLinked, setisEnableChildLinked] = useState(true);

  const [filter, setFilter] = useState('');

  const {
    isLoading,
    data = { links: [], total: 0, filtered: 0 },
    mutate,
    reset,
  } = useMutation(parseChildLinks, {
    onSuccess: (res) => {
      setValueSelected(res.links, true);
      // update the selected links
      setSelected(res.links);
    },
  });

  const {
    selected = [],
    selectAll,
    setSelected,
    unSelectAll,
    isAllSelected,
    isPartialSelected,
    setValueSelected,
  } = Checkbox.useCheckbox(data.links);

  const onHandleParse = () => {
    const url = form.getFieldValue('url');
    if (url) {
      mutate({ projectId, url, filter });
    }
  };

  useEffect(() => {
    if (visible) {
      setisEnableChildLinked(true);
      reset();
      unSelectAll();
      setFilter('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible]);

  const state: any = Form.useFormState('url', form);

  const isDisabled = useMemo(() => {
    const validates = bv(state?.value, { type: 'url' });
    return !!validates.type.url.error || !!validates.string.isRequired.error;
  }, [state?.value]);

  const onSubmit = async () => {
    const { url, ...rest } = await form.validate();
    await createWebLib(projectId, { urls: uniq([url, ...selected]), ...rest });
    Message.success(t['create.success']);
    onSuccess();
    setVisible(false);
  };

  return (
    <div>
      <Button
        type="primary"
        icon={<IconPlus />}
        onClick={() => setVisible(true)}
      >
        {t['create.title']}
      </Button>
      <Modal
        style={{ width: '50%' }}
        title={t['create.title']}
        unmountOnExit
        visible={visible}
        onOk={onSubmit}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label={t['create.remark']} field="description">
            <Input placeholder={t['create.remark']} />
          </Form.Item>
          <Form.Item
            label={
              <span className="flex justify-between items-center w-full">
                <span>{t['create.content']}</span>
                <span className="flex items-center gap-2">
                  <span>{t['create.openChildLinked']}</span>
                  <Switch
                    checked={isEnableChildLinked}
                    onChange={setisEnableChildLinked}
                  />
                </span>
              </span>
            }
            field="url"
            rules={[{ required: true, type: 'url' }]}
            className="[&_>_.arco-form-label-item_>_label]:flex [&_>_.arco-form-label-item_>_label]:items-center"
          >
            <Input placeholder={t['create.content.placeholder']} />
          </Form.Item>
          {isEnableChildLinked && (
            <div className="border border-[var(--color-neutral-3)] rounded p-4">
              <Form.Item
                label={
                  <span className="flex items-center justify-between w-full">
                    <span>{t['create.childLink.filter']}</span>
                    <Button
                      type="primary"
                      size="small"
                      loading={isLoading}
                      onClick={onHandleParse}
                      disabled={isLoading || isDisabled}
                    >
                      {t['create.childLink.parse']}
                    </Button>
                  </span>
                }
                className="[&_>_.arco-form-label-item_>_label]:flex [&_>_.arco-form-label-item_>_label]:items-center"
              >
                <Input
                  placeholder={t['create.childLink.filter.placeholder']}
                  value={filter}
                  onChange={setFilter}
                  disabled={isLoading || isDisabled}
                />
              </Form.Item>
              <Form.Item
                label={
                  <span className="flex justify-between items-center w-full">
                    <span className="flex items-center gap-2">
                      <span>{t['create.childLink.links']}</span>
                      <span className="space-x-2 border border-[var(--color-neutral-3)] px-2 rounded">
                        <span className="text-sm">
                          {t['create.childLink.links.total']}
                          <b>{data.total}</b>
                        </span>
                        <span className="text-sm">
                          {t['create.childLink.links.filtered']}
                          <b>{data.filtered}</b>
                        </span>
                      </span>
                    </span>
                    <Checkbox
                      onChange={(checked) =>
                        checked ? selectAll() : unSelectAll()
                      }
                      checked={isAllSelected()}
                      indeterminate={isPartialSelected()}
                      disabled={isLoading || isDisabled || data.total === 0}
                    >
                      {t['create.childLink.all']}
                    </Checkbox>
                  </span>
                }
                className="[&_>_.arco-form-label-item_>_label]:flex [&_>_.arco-form-label-item_>_label]:items-center"
              >
                <Spin
                  loading={isLoading}
                  className="w-full [&_>_.arco-spin-children]:w-full"
                >
                  {data.total > 0 && (
                    <Checkbox.Group
                      direction="vertical"
                      value={selected}
                      options={data.links}
                      onChange={setSelected}
                      className="max-h-64 overflow-y-auto w-full border border-[var(--color-neutral-3)] px-2 rounded"
                      disabled={isDisabled}
                    />
                  )}
                  {data.total === 0 && <Empty />}
                </Spin>
              </Form.Item>
            </div>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default CreateWebLib;
