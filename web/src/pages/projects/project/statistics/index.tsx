import { Select, Typography } from '@arco-design/web-react';
import dayjs from 'dayjs';
import * as React from 'react';
import styled from 'styled-components';
import Summary from './components/Summary';
import Faq from './components/Faq';
import Flow from './components/Flow';
import Fallback from './components/Fallback';
import KnowledgeBase from './components/knowledgeBase';
import i18n from './i18n';
import useLocale from '@/utils/useLocale';
import { SummaryData } from './types';

const DateRangeContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
`;
const TitleContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;
const Section = styled.div`
  padding: 16px;
`;

const Statistics: React.FC = () => {
  const t = useLocale(i18n);
  const now = React.useMemo(() => dayjs(), []);
  const [dateRange, setDateRange] = React.useState<number[]>(() => [
    now.subtract(7, 'day').startOf('day').valueOf(),
    now.endOf('day').valueOf(),
  ]);
  const onDateRangeChange = React.useCallback(
    (day) => {
      setDateRange([
        now.subtract(Number(day), 'day').startOf('day').valueOf(),
        now.endOf('day').valueOf(),
      ]);
    },
    [now]
  );
  const [summary, setSummary] = React.useState<SummaryData>();
  return (
    <>
      <Section>
        <TitleContainer>
          <Typography.Title heading={5} style={{ margin: 0 }}>
            {t['title']}
          </Typography.Title>
          <DateRangeContainer>
            <Typography.Text>{t['dateRange']}</Typography.Text>
            <Select
              style={{ color: 'var(--color-text-2)', width: 100 }}
              defaultValue="7"
              onChange={onDateRangeChange}
            >
              <Select.Option value="7">7 {t['day.unit']}</Select.Option>
              <Select.Option value="30">30 {t['day.unit']}</Select.Option>
              <Select.Option value="90">90 {t['day.unit']}</Select.Option>
            </Select>
          </DateRangeContainer>
        </TitleContainer>
      </Section>
      <Section>
        <Summary
          startTime={dateRange[0]}
          endTime={dateRange[1]}
          onChange={setSummary}
        />
      </Section>
      <Section>
        <Faq
          startTime={dateRange[0]}
          endTime={dateRange[1]}
          summary={summary}
        />
      </Section>
      <Section>
        <Flow
          startTime={dateRange[0]}
          endTime={dateRange[1]}
          summary={summary}
        />
      </Section>
      <Section>
        <KnowledgeBase
          startTime={dateRange[0]}
          endTime={dateRange[1]}
          summary={summary}
        />
      </Section>
      <Section>
        <Fallback
          startTime={dateRange[0]}
          endTime={dateRange[1]}
          summary={summary}
        />
      </Section>
    </>
  );
};

export default Statistics;
