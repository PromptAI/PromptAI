import { Dispatch, SetStateAction, useEffect, useState } from "react";

export default function useModel<S>(initial?: S) {
  const [state, setState] = useState<S>();
  useEffect(() => {
    setState(initial)
  }, [initial]);
  return [state, setState] as [S, Dispatch<SetStateAction<S>>]
}