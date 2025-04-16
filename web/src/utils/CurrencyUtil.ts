export default function formatMoney(
  amount,
  currency = 'USD',
  locals = 'en-US'
) {
  const options = {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  };
  return new Intl.NumberFormat(locals, options).format(amount);
}
