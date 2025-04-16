export default function overviewMerge(
  overview: any,
  source: any,
  overviewKeys: string[]
) {
  const obj = Object.assign(source, overview);
  overviewKeys.forEach((key) => {
    obj[key] = overview[key];
  });
  return obj;
}
