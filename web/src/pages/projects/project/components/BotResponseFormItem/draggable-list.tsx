import React, { useEffect, useRef } from 'react';

const searchElementByClassName = (anchor, current, root) => {
  return searchElement(
    (node) => node.classList.contains(anchor),
    current,
    root
  );
};

const searchElementByDraggableAttr = (current, root) => {
  return searchElement((node) => node.hasAttribute('draggable'), current, root);
};

const searchElement = (get, current, root) => {
  if (!root) return null;
  let p = current;
  do {
    if (get(p)) {
      return p;
    }
  } while (p !== root && (p = p.parentElement));
  return null;
};

export default ({
  allowedAnchor,
  onChange,
  children,
}: {
  allowedAnchor: string;
  onChange: (from: number, to: number) => void;
  children: React.ReactNode[];
}) => {
  const ref = useRef<HTMLDivElement>();
  const cache = useRef<{ onChange: (from, to) => void }>({ onChange });
  cache.current.onChange = onChange;
  useEffect(() => {
    let el: HTMLElement;
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    let allow;
    let fromElement: HTMLElement;
    let toElement: HTMLElement;
    let startFromIndex: number;
    let toIndex: number;

    const handleMouseDown = (e) => {
      allow = searchElementByClassName(allowedAnchor, e.target, el);
    };
    const handleMouseUp = () => {
      allow = false;
    };
    const handleDragStart = (e: DragEvent) => {
      if (allow) {
        setTimeout(() => {
          fromElement = e.target as HTMLElement;
          fromElement.classList.add('dragging');
        });
        startFromIndex = Array.from(ref.current.children).indexOf(
          e.target as HTMLElement
        );
        e.dataTransfer.effectAllowed = 'move';
      } else {
        e.preventDefault();
      }
    };
    const handleDragEnd = (e) => {
      e.preventDefault();
      Array.from(el.querySelectorAll('.dragging')).forEach((item) =>
        item.classList.remove('dragging')
      );
      cache.current?.onChange(startFromIndex, toIndex);
    };
    const handleDragOver = (e) => e.preventDefault();
    const handleDragEnter = (e) => {
      e.preventDefault();
      const newToElement = searchElementByDraggableAttr(e.target, el);
      if (toElement !== newToElement) {
        toElement = newToElement;
        if (toElement) {
          const list = Array.from(el.children);
          const fromIndex = list.indexOf(fromElement);
          toIndex = list.indexOf(toElement);
          if (fromIndex < 0) {
            return;
          }
          if (fromIndex < toIndex) {
            el.insertBefore(fromElement, toElement.nextElementSibling);
          } else {
            el.insertBefore(fromElement, toElement);
          }
        }
      }
    };

    if ((el = ref.current)) {
      document.addEventListener('mouseup', handleMouseUp);
      el.addEventListener('mousedown', handleMouseDown);
      el.addEventListener('dragstart', handleDragStart);
      el.addEventListener('dragover', handleDragOver);
      el.addEventListener('dragenter', handleDragEnter);
      el.addEventListener('dragend', handleDragEnd);
    }

    return () => {
      el.removeEventListener('mousedown', handleMouseDown);
      document.removeEventListener('mouseup', handleMouseUp);
      el.removeEventListener('dragstart', handleDragStart);
      el.removeEventListener('dragover', handleDragOver);
      el.removeEventListener('dragenter', handleDragEnter);
      el.removeEventListener('dragend', handleDragEnd);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <div ref={ref}>
      {React.Children.map(children.filter(React.isValidElement), (child: any) =>
        React.cloneElement(child, { ...child.props, draggable: true })
      )}
    </div>
  );
};
