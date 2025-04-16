export function findParentForm(node) {
  let p = node;
  while (p) {
    if (p.type === 'form' || p.type === 'form-gpt') break;
    p = p.parent;
  }
  return p;
}
