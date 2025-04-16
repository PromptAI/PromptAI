export interface DeleteColumnProps {
  row: any;
  dataIndex?: string;
  promise: (ids: string[]) => Promise<any>;
  onSuccess: (val: any) => void;
  title?: string;
}