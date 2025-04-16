import React, {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { createPortal, unstable_batchedUpdates } from 'react-dom';
import type { MarkAnnotationProps, MarkAnnotationValue, Mark } from './types';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import './index.css';
import {
  Button,
  Divider,
  Space,
  Input,
  Tag,
  Typography,
} from '@arco-design/web-react';
import { IconHighlight, IconCodeBlock } from '@arco-design/web-react/icon';
import { VscSymbolField } from 'react-icons/vsc';
import useTextSelection from '@/hooks/useTextSelection';
import { useQuery, useMutation } from 'react-query';
import { getList, update, create } from '@/api/synonyms';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { escape } from 'html-escaper';

export { MarkAnnotationProps };

function getMarksByElement(rootElement: HTMLDivElement) {
  const els = Array.from(rootElement.childNodes);
  let temp = 0;
  let text = '';
  const marks: Mark[] = [];
  els.forEach((el) => {
    if (el.nodeName === 'MARK') {
      const dataset = (el as any).dataset;
      const entityId = dataset.entityId;
      const targetId =
        dataset.targetId && dataset.targetId !== 'undefined'
          ? dataset.targetId
          : void 0;
      marks.push({
        start: temp,
        end: temp + el.textContent.length,
        targetId,
        entityId,
      });
    }
    text += el.textContent;
    temp += el.textContent.length;
  });
  return { text, marks };
}

class Portal extends React.Component {
  node: HTMLElement;
  constructor(props) {
    super(props);

    const doc = window.document;
    this.node = doc.createElement('div');
    doc.body.appendChild(this.node);
  }

  render() {
    return <>{createPortal(this.props.children, this.node)}</>;
  }

  componentWillUnmount() {
    window.document.body.removeChild(this.node);
  }
}

type AnnotationElement = {
  type: 'nomarl' | 'mark';
  content: string;
  targetId?: string;
  entityId?: string;
  name?: string;
};
type AnnotationSelection = {
  text: string;
  start: number;
  end: number;
};
const defaultPosition = { x: -9999, y: -9999 };

const CONNECT_STR = '_CONNECT_SLOT_ENTITY_';

const getConnectedId = (slot, entity) => `${slot}${CONNECT_STR}${entity}`;

const defaultValue = { text: '', marks: [], autoFocus: true };
const defaultArray = [];

const MarkAnnotation = ({
  disabled,
  placeholder,
  value = defaultValue,
  slots = defaultArray,
  entities = defaultArray,
  onChange,
}: MarkAnnotationProps) => {
  const { projectId } = useUrlParams();
  const {
    data: synonyms = [],
    isLoading: loading,
    refetch,
  } = useQuery(['allsynonyms', projectId], () => getList({ projectId }), {
    refetchOnWindowFocus: false,
  });
  const { mutateAsync } = useMutation<any, any, any>(
    'mutate',
    (data) => {
      if (data.id) {
        return update({ projectId, ...data });
      } else {
        return create({ projectId, ...data });
      }
    },
    {}
  );
  const t = useLocale(i18n);
  const [currentValue, setCurrentValue] = useState(
    value || { text: '', marks: [] }
  );
  const textRef = useRef(value?.text);
  const divRef = useRef<HTMLDivElement>();
  const ref = useRef<HTMLDivElement>();
  const { text } = useTextSelection(ref);
  const [startMark, setStartMark] = useState(false);
  const [popupVisible, setPopupVisible] = useState(false);
  const [position, setPosition] = useState(defaultPosition);
  const portalRef = useRef<HTMLDivElement>();
  const [seed, setSeed] = useState(0);
  const cacheRef = useRef<{
    currentValue: MarkAnnotationValue;
    blurTimer: any;
    reselectTimer: any;
  }>({ currentValue, blurTimer: null, reselectTimer: null });
  cacheRef.current.currentValue = currentValue;

  useEffect(() => {
    const v = {
      ...value,
      marks: (value?.marks || [])
        .map((o) => {
          if (o.targetId) {
            // Handles the case where the field node has been deleted
            if (!slots.some((s) => s.slotId === o.targetId)) {
              return null;
            }
            return { ...o, targetId: o.targetId || undefined };
          } else {
            if (o.entityId) {
              return {
                ...o,
                entityId: o.entityId,
                name: entities.find((e) => e.slotId === o.entityId)
                  ?.slotDisplay,
              };
            }
            return {
              ...o,
              entityId: entities[0].slotId,
              name: entities[0].slotDisplay,
            };
          }
        })
        .filter(Boolean),
    };
    setCurrentValue(v);
    cacheRef.current.currentValue = v;
  }, [value, entities, slots]);

  useEffect(() => {
    if (text !== '' && startMark) {
      const { anchorOffset, focusOffset } = window.getSelection();
      const obj: AnnotationSelection = {
        text,
        start: Math.min(anchorOffset, focusOffset),
        end: Math.max(anchorOffset, focusOffset),
      };
      if (obj.start !== obj.end) {
        setPopupVisible(true);
      }
    } else {
      setPopupVisible(false);
    }
  }, [text, startMark]);

  useEffect(() => {
    if (value.autoFocus) {
      setStartMark(true);
      setTimeout(() => {
        divRef.current?.focus?.();
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let temp = 0;
    const { text = '', marks = [] } = cacheRef.current.currentValue || {};
    const arr = marks
      .filter((m) => m.start < m.end)
      .sort((s, t) => s.start - t.start)
      .map(({ start, end, targetId, entityId, name }) => {
        const annotations: AnnotationElement[] = [
          {
            type: 'nomarl',
            content: escape(text.slice(temp, start)),
          },
          {
            type: 'mark',
            content: escape(text.slice(start, end)),
            targetId: targetId ? targetId : undefined,
            entityId,
            name,
          },
        ];
        temp = end;
        return annotations;
      })
      .reduce((p, c) => [...p, ...c], []);
    divRef.current.innerHTML = [
      ...arr,
      {
        type: 'nomarl',
        content: escape(text?.slice(temp)) || '',
      } as AnnotationElement,
    ]
      .map((o) =>
        o.type === 'nomarl'
          ? o.content
          : `<mark data-target-id="${o.targetId}" data-entity-id="${
              o.entityId
            }" class="mark-annotation-element" data-name="${o.name || ''}">${
              o.content
            }</mark>`
      )
      .join('');
  }, [seed, value]);

  const handleMark = () => {
    if (!disabled) {
      const v = getMarksByElement(divRef.current);
      onChange?.(v);
    }
    setStartMark(false);
  };
  const handleCancel = () => {
    onChange({ ...value });
    setCurrentValue(value);
    setSeed((s) => s + 1);
    setStartMark(false);
  };
  const handleStartMark = () => {
    setStartMark(true);
    setPosition(defaultPosition);
    setTimeout(() => {
      divRef.current?.focus();
    }, 0);
  };
  const handleTextChange = (evt) => {
    setPopupVisible(false);
    setRange(null);
    setPosition(defaultPosition);
    textRef.current = evt.nativeEvent.target.textContent;
  };
  const handleBlur = () => {
    cacheRef.current.blurTimer = setTimeout(() => {
      setPosition(defaultPosition);
    }, 200);
  };
  const dataTip = useMemo(() => {
    if (startMark) {
      if (popupVisible) {
        return {
          tip: t['markAnnotation.boxSure'],
          className:
            'mark-annotation-container mark-annotation-container-success',
        };
      }
      return {
        tip: t['markAnnotation.boxRange'],
        className: 'mark-annotation-container mark-annotation-container-normal',
      };
    }
    return { tip: '', className: 'mark-annotation-container' };
  }, [popupVisible, startMark, t]);

  const [range, setRange] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState<string | undefined>();
  const [rangeLinkedMapping, setRangeLinkedMapping] = useState(null);
  const [synonymText, setSynonymText] = useState('');

  const getRangeLinkedMapping = (range: Range) => {
    const el: HTMLElement = range.startContainer.childNodes[
      range.startOffset
    ] as HTMLElement;
    if (el) {
      return [
        el.dataset.targetId === 'undefined' || el.dataset.targetId === 'null'
          ? undefined
          : el.dataset.targetId,
        el.dataset.entityId,
      ];
    }
    return [];
  };

  const updateSelection = (range) => {
    setTimeout(() => {
      const bounds = range.getBoundingClientRect();
      const divBounds = portalRef.current.getBoundingClientRect();
      const x = bounds.left + bounds.width / 2 - divBounds.width / 2;
      setRange(range);
      // x and y should be in viewport
      setPosition({
        x:
          x + divBounds.width > document.documentElement.offsetWidth - 150
            ? document.documentElement.offsetWidth - divBounds.width - 150
            : x,
        y:
          bounds.top + bounds.height + 8 + divBounds.height >
          document.documentElement.offsetHeight
            ? bounds.top - divBounds.height - 8
            : bounds.top + bounds.height + 8,
      });
    });
  };

  const insertTextAtRange = useCallback((text) => {
    const sel = window.getSelection();
    if (sel.getRangeAt && sel.rangeCount) {
      const r = sel.getRangeAt(0);
      r.deleteContents();
      r.insertNode(document.createTextNode(text));
      r.setStart(r.endContainer, r.endOffset);
      reselect(r);
    }
  }, []);

  const excludeHandler = (e) => {
    if (e.key === 'Enter') {
      e.stopPropagation();
      e.preventDefault();
      return false;
    }
    // This is to fix the following bug:
    // ----
    // When I now remove the marked tag with Backspace
    // and start retyping some new characters
    // after deleted the last one inside the content editable
    // there is a <font style="..."><span style="...">new text created.
    if (e.key === 'Backspace') {
      const selection = window.getSelection();
      const r = selection.getRangeAt(0);
      if (!r.collapsed) {
        const nr = new Range();
        nr.setStart(r.startContainer, 0);
        nr.setEnd(r.endContainer, 0);
        reselect(nr);
        reselect(r);
      }
    }
  };

  const handleEditorMouseDown = () => {
    if (startMark) {
      setPosition(defaultPosition);
      preventReselect();
      const update = (isMouseUp = false) =>
        unstable_batchedUpdates(() =>
          requestAnimationFrame(() => {
            const selection = window.getSelection();
            const r = selection.getRangeAt(0);
            setSynonymText('');
            setSelectedSlot(undefined);
            setRangeLinkedMapping(null);
            // range cross node
            if (r.startContainer !== r.endContainer && !selection.isCollapsed) {
              updateSelection(r);
            } else {
              const nodes = Array.from(divRef.current.childNodes);
              const index = nodes.findIndex(
                (n) => n === r.commonAncestorContainer
              );
              // click
              if (index === -1 && isMouseUp === true && selection.isCollapsed) {
                setTimeout(() => {
                  const range = new Range();
                  let i = 0;
                  let p = r.commonAncestorContainer;
                  if (p !== divRef.current && divRef.current.contains(p)) {
                    while (p !== divRef.current && p) {
                      p = p.parentNode;
                    }
                    if (!p) return;
                    i = nodes.findIndex(
                      (n) => n === r.commonAncestorContainer.parentElement
                    );
                  } else if (p === divRef.current) {
                    i = r.startOffset;
                  }

                  try {
                    range.setStart(p, i);
                    range.setEnd(p, i + 1);
                    reselect(range);
                    updateSelection(range);
                    const [targetId = undefined, entityId] =
                      getRangeLinkedMapping(range);
                    setSelectedSlot(targetId);
                    setRangeLinkedMapping(getConnectedId(targetId, entityId));
                  } catch {}
                });
              } else {
                if (!selection.isCollapsed) {
                  updateSelection(r);
                }
              }
            }
          })
        );
      const handle = () => update();
      const handleup = () => {
        update(true);
        document.removeEventListener('mousemove', handle);
        document.removeEventListener('mouseup', handleup);
      };
      document.addEventListener('mousemove', handle);
      document.addEventListener('mouseup', handleup);
    }
  };

  const filteredSynonyms = useMemo(() => {
    if (range) {
      const text = range.toString();
      const get = (id, text, cur) => ({
        key: `${id}-${text}`,
        id,
        text,
        data: cur,
      });
      return synonyms.reduce((acc, cur) => {
        if (cur.data.original === text) {
          return [...acc, ...cur.data.synonyms.map((o) => get(cur.id, o, cur))];
        } else if (cur.data.synonyms.includes(text)) {
          return [
            ...acc,
            ...cur.data.synonyms
              .filter((o) => o !== text)
              .map((o) => get(cur.id, o, cur)),
            get(cur.id, cur.data.original, cur),
          ];
        }
        return acc;
      }, []);
    }
    return [];
  }, [synonyms, range]);

  const preventBlur = () =>
    setTimeout(() => {
      window.clearTimeout(cacheRef.current.blurTimer);
      cacheRef.current.blurTimer = null;
    });

  const preventReselect = () =>
    setTimeout(() => {
      window.clearTimeout(cacheRef.current.reselectTimer);
      cacheRef.current.reselectTimer = null;
    });

  const reselect = (r) => {
    cacheRef.current.reselectTimer = setTimeout(() => {
      if (r) {
        const s = window.getSelection();
        s.removeAllRanges();
        s.addRange(r);
      }
    }, 100);
  };
  const unWrapMarkById: (
    params: { entityId?: string; targetId?: string },
    range?: Range
  ) => any = (params, range = null) => {
    const paramsArray = Object.entries(params).filter(
      ([, v]) => !!v && v !== 'undefined'
    );
    if (paramsArray.length) {
      const el = divRef.current.querySelector(
        paramsArray.reduce(
          (acc, [k, v]) =>
            acc +
            `[data-${k.replace(
              /([A-Z])/,
              (m) => `-${m.toLowerCase()}`
            )}="${v}"]`,
          ''
        )
      );
      if (!el) return;

      let startOffset;
      let endOffset;
      let inLeft = false;
      let inRight = false;
      if (range) {
        inLeft = range.startContainer.parentNode === el;
        inRight = range.endContainer.parentElement === el;
        if (inLeft) {
          startOffset = range.startOffset;
        }
        if (inRight) {
          endOffset = range.endOffset;
        }
      }
      const text = document.createTextNode(el.textContent);
      el.parentNode.insertBefore(text, el);
      el.parentNode.removeChild(el);
      if (inLeft) range.setStart(text, startOffset);
      if (inRight) range.setEnd(text, endOffset);
      return text;
    }
  };

  const wrapMark = (r, entityId, targetId) => {
    const mark = document.createElement('mark') as HTMLElement;
    mark.classList.add('mark-annotation-element');
    mark.contentEditable = 'true';
    mark.dataset.entityId = entityId;
    mark.dataset.targetId = targetId;
    mark.dataset.name =
      entities.find((e) => e.slotId === entityId)?.slotDisplay || '';
    try {
      r.surroundContents(mark);
      mark.innerHTML = escape(mark.textContent);
    } catch (e) {
      console.error(e);
    }
  };

  const getRealChild = (el) => {
    if (el === divRef.current) return el;
    let p = el;
    while (p.parentNode !== divRef.current && p) {
      p = p.parentNode;
    }
    return p;
  };

  const markText = (r, entityId, targetId) => {
    let nodes = Array.from(divRef.current.childNodes);
    // to sure entity
    const s = r.startContainer;
    const so = r.startOffset;
    const sc = r.startContainer.parentNode === divRef.current;
    const sr = getRealChild(s);
    const si = !sc ? nodes.findIndex((n) => n === sr) : -1;

    const e = r.endContainer;
    const eo = r.endOffset;
    const ec = r.endContainer.parentNode === divRef.current;
    const er = getRealChild(e);
    const ei = !ec ? nodes.findIndex((n) => n === er) : -1;

    unWrapMarkById({ entityId, targetId });

    if (!sc && si > -1) {
      unWrapMarkById(sr.dataset);
      nodes = Array.from(divRef.current.childNodes);
      r.setStart(nodes[si], so);
    }
    if (!ec && ei > -1) {
      unWrapMarkById(er.dataset);
      nodes = Array.from(divRef.current.childNodes);
      r.setEnd(nodes[ei], eo);
    }
    wrapMark(r, entityId, targetId);
  };

  useEffect(() => {
    const handle = (e) => {
      if (e.target === divRef.current || divRef.current.contains(e.target)) {
        const clipdata = e.clipboardData || window['clipboardData'];
        e.preventDefault();
        insertTextAtRange(clipdata.getData('text/plain'));
      }
    };
    const onMouseWheel = () => {
      setPosition(defaultPosition);
    };
    window.addEventListener('paste', handle);
    document.addEventListener('mousewheel', onMouseWheel);
    return () => {
      window.removeEventListener('paste', handle);
      document.removeEventListener('mousewheel', onMouseWheel);
    };
  }, [insertTextAtRange]);

  return (
    <>
      {startMark && (
        <Portal>
          <div
            ref={portalRef}
            className={'mark-menu'}
            style={{
              left: position.x,
              top: position.y,
              maxWidth: 400,
            }}
          >
            {slots.length > 0 && (
              <>
                {slots.map((o) => {
                  return (
                    <Tag
                      bordered
                      icon={<IconCodeBlock />}
                      key={o.slotId}
                      color={selectedSlot === o.slotId ? 'green' : 'gray'}
                      style={{ margin: '0 5px 5px 0' }}
                      onClick={() => {
                        preventBlur();
                        if (o.slotId === selectedSlot) {
                          if (rangeLinkedMapping) {
                            const [targetId, entityId] =
                              rangeLinkedMapping.split(CONNECT_STR);
                            unWrapMarkById({ entityId, targetId });
                            setPosition(defaultPosition);
                          } else {
                            setSelectedSlot(undefined);
                          }
                        } else if (rangeLinkedMapping) {
                          const [, entityId] =
                            rangeLinkedMapping.split(CONNECT_STR);
                          unWrapMarkById({ targetId: o.slotId }, range);
                          markText(range, entityId, o.slotId);
                          setPosition(defaultPosition);
                        } else {
                          reselect(range);
                          setSelectedSlot(o.slotId);
                          // set selectedSlot will be change the portal height
                          setTimeout(() => {
                            const bounds = range.getBoundingClientRect();
                            const divBounds =
                              portalRef.current.getBoundingClientRect();
                            if (
                              bounds.top +
                                bounds.height +
                                8 +
                                divBounds.height >
                              document.documentElement.offsetHeight
                            ) {
                              setPosition(({ x }) => ({
                                x,
                                y: bounds.top - divBounds.height - 8,
                              }));
                            }
                          }, 0);
                        }
                      }}
                    >
                      {o.slotDisplay}
                    </Tag>
                  );
                })}
                {selectedSlot && <Divider style={{ margin: '0 0 5px 0' }} />}
              </>
            )}
            {((slots.length > 0 && selectedSlot) || slots.length === 0) &&
              entities.map((o) => {
                const connectedId = getConnectedId(selectedSlot, o.slotId);
                return (
                  <Tag
                    bordered
                    color={
                      rangeLinkedMapping === connectedId ? 'green' : 'gray'
                    }
                    icon={
                      <VscSymbolField
                        style={{ display: 'flex', alignItems: 'center' }}
                      />
                    }
                    key={o.id}
                    style={{ margin: '0 5px 5px 0' }}
                    onClick={() => {
                      preventBlur();
                      unstable_batchedUpdates(() => {
                        const targetId = selectedSlot
                          ? selectedSlot
                          : 'undefined';
                        const entityId = o.slotId;
                        if (rangeLinkedMapping === connectedId) {
                          unWrapMarkById({ entityId, targetId });
                        } else {
                          unWrapMarkById({ targetId }, range);
                          markText(range, entityId, targetId);
                        }
                        setPosition(defaultPosition);
                      });
                    }}
                  >
                    {o.slotDisplay}
                  </Tag>
                );
              })}
            <Divider style={{ margin: '0 0 5px 0' }} />
            <Typography.Text style={{ fontSize: 12 }}>
              {t['markAnnotation.synonyms']}
              {loading
                ? t['markAnnotation.synonymsLoading']
                : filteredSynonyms.length
                ? filteredSynonyms.map((o) => o.text).join(', ')
                : t['markAnnotation.synonymsNone']}
            </Typography.Text>
            <Divider style={{ margin: '5px 0' }} />
            <Input
              value={synonymText}
              placeholder={t['markAnnotation.addSynonyms']}
              onChange={(value) => setSynonymText(value)}
              onFocus={() => preventBlur()}
              onBlur={() => reselect(range)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  const id = filteredSynonyms.length
                    ? filteredSynonyms[0].id
                    : void 0;
                  const data = id
                    ? filteredSynonyms[0].data
                    : {
                        data: {
                          synonyms: [],
                          enabled: true,
                          original: '',
                          description: '',
                          display: '',
                          labels: [],
                        },
                      };
                  const newSynonym = synonymText;
                  if (!id) {
                    data.data.original = range.toString();
                    data.data.synonyms = [newSynonym];
                  } else {
                    data.data.synonyms = Array.from(
                      new Set([...data.data.synonyms, newSynonym])
                    );
                  }
                  mutateAsync({
                    id,
                    ...data,
                  })
                    .then(() => refetch())
                    .then(() => {
                      reselect(range);
                      setSynonymText('');
                    });
                }
              }}
            />
          </div>
        </Portal>
      )}
      <div className={dataTip.className} data-tip={dataTip.tip} ref={ref}>
        <div
          ref={divRef}
          className="mark-annotation"
          contentEditable={!disabled && startMark}
          suppressContentEditableWarning
          placeholder={placeholder}
          onInput={handleTextChange}
          onMouseDown={handleEditorMouseDown}
          onBlur={handleBlur}
          onKeyDown={excludeHandler}
        ></div>
        {!disabled && (
          <>
            <Divider style={{ margin: 0 }} />
            <div className="mark-annotation-trigger">
              {startMark ? (
                <div className="mark-annotation-confirm">
                  <Space>
                    <Button size="mini" type="primary" onClick={handleMark}>
                      {t['markAnnotation.sure']}
                    </Button>
                    <Button size="mini" type="secondary" onClick={handleCancel}>
                      {t['markAnnotation.cancel']}
                    </Button>
                  </Space>
                </div>
              ) : (
                <div className="mark-annotation-start-mark">
                  <Button
                    size="mini"
                    type="outline"
                    onClick={handleStartMark}
                    icon={<IconHighlight />}
                  >
                    {t['markAnnotation.change']}
                  </Button>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </>
  );
};

export default memo(MarkAnnotation);
