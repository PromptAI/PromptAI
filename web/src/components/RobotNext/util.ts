
export class ArrayHelper {
  static add<T>(array: T[], item: T) {
    return [...array, item];
  }
  static override<T>(array: T[], item: T, find: (f: T, i?: number) => boolean) {
    const index = array.findIndex(find);
    if (index < 0) {
      return array;
    }
    return [...array.slice(0, index), item, ...array.slice(index + 1)];
  }
  static update<T>(
    array: T[],
    item: Partial<T>,
    find: (f: T, i?: number) => boolean
  ) {
    const index = array.findIndex(find);
    if (index < 0) {
      return array;
    }
    return [
      ...array.slice(0, index),
      { ...array[index], ...item },
      ...array.slice(index + 1),
    ];
  }
  static del<T>(array: T[], find: (f: T, i?: number) => boolean) {
    const index = array.findIndex(find);
    if (index < 0) {
      return array;
    }
    return [...array.slice(0, index), ...array.slice(index + 1)];
  }
}