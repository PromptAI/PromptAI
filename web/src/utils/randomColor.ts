export default function randomColor() {
  return '#' + ((Math.random() * 0x1000000 << 0).toString(16)).substring(-6);
}