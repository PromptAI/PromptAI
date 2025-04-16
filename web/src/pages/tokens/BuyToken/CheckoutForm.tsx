import React from 'react';
import { useEffect, useState } from 'react';
import {
  PaymentElement,
  useStripe,
  useElements,
} from '@stripe/react-stripe-js';
import ReactLoading from 'react-loading';
import classNames from 'classnames';
import { Message } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from '../locale';
export default function CheckoutForm({
  clientSecret,
}: {
  clientSecret: string;
}) {
  const stripe = useStripe();
  const elements = useElements();
  const [isLoading, setIsLoading] = useState(true);
  const t = useLocale(i18n);
  useEffect(() => {
    if (!stripe || !clientSecret) {
      return;
    }
    stripe.retrievePaymentIntent(clientSecret).then(({ paymentIntent }) => {
      switch (paymentIntent?.status) {
        case 'succeeded':
          Message.success(t['token.recharge.success']);
          break;
        case 'processing':
          Message.success(t['token.recharge.Processing']);
          break;
      }
    });
  }, [stripe, clientSecret]);

  const handleSubmit = async (e: any) => {
    e.preventDefault();

    if (!stripe || !elements) {
      // Stripe.js has not yet loaded.
      // Make sure to disable form submission until Stripe.js has loaded.
      return;
    }

    setIsLoading(true);

    const { error } = await stripe.confirmPayment({
      elements,
      confirmParams: {
        // Make sure to change this to your payment completion page
        return_url: `${window.location.origin}/tokens`,
      },
    });

    // This point will only be reached if there is an immediate error when
    // confirming the payment. Otherwise, your customer will be redirected to
    // your `return_url`. For some payment methods like iDEAL, your customer will
    // be redirected to an intermediate site first to authorize the payment, then
    // redirected to the `return_url`.
    Message.error(error.message as string);

    setIsLoading(false);
  };

  return (
    <form id="payment-form" onSubmit={handleSubmit}>
      <PaymentElement
        id="payment-element"
        options={{
          layout: 'tabs',
          paymentMethodOrder: ['card', 'cashapp', 'alipay', 'wechat_pay'],
        }}
        onLoaderStart={() => setIsLoading(true)}
        onReady={() => setIsLoading(false)}
        onLoadError={() => Message.error('js error')}
      />
      {isLoading ||
        !stripe ||
        (!elements && (
          <ReactLoading type="bars" className="mx-auto" color="hsl(var(--p))" />
        ))}
      <button
        disabled={isLoading || !stripe || !elements}
        id="submit"
        className={classNames(
          'arco-btn arco-btn-primary arco-btn-size-default arco-btn-shape-square h-10 w-full primary',
          {
            loading: isLoading,
          }
        )}
      >
        <span id="button-text">{isLoading ? 'loading' : '支付'}</span>
      </button>
    </form>
  );
}
