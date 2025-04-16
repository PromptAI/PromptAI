import React, {useEffect, useState} from 'react';
import {getTemplate} from '@/api/template';
import TemplateNotEnable from '@/pages/entry/templates/components/TemplateNotEnable';
import TemplateView from '@/pages/entry/templates/components/TemplateView';
import {useHistory} from 'react-router';
import i18n from './locale';
import useLocale from '@/utils/useLocale';

const Templates: React.FC = () => {
  const [template, setTemplate] = useState(null);
  const history = useHistory();
  const t = useLocale(i18n);

  useEffect(() => {
    // 获取当前页面的完整URL
    const currentUrl = window.location.href;

    const pathSegments = currentUrl.split('/');
    let projectId = pathSegments[pathSegments.length - 1];

    // 有其他附加?的参数，现在忽略掉
    if (projectId.includes('?')) {
      const projectAndQueryParam = projectId.split('?');
      projectId = projectAndQueryParam[0];
    }

    // query param
    if (projectId) {
      getTemplate(projectId).then((res) => {
        // 这表示当前的模板可以用
        const ok = res.ok;
        if (ok) {
          setTemplate(res);
        }
      });
    }
  }, []);

  return (
    <div className={'flex-row justify-center mt-28'}>
      {/* header */}

      <div className={' h-4/5'}>
        {!template && <TemplateNotEnable />}

        {template && <TemplateView data={template} />}
      </div>

      <div className="flex justify-center mt-16">
        {/*<Button*/}
        {/*  type={'primary'}*/}
        {/*  className={'rounded-lg'}*/}
        {/*  onClick={() => history.push('/login')}*/}
        {/*>*/}
        {/*  {t['template.login.title']}*/}
        {/*</Button>*/}
        <a className={'text-orange-600 italic text-3xl cursor-pointer' } onClick={() => history.push('/login')}>  {t['template.login.title']}</a>
      </div>
    </div>
  );
};

export default Templates;
