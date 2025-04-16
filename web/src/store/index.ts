import { useSelector } from 'react-redux';
import { combineReducers, createStore } from 'redux';
import settingsReducer, { SettingsState } from './settings';

export interface UserState {
  name?: string;
  username?: string;
  mobile?: string;
  initPass?: string;
  avatar?: string;
  job?: string;
  organization?: string;
  location?: string;
  email?: string;
  restToken?: string;
  permissions: Record<string, string[]>;
  featureEnable?: boolean;
}

const initialState: UserState = {
  permissions: {},
};

function userReducer(state = initialState, action) {
  switch (action.type) {
    case 'update-userInfo': {
      const { userInfo = initialState, userLoading } = action.payload;
      return {
        ...state,
        userLoading,
        ...userInfo,
      };
    }
    default:
      return state;
  }
}

export type RootState = {
  user: UserState;
  settings: SettingsState;
};

const store = createStore(
  combineReducers({
    user: userReducer,
    settings: settingsReducer,
  })
);

export default store;
export function useSelectorStore<T>(key: string) {
  return useSelector<RootState, T>((state) => state[key]);
}
