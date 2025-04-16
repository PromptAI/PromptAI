import { get } from "@/utils/request";

export async function listBuiltinActions() {
  return get('/api/project/component/internal/action')
}