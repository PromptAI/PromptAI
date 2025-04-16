import { getStripePublicKey, packages, payment } from '@/api/stripe';
import { Message, Modal } from '@arco-design/web-react';
import React, { useEffect, useState } from 'react';
import Plan from '@/pages/tokens/BuyToken/Plan';
import Pay from '@/pages/tokens/BuyToken/Pay';
import { loadStripe } from '@stripe/stripe-js';

const BuyToken = ({ ...props }) => {
  const [visible, setVisible] = useState(false);
  const [data, setData] = useState([]);

  const [pubkey, setPubkey] = useState();
  const [secret, setSecret] = useState();

  function loadPubKey() {
    getStripePublicKey()
      .then((secrete) => {
        setPubkey(secrete);
      })
      .catch((e) => {
        Message.error(e);
      });
  }

  useEffect(() => {
    loadPubKey();
    loadData();
  }, []);

  function loadData() {
    const param = {
      type: 'token',
    };

    packages(param).then((res) => {
      setData(res);
    });
  }

  function onClickBuy(data) {
    const body = {
      id: data.id,
    };
    setVisible(true);
    payment(body).then((res) => {
      setSecret(res.clientSecret);
    });
  }

  return (
    <div>
      <div className={'flex space-x-4 justify-center'}>
        {data.map((item, index) => {
          return <Plan key={index} data={item} onClickBuy={onClickBuy} />;
        })}
      </div>
      {secret && (
        <Modal visible={visible} simple={true} footer={null}>
          <Pay
            key={secret}
            clientSecret={secret}
            stripePromise={loadStripe(pubkey)}
            onCancel={setVisible}
          />
        </Modal>
      )}
    </div>
  );
};

export default BuyToken;
