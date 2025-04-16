export async function serialize(data: any) {
  return new Promise((resolve) => {
    window.localStorage.setItem('serialize', JSON.stringify(data));
    resolve(undefined);
  });
}
export async function unSerialize() {
  return new Promise((resolve, reject) => {
    try {
      const data = JSON.parse(window.localStorage.getItem('serialize'));
      resolve(data || {});
    } catch (e) {
      reject(e);
    }
  });
}
