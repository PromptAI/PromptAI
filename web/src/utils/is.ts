export function isArray(val): boolean {
  return Object.prototype.toString.call(val) === '[object Array]';
}
export function isObject(val): boolean {
  return Object.prototype.toString.call(val) === '[object Object]';
}
export function isString(val): boolean {
  return Object.prototype.toString.call(val) === '[object String]';
}

export const isSSR = (function () {
  try {
    return !(typeof window !== 'undefined' && document !== undefined);
  } catch (e) {
    return true;
  }
})();

export const isBlank = (str: string): boolean => {
  if (str === '') return true;
  const regu = '^[ ]+$';
  const re = new RegExp(regu);
  return re.test(str);
};

export const isNotNone = (obj) => {
  return obj !== null && obj !== undefined;
};
