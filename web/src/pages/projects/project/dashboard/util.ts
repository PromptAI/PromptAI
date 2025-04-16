import moment from 'moment';

export function buildParams(time: string, other: any) {
  const startTime = moment()
    .subtract(Number(time), 'days')
    .startOf('days')
    .valueOf();
  return {
    startTime,
    endTime: moment().endOf('days').valueOf(),
    ...other,
  };
}
