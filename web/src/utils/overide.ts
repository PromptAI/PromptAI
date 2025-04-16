import { omit, xor } from 'lodash';

export default function overide(source: object, overide: object) {
  const value = { ...source, ...overide };
  return omit(value, xor(Object.keys(source), Object.keys(overide)));
}
