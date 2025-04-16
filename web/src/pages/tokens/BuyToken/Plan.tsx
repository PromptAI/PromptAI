import React, { useState } from 'react';
import { Button } from '@arco-design/web-react';
import { AiOutlineCheckCircle } from 'react-icons/ai';
import formatMoney from '@/utils/CurrencyUtil';

const MoneyUnit = ({ money, token }) => (
  <div className="justify-center flex items-end ">
    <div className="h-10">
      <p className="m-0 text-4xl font-medium">
        {money <= 0 ? 'Free ' : ' '}
        {formatMoney(money / 100)}
      </p>
    </div>
    <span className="opacity-80">/{Number(token).toLocaleString()}</span>
  </div>
);

const PricingCard = ({ title, money, items, data, onClickBuy }) => {
  const [loading, setLoading] = useState(false);
  return (
    <div className="w-full md:max-w-md px-8 py-4 rounded-lg flex flex-col text-xl border shadow {className}">
      <div className="space-y-8">
        <div className="space-y-4">
          <span className="font-medium opacity-60">{title}</span>
          <MoneyUnit money={money} token={data.token.token} />
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
        <Button
          htmlType={'submit'}
          type={'primary'}
          className="h-10 w-full primary"
          loading={loading}
          onClick={() => {
            setLoading(true);
            try {
              onClickBuy(data);
            } finally {
              setLoading(false);
            }
          }}
        >
          Buy
        </Button>
      </div>
    </div>
  );
};

const Plan = ({ data, onClickBuy }) => {
  console.log(JSON.stringify(data));
  const descriptions = data.token.description.split('\n');
  return (
    <div>
      <div className="flex justify-center flex-wrap gap-4 w-full">
        <PricingCard
          title={data.name}
          money={data.salePrice}
          items={descriptions}
          data={data}
          onClickBuy={onClickBuy}
        />
      </div>
    </div>
  );
};

export default Plan;
