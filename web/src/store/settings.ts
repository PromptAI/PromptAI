import defaultSettings from '../settings.json';
import { Action } from './types';

export interface SettingsState {
  colorWeek: boolean;
  navbar: boolean;
  menu: boolean;
  footer: boolean;
  themeColor: string;
  menuWidth: number;
}

const initialState: SettingsState = {
  ...defaultSettings,
};

const update = (
  state: SettingsState,
  payload: SettingsState
): SettingsState => {
  return { ...state, ...payload };
};

export default function settingsReducer(
  state = initialState,
  action: Action<SettingsState>
) {
  switch (action.type) {
    case 'settings/update': {
      return update(state, action.payload);
    }
    default:
      return state;
  }
}
