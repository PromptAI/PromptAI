import { isString } from 'lodash';

export function includesRegExp(
  array: string[],
  regxp: string | RegExp
): boolean {
  const rule = isString(regxp) ? new RegExp(regxp) : regxp;
  return array.some((s) => {
    const r = rule.test(s);
    return r;
  });
}
export function regxpsSomeMacth(
  regxps: string[] | RegExp[],
  target: string
): boolean {
  const rules: RegExp[] = regxps.map((r) => (isString(r) ? new RegExp(r) : r));
  return rules.some((re) => re.test(target));
}
