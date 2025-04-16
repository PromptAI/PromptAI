import i18n from './locale';
import { Image, Button, Typography, Spin, Tag } from '@arco-design/web-react';
import { IconFilePdf } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import moment from 'moment';
import React, { Fragment, useMemo } from 'react';
import Empty from '../Empty';
import './message.css';
import useCacheMessage from './cache';
import { linkMap } from '../SourceColumn';

const imageTypes = ['png', 'avg', 'jpg', 'jpeg', 'gif', 'webp'];
const AttachmentView = ({ name, href, type }) => {
  return (
    <>
      {imageTypes.includes(type) && (
        <Image src={href} width="220px" style={{ background: 'white' }} />
      )}
      <div className="attachmentLink">
        <IconFilePdf />
        <a href={href} download={name}>
          {name}
        </a>
      </div>
    </>
  );
};

export default function Message({ width, height, background, msgInfo }) {
  const t = useMemo(
    () => i18n[msgInfo.locale === 'en' ? 'en-US' : 'zh-CN'],
    [msgInfo.locale]
  );
  const [loading, datasource] = useCacheMessage(msgInfo.id);

  return (
    <Spin loading={loading} className="w-full">
      <section style={{ width, height, background }} className="x-robot">
        {datasource ? (
          <div className="x-robot-m">
            {datasource.map(
              ({ data, id: i, similarQuestions = [], links = [] }) => (
                <Fragment key={i}>
                  {data.map(
                    ({
                      id,
                      content,
                      position,
                      avatar,
                      time,
                      evaluation,
                      custom,
                    }) => (
                      <Fragment key={id + i}>
                        {position === 'right' && (
                          <div className="x-robot-u">
                            <div className="x-robot-uc">
                              <span>
                                {time
                                  ? moment(Number(time)).format(
                                      'YYYY-MM-DD HH:mm:ss'
                                    )
                                  : null}
                              </span>
                              <div className="x-robot-ucc">
                                <div style={{ maxWidth: '100%' }}>
                                  {content?.map(({ text, attachment }, index) =>
                                    attachment ? (
                                      <AttachmentView
                                        key={index}
                                        {...attachment}
                                      />
                                    ) : (
                                      <span key={index}>{text}</span>
                                    )
                                  ) || '-'}
                                </div>
                              </div>
                            </div>
                            <img src={avatar} alt="U" className="x-robot-a" />
                          </div>
                        )}
                        {position === 'left' && (
                          <div className="x-robot-b">
                            <img src={avatar} alt="B" className="x-robot-a" />
                            <div className="x-robot-bc">
                              <span style={{ paddingLeft: 10 }}>
                                {time
                                  ? moment(Number(time)).format(
                                      'YYYY-MM-DD HH:mm:ss'
                                    )
                                  : null}
                              </span>
                              <div className="x-robot-bcc">
                                {content?.map(
                                  (
                                    { text, image, buttons, attachment },
                                    index
                                  ) => (
                                    <>
                                      <div
                                        key={index + text}
                                        className="x-robot-bcc-m"
                                        style={{ position: 'relative' }}
                                      >
                                        {!isEmpty(text) && (
                                          <span className="x-robot-bcc-mt">
                                            {text}
                                          </span>
                                        )}
                                        {image && (
                                          <div className="x-robot-bcc-mi">
                                            {image
                                              ?.split(',')
                                              .filter((i) => i !== '')
                                              .map((i, ind) => (
                                                <Image
                                                  width="100%"
                                                  height="100%"
                                                  key={i + ind}
                                                  src={i}
                                                  alt="bg"
                                                  className="x-robot-bcc-mi-img"
                                                />
                                              ))}
                                          </div>
                                        )}
                                        {attachment && (
                                          <AttachmentView {...attachment} />
                                        )}
                                        {buttons && (
                                          <div className="x-robot-bcc-mb">
                                            <div className="x-robot-bcc-mb-primary">
                                              {buttons.map(
                                                ({ payload, title }, inx) => (
                                                  <div
                                                    key={
                                                      payload + inx + 'primary'
                                                    }
                                                  >
                                                    <Button
                                                      type="outline"
                                                      style={{ maxWidth: 140 }}
                                                    >
                                                      <Typography.Text
                                                        ellipsis={{
                                                          showTooltip: true,
                                                        }}
                                                        type="primary"
                                                        style={{ margin: 0 }}
                                                      >
                                                        {title}
                                                      </Typography.Text>
                                                    </Button>
                                                  </div>
                                                )
                                              )}
                                            </div>
                                          </div>
                                        )}
                                      </div>
                                    </>
                                  )
                                )}
                              </div>
                              {custom?.fallback && (
                                <div
                                  style={{
                                    height: 'max-content',
                                    marginLeft: '0.8rem',
                                  }}
                                >
                                  <Tag
                                    size="small"
                                    color={linkMap['fallback']?.color}
                                  >
                                    {t[linkMap['fallback'].i18n] || '-'}(
                                    {t[linkMap[custom.fallback]?.i18n] || '-'})
                                  </Tag>
                                </div>
                              )}
                              {evaluation && (
                                <span style={{ paddingLeft: 10, fontSize: 12 }}>
                                  {t['message.evalute']}:{' '}
                                  {t[`message.helpful.${evaluation.helpful}`]}
                                </span>
                              )}
                              {similarQuestions &&
                                similarQuestions.length > 0 && (
                                  <SimilarQuestions
                                    items={similarQuestions}
                                    t={t}
                                  />
                                )}
                              {links && links.length > 0 && (
                                <Links items={links} t={t} />
                              )}
                            </div>
                          </div>
                        )}
                      </Fragment>
                    )
                  )}
                </Fragment>
              )
            )}
          </div>
        ) : (
          <Empty />
        )}
      </section>
    </Spin>
  );
}

const SimilarQuestions = ({ items, t }) => {
  return (
    <div className="similarsContainer">
      <span>{t['message.similarQuestions']}:</span>
      <div className="similars">
        {items.map(({ query, id }, index) => (
          <span key={id} title={query}>
            {index + 1}.{query}
          </span>
        ))}
      </div>
    </div>
  );
};

const Links = ({ items, t }) => {
  return (
    <div className="similarsContainer">
      <span>{t['message.links']}:</span>
      <div className="similars">
        {items.map(({ url }) => (
          <a key={url} title={url} href={url} target="_blank" rel="noreferrer">
            {url}
          </a>
        ))}
      </div>
    </div>
  );
};
