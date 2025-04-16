/// [x, y]
type Point = [number, number];
class Position {
  left: Point;
  right: Point;
  static fromDOMRect(rect: DOMRect) {
    const { left, top, width, height } = rect;
    const position = new Position();
    position.left = [left, top + height / 2];
    position.right = [left + width, top + height / 2];
    return position;
  }
}
export default class PositionHub {
  name: string;
  private value: Map<string, Position | undefined>;
  constructor(name: string) {
    this.name = name;
    this.value = new Map();
  }

  set(key: string, rect: DOMRect) {
    this.value.set(key, Position.fromDOMRect(rect));
  }
  get(key: string): Position | undefined {
    return this.value.get(key);
  }
}
