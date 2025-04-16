import { UserText } from '@/graph-next/components/IconText';
import useLocale from '@/utils/useLocale';
import { Tag } from '@arco-design/web-react';
import { IconStar } from '@arco-design/web-react/icon';
import { VscSymbolField } from 'react-icons/vsc';
import { isEmpty } from 'lodash';
import React from 'react';
import i18n from '../locale';

const UserView = (props) => {
  const {
    data: { examples, mappingsEnable, mappings },
    linkedFrom,
  } = props;
  const t = useLocale(i18n);
  if (examples?.length > 0) {
    const example = examples[0];
    if (!isEmpty(example?.text)) {
      if (mappingsEnable) {
        return (
          <>
            {linkedFrom && (
              <Tag size="small" color="orange" icon={<IconStar />}>
                {linkedFrom.name}
              </Tag>
            )}
            <UserText>{example.text}</UserText>
            {mappings && (
              <div className="flex gap-2">
                {mappings?.map(({ id, slotDisplay, slotName }) => (
                  <Tag
                    key={id}
                    size="small"
                    color="arcoblue"
                    icon={<VscSymbolField />}
                  >
                    {slotDisplay || slotName}
                  </Tag>
                ))}
              </div>
            )}
          </>
        );
      }
      return (
        <>
          {linkedFrom && (
            <Tag size="small" color="orange" icon={<IconStar />}>
              {linkedFrom.name}
            </Tag>
          )}
          <UserText>{example.text}</UserText>
        </>
      );
    }
  }
  return <UserText>{t['flow.node.user.unknown']}</UserText>;
};

export default UserView;
