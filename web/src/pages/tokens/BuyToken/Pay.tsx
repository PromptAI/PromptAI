import React from 'react';
import { Elements } from '@stripe/react-stripe-js';
import CheckoutForm from './CheckoutForm';
import { Button, Modal } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from '../locale';

const Pay = ({
  clientSecret,
  stripePromise,
  onCancel,
}: {
  clientSecret: any;
  stripePromise: any;
  onCancel: (boolean) => void;
}) => {
  const t = useLocale(i18n);
  return (
    <>
      <div className="modal cursor-pointer flex w-full">
        <div
          className="modal-box p-0 w-full min-w-xl"
          onClick={(evt) => evt.stopPropagation()}
        >
          <div className="p-4 pb-2 border-b-[1px] border-solid border-b-base-content border-opacity-20">
            {'Pay'}
          </div>
          <div className="px-8 py-4">
            <Elements
              stripe={stripePromise}
              options={{ clientSecret, appearance: { theme: 'stripe' } }}
            >
              <CheckoutForm clientSecret={clientSecret} />
            </Elements>
          </div>
          <div className="modal-action border-t-[1px] flex justify-end border-solid border-t-base-content border-opacity-20 p-4 mt-0 min-w-[3.5rem]">
            <Button className="btn btn-sm" onClick={() => onCancel(false)}>
              {t['token.recharge.cancel']}
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};
export default Pay;
