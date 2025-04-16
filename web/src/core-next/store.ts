import { debounce, DebouncedFunc } from 'lodash';

type Position = {
  x: number;
  y: number;
};

type Events = 'zoom' | 'resize';
type Listener = {
  name: Events;
  callback: (...args: any | undefined) => void;
};
type Config = {
  contrlMoving?: boolean;
  unMovingClassNames?: string[];
};
const MIN_DISTANCE = 5;
const MAX_ZOOM = 1.5;
const MIN_ZOOM = 0.5;
const NORMAL_ZOOM = 1;
const SPACE_KEY = ' ';
const SCALE_TATE = 0.1;
class MoveAndZoom<T extends HTMLElement> {
  config: Config;

  target: T | null;
  action: T | undefined;

  spaceKeyDowned: boolean;
  start: Position | null;
  moving: boolean;
  offset: Position;
  cacheOffset: Position;
  // wheelOffset:

  zoom: number;
  listeners: Listener[] = [];
  private os: 'windows' | 'macos' | 'linux';
  constructor(config: Config) {
    this.config = config;
    this.os = this.getOs();
  }
  private getOs() {
    const agent = navigator.userAgent.toLowerCase();
    const isMac = /macintosh|mac os x/i.test(navigator.userAgent);
    if (agent.indexOf('win32') >= 0 || agent.indexOf('wow32') >= 0) {
      //your code
      return 'windows';
    }
    if (agent.indexOf('win64') >= 0 || agent.indexOf('wow64') >= 0) {
      //your code
      return 'windows';
    }
    if (isMac) {
      //your code
      return 'macos';
    }
    return 'linux';
  }

  initial(target: T, action?: T) {
    this.spaceKeyDowned = false;
    this.moving = false;
    this.offset = { x: 0, y: 0 };
    this.cacheOffset = { x: 0, y: 0 };
    this.zoom = NORMAL_ZOOM;
    this.target = target;
    this.action = action;
    this.initialEvents();
  }
  on(name: Events, callback: (...args: any | undefined) => void) {
    this.listeners.push({ name, callback });
  }
  private emit(name: Events, ...args: any | undefined) {
    this.listeners.forEach((listener) => {
      if (listener.name === name) listener.callback(...args);
    });
  }
  private initialEvents() {
    this.target.addEventListener('mousedown', this.mousedown.bind(this));
    document.addEventListener('mouseup', this.mouseup.bind(this));
    this.target.addEventListener('mousemove', this.mousemove.bind(this));
    document.addEventListener('keydown', this.keydown.bind(this));
    document.addEventListener('keyup', this.keyup.bind(this));

    const debounceResize = debounce(this.resize.bind(this), 500);
    window.addEventListener('resize', debounceResize);
    this.debounceResize = debounceResize;
    this.target.addEventListener('wheel', this.wheel.bind(this), false);
  }
  private wheel(evt: any) {
    evt.preventDefault();
    const { deltaX, deltaY, ctrlKey, metaKey } = evt;
    let zoom = this.zoom || 1;
    if (this.os === 'macos') {
      if (ctrlKey) {
        // touchpad
        if (evt.deltaY > 0) {
          // zoom out
          zoom = Math.min(zoom - SCALE_TATE, MAX_ZOOM);
        } else {
          // zoom in
          zoom = Math.max(zoom + SCALE_TATE, MIN_ZOOM);
        }
        if (zoom !== this.zoom && zoom <= MAX_ZOOM && zoom >= MIN_ZOOM)
          this.setZoom(zoom);
      }
      if (metaKey) {
        // mouse wheel
        if (evt.deltaY > 0) {
          // zoom in
          zoom = Math.min(zoom + SCALE_TATE, MAX_ZOOM);
        } else {
          // zoom out
          zoom = Math.max(zoom - SCALE_TATE, MIN_ZOOM);
        }
        if (zoom !== this.zoom && zoom <= MAX_ZOOM && zoom >= MIN_ZOOM)
          this.setZoom(zoom);
      }
    } else {
      // windows or linux
      if (ctrlKey) {
        // touchpad and mouse wheel
        if (evt.deltaY > 0) {
          // zoom in
          zoom = Math.min(zoom + SCALE_TATE, MAX_ZOOM);
        } else {
          // zoom out
          zoom = Math.max(zoom - SCALE_TATE, MIN_ZOOM);
        }
        if (zoom !== this.zoom && zoom <= MAX_ZOOM && zoom >= MIN_ZOOM)
          this.setZoom(zoom);
      }
    }

    if (this.os === 'macos') {
      this.transform(Math.trunc(-deltaX / 3), Math.trunc(-deltaY / 3));
    } else {
      this.transform(Math.trunc(deltaX / 3), Math.trunc(deltaY / 3));
    }
    this.cacheOffset = { ...this.offset };
  }
  private debounceResize: DebouncedFunc<any>;
  private resize() {
    this.emit('resize');
  }
  zoomIn() {
    const zoom = Math.min(this.zoom + SCALE_TATE, MAX_ZOOM);
    if (zoom !== this.zoom) this.setZoom(zoom);
  }
  zoomOut() {
    const zoom = Math.max(this.zoom - SCALE_TATE, MIN_ZOOM);
    if (zoom !== this.zoom) this.setZoom(zoom);
  }
  private setZoom(zoom: number) {
    const element = this.action || this.target;
    element.style.transform = `translate3d(${this.offset.x}px, ${this.offset.y}px, 0px) scale(${zoom})`;
    this.zoom = zoom;
    this.emit('zoom', zoom);
  }
  private keydown(evt: KeyboardEvent) {
    if (evt.key === SPACE_KEY) {
      this.spaceKeyDowned = true;
    }
  }
  private keyup(evt: KeyboardEvent) {
    if (evt.key === SPACE_KEY) {
      this.spaceKeyDowned = false;
    }
  }
  private mousedown(evt: MouseEvent) {
    if (this.canbeMoving(evt.target as HTMLElement)) {
      this.start = { x: evt.clientX, y: evt.clientY };
    }
  }
  private canbeMoving(target: HTMLElement) {
    let condition = target === this.target || target === this.action;
    // (target === (this.target | this.action) || unMovingClassNames)  &&  contrlMoving
    if (!condition && this.config.unMovingClassNames) {
      const rootClassNames = [
        this.action?.classList.toString(),
        this.target?.classList.toString(),
      ].filter(Boolean);
      let node = target;
      while (node) {
        if (
          this.config.unMovingClassNames.some((className) =>
            node.classList.contains(className)
          )
        ) {
          condition = false;
          break;
        }
        if (
          rootClassNames.some((rootClassName) =>
            node.classList.contains(rootClassName)
          )
        ) {
          condition = true;
          break;
        }
        node = node.parentElement;
      }
    }
    if (this.config.contrlMoving) {
      condition = condition && this.spaceKeyDowned;
    }
    return condition;
  }
  private mouseup() {
    this.moving = false;
    this.start = null;
    if (this.target) this.target.style.cursor = 'grab';
    if (this.action) this.action.style.cursor = 'grab';
    this.cacheOffset = { ...this.offset };
  }
  private mousemove(evt: MouseEvent) {
    if (this.moving) {
      const [x, y] = this.computeMove(evt.clientX, evt.clientY);
      this.transform(x, y);
      return;
    }
    if (this.moveDistance() > MIN_DISTANCE) {
      this.moving = true;
    }
  }
  private transform(x: number, y: number) {
    if (this.target) {
      let { x: ox, y: oy } = this.cacheOffset;
      ox += x;
      oy += y;
      this.offset = { x: ox, y: oy };
      const element = this.action || this.target;
      element.style.transform = `translate3d(${ox}px, ${oy}px, 0px) scale(${this.zoom})`;
      element.style.transformOrigin = '0 50% 0';

      if (this.action) this.action.style.cursor = 'grabbing';
      this.target.style.cursor = 'grabbing';
    }
  }
  private computeMove(pageX: number, pageY: number) {
    let moveX = 0,
      moveY = 0;
    if (this.start) {
      if (pageX < this.start.x) {
        moveX = pageX - this.start.x;
      }
      if (pageX > this.start.x) {
        moveX = pageX - this.start.x;
      }
      if (pageY < this.start.y) {
        moveY = pageY - this.start.y;
      }
      if (pageY > this.start.y) {
        moveY = pageY - this.start.y;
      }
    }
    return [moveX, moveY];
  }
  private moveDistance() {
    if (this.start) {
      return Math.sqrt(this.start.x ** 2 + this.start.y ** 2) / 2;
    }
    return 0;
  }
  destroy() {
    this.destroyEvents();
    this.start = null;
    this.moving = false;
    this.target = null;
    this.action = null;
    this.offset = { x: 0, y: 0 };
    this.zoom = 1;
    this.spaceKeyDowned = false;
    this.listeners = [];
  }
  private destroyEvents() {
    document.removeEventListener('mouseup', this.mouseup.bind(this));
    document.removeEventListener('keydown', this.keydown.bind(this));
    document.removeEventListener('keyup', this.keyup.bind(this));
    window.removeEventListener('resize', this.debounceResize);
  }
}

class MoveAndZoomStore {
  private store = new Map<string, MoveAndZoom<HTMLElement>>();

  getInstance(key: string, config?: Config) {
    if (this.store.has(key)) {
      return this.store.get(key);
    }
    const instance = new MoveAndZoom<HTMLElement>(
      config || {
        contrlMoving: false,
        unMovingClassNames: ['node', 'mind-graph-node-wrapper'],
      }
    );
    this.store.set(key, instance);
    return instance;
  }
  removeInstance(key: string) {
    if (this.store.has(key)) {
      this.store.get(key).destroy();
      this.store.delete(key);
    }
  }
}

const store = new MoveAndZoomStore();
if (window) {
  (window as any).store = store;
}
export default store;
