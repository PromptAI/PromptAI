export const UNIT_TYPE = {
  standard: 'standard',
};

const UNIT_MAP = {
  [UNIT_TYPE.standard]: [
    {
      unit: 'KB',
      operator: 1024,
    },
    {
      unit: 'MB',
      operator: 1024 ** 2,
    },
    {
      unit: 'GB',
      operator: 1024 ** 3,
    },
    {
      unit: 'TB',
      operator: 1024 ** 4,
    },
  ],
};

export default function toShrinkNumber(
  value,
  type = UNIT_TYPE.standard,
  precision = 3,
  withSuffix = true,
  defaultUnit = '',
) {
  // eslint-disable-next-line eqeqeq
  if (value == 0) {
    return withSuffix && defaultUnit ? `${value}${defaultUnit}` : value;
  }
  const item = [{ unit: defaultUnit, operator: 1 }, ...UNIT_MAP[type]]
    .reverse()
    .map(({ unit, operator }) => {
      if (value >= operator) {
        return { unit, operator };
      }
      return void 0;
    })
    .find((o) => o !== void 0);

  const result = item
    ? Number(Number(value / item.operator).toPrecision(precision)).toString()
    : value;

  if (withSuffix) {
    // eslint-disable-next-line no-nested-ternary
    return `${result} ${item?.unit || defaultUnit}`;
  }

  return result ? `${result}` : value;
}