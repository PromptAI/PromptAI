import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { DataType, KeyOption, ReplyVariable } from './@types';
import {
  Button,
  Empty,
  Message,
  Modal,
  Popconfirm,
  Space,
  Table,
} from '@arco-design/web-react';
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';
import { ColumnProps } from '@arco-design/web-react/es/Table';
import { nanoid } from 'nanoid';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { ID_KEY, RES_KEY } from './const';
import EditReplyVariable from './EditReplyVriable';
import { cloneDeep, isEmpty, isEqual, keyBy, xor } from 'lodash';
import { isBlank } from '@/utils/is';
import { decodeAttachmentText } from '@/utils/attachment';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import AddReplyVariable from './AddReplyVriable';
import SlotSelecter from '../SlotSelecter';

function isValidateData(value: DataType) {
  if (!value) return false;
  let flag = true;
  for (const [k, v] of Object.entries(value)) {
    if (k === ID_KEY) continue;
    if (k === RES_KEY && isEmpty(v)) {
      flag = false;
      break;
    }
    if (!v) {
      flag = false;
      break;
    }
    if (typeof v === 'string' && isBlank(v)) {
      flag = false;
      break;
    }
  }
  return flag;
}
const DEFAULT_COLUMN_KEYS = [RES_KEY, `${RES_KEY}_count`, 'op'];
interface ReplyVariablesProps {
  options: KeyOption[];
  value?: ReplyVariable[];
  onChange?: (value: ReplyVariable[]) => void;
}
const ReplyVariables: React.FC<ReplyVariablesProps> = ({
  options: keyOptions,
  value,
  onChange,
}) => {
  const t = useLocale(i18n);

  const keysCacheRef = useRef<string[]>([]);

  const [visible, setVisible] = useState(false);

  const [data, setData] = useState<DataType[]>([]);

  const onDelete = useCallback((id: string) => {
    setData((d) => ObjectArrayHelper.del(d, (o) => o[ID_KEY] === id));
  }, []);
  const onEdit = useCallback((value: DataType) => {
    setData((d) =>
      ObjectArrayHelper.update(d, value, (o) => o[ID_KEY] === value[ID_KEY])
    );
  }, []);
  const defaultColumns = useMemo<ColumnProps[]>(
    () => [
      {
        title: t['table.reply'],
        key: DEFAULT_COLUMN_KEYS[0],
        dataIndex: RES_KEY,
        ellipsis: true,
        render: (responses: ReplyVariable['responses']) =>
          responses[0]
            ? responses[0].type === 'attachment'
              ? decodeAttachmentText(responses[0].content.text).name
              : responses[0].content.text
            : '-',
      },
      {
        title: t['table.reply.count'],
        key: DEFAULT_COLUMN_KEYS[1],
        dataIndex: RES_KEY,
        width: 160,
        align: 'center',
        render: (responses: ReplyVariable['responses']) =>
          responses?.length ?? 0,
      },
      {
        title: t['table.operation'],
        key: DEFAULT_COLUMN_KEYS[2],
        align: 'center',
        width: 100,
        render: (_, row) => (
          <Space>
            <EditReplyVariable
              options={keyOptions}
              initialValue={row}
              onSuccess={onEdit}
            />
            <Popconfirm
              focusLock
              title={t['table.operation.delete.title']}
              onOk={() => onDelete(row[ID_KEY])}
              position="tr"
            >
              <Button
                size="mini"
                type="outline"
                status="danger"
                icon={<IconDelete />}
              />
            </Popconfirm>
          </Space>
        ),
      },
    ],
    [keyOptions, onDelete, onEdit, t]
  );
  const [columns, setColumns] = useState<ColumnProps[]>(defaultColumns);

  useEffect(() => {
    if (visible) {
      setData(
        (value || []).map((v) => ({
          [ID_KEY]: v.id,
          ...v.entities,
          [RES_KEY]: v.responses,
        }))
      );
      const keys = value?.[0]
        ? Object.keys(value[0].entities).map((k) => k)
        : [];
      const optionMap = keyBy(keyOptions, 'value');
      setColumns([
        ...keys.map((k) => ({
          title: optionMap[k]?.label || '-',
          dataIndex: k,
        })),
        ...defaultColumns,
      ]);
      keysCacheRef.current = keys;
    }
  }, [visible, value, defaultColumns, keyOptions]);

  const onVariableChange = useCallback(
    (_, options: any[]) => {
      const selections: ColumnProps[] = options
        .map(
          (o) =>
            ({ title: o.display || o.name, dataIndex: o.id } as ColumnProps)
        )
        .map((p) => ({
          ...p,
          render: (value) =>
            value || (
              <span className="text-white rounded px-1 pb-1 leading-none font-medium bg-red-400">
                {t['required']}
              </span>
            ),
        }));
      const changeKes = selections.map((s) => s.dataIndex);
      // condition of optional type
      if (
        changeKes.length < keysCacheRef.current.length &&
        !isEqual(keysCacheRef.current, changeKes)
      ) {
        // delete condition
        const deleteKeys = xor(changeKes, keysCacheRef.current);
        const deleteColumns = keyOptions.filter((m) =>
          deleteKeys.includes(m.value)
        );
        function doAction() {
          setData((d) =>
            d.map((o) => {
              const clone = cloneDeep(o);
              deleteKeys.forEach(
                (k) => Object.hasOwn(clone, k) && delete clone[k]
              );
              return clone;
            })
          );
          setColumns([...selections, ...defaultColumns]);
        }
        if (data.some((d) => deleteKeys.some((c) => d[c]))) {
          Modal.confirm({
            title:
              deleteColumns.length > 1
                ? t['table.operation.delete.variables']
                : `${t['table.operation.delete.variable.prefix']}[ ${deleteColumns[0].label} ]${t['table.operation.delete.variable.subfix']}`,
            content: (
              <div className="flex justify-center">
                {deleteColumns.length > 1
                  ? t['table.operation.delete.variables.value']
                  : t['table.operation.delete.variable.value']}
              </div>
            ),
            style: { width: '520px' },
            onOk: () => doAction(),
          });
        } else {
          doAction();
        }
        return;
      }
      setData((d) =>
        d.map((o) => {
          const clone = cloneDeep(o);
          changeKes.forEach((k) => !Object.hasOwn(clone, k) && (clone[k] = ''));
          return clone;
        })
      );
      setColumns([...selections, ...defaultColumns]);
    },
    [defaultColumns, data, keyOptions, t]
  );

  const onAdd = useCallback(() => {
    const keys = columns
      .filter((c) => !defaultColumns.includes(c))
      .map((c) => c.dataIndex);
    const value = keys.reduce((p, c) => ({ ...p, [c]: '' }), {
      [ID_KEY]: nanoid(),
      [RES_KEY]: [],
    });
    setData((d) => [...d, value]);
  }, [columns, defaultColumns]);

  const onCancel = useCallback(() => {
    setVisible(false);
    setData([]);
    setColumns(defaultColumns);
  }, [defaultColumns]);

  const keys = useMemo(() => {
    const ks = columns
      .filter((c) => !defaultColumns.includes(c))
      .map((c) => c.dataIndex);
    keysCacheRef.current = ks;
    return ks;
  }, [columns, defaultColumns]);

  const onOk = () => {
    if (data.some((v) => !isValidateData(v))) {
      Message.error(t['message.error']);
      return;
    }
    const target = data.map(
      (d) =>
        ({
          id: d[ID_KEY],
          entities: keys.reduce((p, c) => ({ ...p, [c]: d[c] }), {}),
          responses: d[RES_KEY] || [],
        } as ReplyVariable)
    );
    onChange?.(target);
    setVisible(false);
  };

  const selectionKeyOptions = useMemo(() => {
    return columns
      .filter((c) => !DEFAULT_COLUMN_KEYS.includes(c.key + ''))
      .map(({ title, dataIndex }) => ({
        label: title as string,
        value: dataIndex,
      }));
  }, [columns]);

  return (
    <>
      <Button type="primary" long onClick={() => setVisible((v) => !v)}>
        {t['trigger.title']}
      </Button>
      <Modal
        style={{ width: '55%' }}
        title={t['trigger.title']}
        visible={visible}
        onCancel={onCancel}
        onOk={onOk}
        unmountOnExit
        closable={false}
      >
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2">
            <span>{t['trigger.variable.title']}</span>
            <SlotSelecter
              value={keys}
              mode="multiple"
              placeholder={t['trigger.variable.placehodler']}
              className="w-full"
              onChange={onVariableChange}
            />
          </div>
          {columns.length < 4 && (
            <Empty className="border border-[var(--color-neutral-3)] rounded" />
          )}
          {columns.length > 3 && (
            <Table
              rowKey={ID_KEY}
              columns={columns}
              data={data}
              pagination={false}
            />
          )}
          {columns.length > 3 && (
            <AddReplyVariable
              options={selectionKeyOptions}
              onSuccess={(value) => setData((d) => [...d, value])}
              trigger={
                <Button
                  type="outline"
                  status="success"
                  long
                  icon={<IconPlus />}
                  onClick={onAdd}
                >
                  {t['trigger.variable.add']}
                </Button>
              }
            />
          )}
        </div>
      </Modal>
    </>
  );
};

export default ReplyVariables;
