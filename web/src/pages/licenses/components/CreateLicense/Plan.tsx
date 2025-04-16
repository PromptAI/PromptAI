import React, { useState } from 'react';
import { Button, Message } from '@arco-design/web-react';
import { AiOutlineCheckCircle } from 'react-icons/ai';
import Token from '@/utils/token';
import formatMoney from '@/utils/CurrencyUtil';

const MoneyUnit = ({ money, unit }) => (
  <div className="justify-center flex items-end justify-center">
    <div className="h-10">
      <p className="m-0 text-4xl font-medium">
        {money <= 0 ? 'Free ' : ' '}
        {formatMoney(money / 100)}
      </p>
    </div>
    <span className="opacity-80">/{unit}</span>
  </div>
);

const PricingCard = ({ title, money, items, data, onSubscribe }) => {
  const [loading, setLoading] = useState(false);
  return (
    <div className="w-full md:max-w-md px-8 py-4 rounded-lg flex flex-col text-xl border shadow {className}">
      <div className="space-y-8">
        <div className="space-y-4">
          <span className="font-medium opacity-60">{title}</span>
          <MoneyUnit money={money} unit={'Month'} />
        </div>
        <ul className="m-0 space-y-2">
          {items.map((item) => (
            <li
              key={item}
              className="marker:text-gray-800 m-0 flex gap-4 items-center"
            >
              <AiOutlineCheckCircle className="text-2xl" />
              <span>{item}</span>
            </li>
          ))}
        </ul>
      </div>
      <div className="mt-8">
        {/* jump to stripe to buy   */}
        {money > 0 && (
          <form
            action={
              '/api/stripe/create/session?id=' +
              data.id +
              '&token=' +
              Token.get()
            }
            method="POST"
          >
            <Button
              htmlType={'submit'}
              type={'primary'}
              className="h-10 w-full primary"
              loading={loading}
              onClick={() => {
                setLoading(true);
              }}
            >
              Subscribe
            </Button>
          </form>
        )}

        {/*  Free */}
        {money <= 0 && (
          <Button
            type={'primary'}
            className="h-10 w-full primary"
            loading={loading}
            onClick={() => {
              try {
                setLoading(true);
                onSubscribe(data);
              } catch (e) {
                Message.error(e.message);
              } finally {
                setLoading(false);
              }
            }}
          >
            Free
          </Button>
        )}
      </div>
    </div>
  );
};

const Plan = ({ data, onSubscribe }) => {
  console.log(JSON.stringify(data));
  const descriptions = data.license.description.split('\n');
  return (
    <div>
      <div className="flex justify-center flex-wrap gap-4 w-full">
        <PricingCard
          title={data.name}
          money={data.salePrice}
          items={descriptions}
          data={data}
          onSubscribe={onSubscribe}
        />
      </div>
    </div>
  );
};

export default Plan;
