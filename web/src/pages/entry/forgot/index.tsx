import EntryLayout from '@/pages/components/entry-layout';
import React from 'react';
import ForgotForm from './ForgotForm';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

export default function Forgot() {
  const t = useLocale(i18n);
  return (
    <EntryLayout title={t['forgot.title']} types={[]} value="forgot">
      <ForgotForm />
    </EntryLayout>
  );
}
