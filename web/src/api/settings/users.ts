import { del, get, post, put } from "@/utils/request";
import QueryString from "qs";

export async function pageUsers(params) {
  return get('/api/settings/users', params)
}
export async function createUser(data) {
  return post('/api/settings/users', data)
}
export async function updateUser(data) {
  return put('/api/settings/users', data)
}
export async function updateUserPwd(password, id) {
  return post('/api/settings/users/resetpass', { newPass: password, id })
}
export async function deleteUser(ids) {
  return del(`/api/settings/users?${QueryString.stringify({ ids }, { indices: false })}`);
}