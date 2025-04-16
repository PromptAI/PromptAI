import { post } from '@/utils/request';

export type GenerateExampleParams = {
  intent: string;
  count: number; // min 1, max 30
  exts?: string[];
  answer?: string;
};
export async function generateExample(data: GenerateExampleParams) {
  return post('/api/project/component/generate/intent', data);
}
