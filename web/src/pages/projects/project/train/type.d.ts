type ResetSlot = {
  /**
   * slot id
   */
  id: string;
  /**
   * function body: new Function(body)();
   */
  body: string;
};
export type Settings = {
  name: string;
  survey: boolean;
  upload: boolean;
  welcome: string;
  locale: string;
  theme: string;
  schedule: string;
  /// web settings
  slots: ResetSlot[];
  /// mobile
};
