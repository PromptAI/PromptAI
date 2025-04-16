import Plugin from '../Plugin';
import { isEmpty } from 'lodash';
import moment from 'moment';
import { refreshToken } from '@/api/auth';
import Token from '@/utils/token';

class AutoRefreshToken {
  private schedule: number;
  private interval: any | null;
  constructor(schedule) {
    this.schedule = schedule;
  }
  async start() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
    this.interval = setInterval(() => {
      const { token, tokenExpireAt } = Token.getToken();
      let isExpired = false;
      if (isEmpty(token) || tokenExpireAt === 0) {
        isExpired = true;
      }
      const time = moment().add('hours', 2).valueOf();
      if (time > tokenExpireAt) {
        isExpired = true;
      }
      if (isExpired) {
        refreshToken().then((data) => {
          Token.set(data?.token || '', data?.tokenExpireAt);
        });
      }
    }, this.schedule);
  }
  done() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }
}
export default class AutoRefreshTokenPlugin extends Plugin {
  handler: AutoRefreshToken;
  constructor(schedule = 60 * 1000) {
    super('logged');
    this.handler = new AutoRefreshToken(schedule);
  }
  async start() {
    this.handler.start();
  }
  done(): void {
    this.handler.done();
  }
}
