import {
  Button,
  Input,
  List,
  Trigger,
  Typography,
} from '@arco-design/web-react';
import { isEmpty } from 'lodash';
import React, { useState } from 'react';
import styles from './styles.module.less';

const Popup = ({ items, name, placeholder }) => {
  const [dataSource, setDataSource] = useState(items);
  const onSearch = (evt) => {
    const val = evt.target.value;
    if (isEmpty(val)) {
      setDataSource(items);
      return;
    }
    setDataSource(
      items.filter((i: string) => i.toLowerCase().includes(val.toLowerCase()))
    );
  };
  return (
    <div className={styles.container}>
      <List
        size="small"
        className={styles.list}
        virtualListProps={{ height: '50vh' }}
        header={
          <div className={styles.headerTitle}>
            <span>{name}</span>
            <Input.Search
              className={styles.headerSearch}
              onPressEnter={onSearch}
              placeholder={placeholder}
            />
          </div>
        }
        dataSource={dataSource}
        render={(item, index) => (
          <List.Item key={index}>
            <Typography.Text
              style={{ margin: 0 }}
              ellipsis={{ showTooltip: true }}
            >
              {item}
            </Typography.Text>
          </List.Item>
        )}
      />
    </div>
  );
};
const arrowProps = {
  style: {
    background: 'var(--color-fill-4)',
    boxShadow:
      '0 4px 6px -1px rgb(0 0 0 / 10%), 0px -2px 4px -1px rgb(0 0 0 / 6%)',
  },
};
const PreviewCorpus = ({ items, title, name, placeholder }) => {
  return (
    <Trigger
      popup={() => (
        <Popup items={items} name={name} placeholder={placeholder} />
      )}
      trigger="click"
      position="left"
      showArrow
      arrowProps={arrowProps}
    >
      <Button type="outline" status="success" size="mini">
        {title}
      </Button>
    </Trigger>
  );
};

export default PreviewCorpus;
