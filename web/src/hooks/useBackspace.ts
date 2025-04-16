import { useKeyPress } from 'ahooks';

export default function useBackspace(callback: () => void) {
  useKeyPress('Backspace', (evt) => {
    if ((evt.target as HTMLElement).nodeName === 'BODY') {
      callback();
    }
  });
}
