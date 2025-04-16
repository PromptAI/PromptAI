import { useKeyPress } from 'ahooks';

export default function useEnter(callback: () => void) {
  useKeyPress('enter', (evt: any) => {
    if ((evt.target as HTMLElement).nodeName === 'BODY') {
      callback();
    }
  });
}
