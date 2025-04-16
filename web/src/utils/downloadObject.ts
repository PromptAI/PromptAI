export default function downloadJSON(content: any, name: string) {
  // 下载保存json文件
  const eleLink = document.createElement('a');
  eleLink.download = `${name}.json`;
  eleLink.style.display = 'none';
  // 字符内容转变成blob地址
  const data = JSON.stringify(content, undefined, 0);
  const blob = new Blob([data], { type: 'text/json' });
  eleLink.href = URL.createObjectURL(blob);
  // 触发点击
  document.body.appendChild(eleLink);
  eleLink.click();
  // 然后移除
  document.body.removeChild(eleLink);
}
export function downloadUrlFile(url: string) {
  const eleLink = document.createElement('a');
  eleLink.download = url
    .replace('http://', '')
    .replace('/', '')
    .replaceAll('/', '-');
  eleLink.style.display = 'none';
  // 字符内容转变成blob地址
  eleLink.href = url;
  // 触发点击
  document.body.appendChild(eleLink);
  eleLink.click();
  // 然后移除
  document.body.removeChild(eleLink);
}
export const downloadFile = (response, name?: string) => {
  if (!response) {
    return;
  }
  const blob = new Blob([response.data]);
  let fileName = name;
  const disposition =
    response.headers && response.headers['content-disposition'];
  if (disposition) {
    fileName = disposition.split('filename=')[1];
    fileName = decodeURIComponent(fileName) || fileName; // 中文处理
  }
  if ('download' in document.createElement('a')) {
    // 非IE下载
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  } else {
    (navigator as any).msSaveBlob(blob, fileName);
  }
};
