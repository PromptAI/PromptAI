import { get, put } from "@/utils/request";

export async function getConfig() {
  return get('/api/sysadmin/configuration')
}
type ConfigItem = { name: string, value: string };

export async function updateConfigItem(data: ConfigItem) {
  return put('/api/sysadmin/configuration', data);
}