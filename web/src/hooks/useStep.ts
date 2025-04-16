import { useCallback, useState } from "react";

interface IParams {
  completeStep: number;
  onComplete?: () => void;
  nextCondition?: (step: number) => Promise<boolean>;
}
export default function useStep({ completeStep, onComplete, nextCondition }: IParams) {
  const [nextLoading, setLoading] = useState(false);
  const [step, setStep] = useState(0);
  const next = async () => {
    if (step < completeStep) {
      setLoading(true)
      const condition = await (nextCondition ? nextCondition(step) : Promise.resolve(true));
      if (condition) {
        setStep(step + 1);
      }
      setLoading(false)
    } else {
      onComplete?.();
    }
  }
  const pre = useCallback(() => {
    setStep(s => Math.max(s - 1, 0))
  }, []);
  return { step, next, pre, nextLoading }
}