import { useKeyPress } from 'ahooks';

export default function useTab(callback: () => void) {
  useKeyPress('tab', (evt) => {
    if ((evt.target as HTMLElement).nodeName === 'BODY') {
      evt.stopPropagation();
      evt.preventDefault();
      callback();
    }
  });
}
