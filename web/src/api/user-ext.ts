import { post } from "@/utils/request";

export async function importUserExt(file) {
  const form = new FormData();
  form.append('file', file);
  return post('/api/project/component/user/ext/parse', form)
}