import { get, post } from "@/utils/request";

export async function initilRasaEnv() {
  return get('/api/published/project/debug/status')
}
export async function init() {
  return post('/api/published/project/debug/init');
}