export function getObjectJsonFile(object: any, filename?: string) {
  return new File(
    [JSON.stringify(object, null, 0)],
    `${filename || Date.now()}.json`,
    {
      type: 'application/json',
    }
  );
}
export function getObjectJsonFileFormData(object: {
  data: any;
  filename?: string;
  projectId: string;
  componentId: string[];
}) {
  const form = new FormData();
  for (let i = 0; i < object.componentId.length; i++) {
    form.append('componentIds', object.componentId[i]);
  }
  form.append('file', getObjectJsonFile(object.data, object.filename));
  form.append('projectId', object.projectId);
  return form;
}

export function getFileUrl(object: File) {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = () => {
      resolve(fileReader.result);
      reject(fileReader.error);
    };
    fileReader.readAsDataURL(object);
  });
}
