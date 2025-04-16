import { Dynamic } from './components/Layout/types';

export type MenuRoute = {
  name: string;
  path: string;
  componentPath?: string;
  icon?: string;
  layout?: 'default' | 'project' | 'lib' | 'settings-layout';
  redirect?: string;
  children?: MenuRoute[];
  hideInMenu?: boolean;
  spliter?: boolean;
  selectable?: boolean;
  dynamic?: Dynamic;
  defaultExpend?: boolean;
};
const routes: MenuRoute[] = [
  {
    name: 'menu.projects',
    path: '/projects',
    componentPath: 'projects'
  },
  {
    name: 'menu.projects.id',
    path: '/projects/:id',
    componentPath: 'projects/project',
    layout: 'project',
    children: [
      // {
      //   name: 'menu.projects.id.overview',
      //   path: '/projects/:id/overview',
      //   componentPath: 'projects/project/overview',
      //   icon: 'home',
      // },
      {
        name: 'menu.projects.id.llm',
        path: '/projects/:id/llm',
        componentPath: 'projects/project/llm',
        icon: 'llm'
      },

      // deprecated
      // {
      //   name: 'menu.projects.id.gpt',
      //   path: '/projects/:id/overview/gpt',
      //   icon: 'faqs',
      //   componentPath: 'projects/project/gpt',
      // },
      // {
      //   name: 'menu.projects.id.knowledge',
      //   path: '/projects/:id/overview/knowledge/text',
      //   icon: 'knowledge',
      //   children: [
      //     {
      //       name: 'menu.projects.id.sample',
      //       path: '/projects/:id/overview/knowledge/sample',
      //       icon: 'faqs',
      //       componentPath: 'projects/project/sample'
      //     },
      //     {
      //       name: 'menu.projects.id.knowledge.text',
      //       path: '/projects/:id/overview/knowledge/text',
      //       icon: 'text',
      //       componentPath: 'projects/project/text'
      //     },
      //     {
      //       name: 'menu.projects.id.knowledge.web',
      //       path: '/projects/:id/overview/knowledge/web',
      //       icon: 'web',
      //       componentPath: 'projects/project/web'
      //     },
      //     {
      //       name: 'menu.projects.id.knowledge.pdf',
      //       path: '/projects/:id/overview/knowledge/pdf',
      //       icon: 'pdf',
      //       componentPath: 'projects/project/pdf'
      //     }
      //   ]
      // },
      {
        name: 'menu.projects.id.complex',
        path: '/projects/:id/overview/complexs',
        icon: 'complex',
        dynamic: {
          api: '/api/project/component/:id',
          params: {
            type: 'conversation'
          },
          nameIndex: 'data.name',
          menuSuffix: 'branch/complex',
          transformAdd: (values, urlParams) => ({
            id: undefined,
            type: 'conversation',
            relations: [urlParams.id],
            data: values
          }),
          onDeletedRedirectUrl: (dynamic, urlParams) => {
            return `/projects/${urlParams.id}/tool/setting`;
          }
        }
      },
      {
        name: 'menu.projects.id.branch',
        path: '/projects/:id/overview/complexs/:cId/branch/complex',
        componentPath: 'projects/project/conversations/conversation',
        icon: 'complex',
        hideInMenu: true
      },
      {
        name: 'menu.projects.id.webhooks',
        path: '/projects/:id/view/webhooks',
        icon: 'cloud',
        componentPath: 'projects/project/webhooks'
      },
      {
        name: 'menu.projects.id.messages',
        path: '/projects/:id/view/messages',
        icon: 'history',
        componentPath: 'projects/project/messages',
        spliter: true
      },
      {
        name: 'menu.projects.id.webhooks.wId',
        path: '/projects/:id/view/webhooks/info/:wId',
        componentPath: 'projects/project/webhooks/webhook',
        hideInMenu: true
      },
      {
        name: 'menu.projects.id.webhooks',
        path: '/projects/:id/view/webhooks',
        icon: 'cloud',
        componentPath: 'projects/project/webhooks'
      },
      {
        name: 'menu.projects.id.webhooks.create',
        path: '/projects/:id/view/webhooks/create',
        componentPath: 'projects/project/webhooks/create-webhook',
        hideInMenu: true
      },
      // {
      //   name: 'menu.projects.id.favorites',
      //   path: '/projects/:id/favorites/faqs',
      //   icon: 'star',
      //   componentPath: 'projects/project/favorites/faqs',
      //   defaultExpend: false,
      //   children: [
      //     {
      //       name: 'menu.projects.id.favorites.faqs',
      //       path: '/projects/:id/favorites/faqs',
      //       icon: 'faqs',
      //       componentPath: 'projects/project/favorites/faqs',
      //     },
      //     {
      //       name: 'menu.projects.id.favorites.flows',
      //       path: '/projects/:id/favorites/flows',
      //       icon: 'complex',
      //       componentPath: 'projects/project/favorites/flows',
      //     },
      //   ],
      // },
      // {
      //   name: 'menu.projects.id.view',
      //   icon: 'palette',
      //   path: '/projects/:id/view/intents',
      //   componentPath: 'projects/project/global-intent',
      //
      //   children: [
      //     {
      //       name: 'menu.projects.id.intents',
      //       path: '/projects/:id/view/intents',
      //       icon: 'intents',
      //       componentPath: 'projects/project/global-intent',
      //     },
      //     {
      //       name: 'menu.projects.id.bots',
      //       path: '/projects/:id/view/bots',
      //       icon: 'bots',
      //       componentPath: 'projects/project/global-bot',
      //     },
      //     {
      //       name: 'menu.projects.id.slots',
      //       path: '/projects/:id/view/slots',
      //       icon: 'slots',
      //       componentPath: 'projects/project/slots',
      //     },
      //     {
      //       name: 'menu.projects.id.synonyms',
      //       path: '/projects/:id/view/synonyms',
      //       icon: 'synonyms',
      //       componentPath: 'projects/project/synonyms',
      //     },
      //     {
      //       name: 'menu.projects.id.webhooks',
      //       path: '/projects/:id/view/webhooks',
      //       icon: 'cloud',
      //       componentPath: 'projects/project/webhooks',
      //     },
      //     {
      //       name: 'menu.projects.id.webhooks.create',
      //       path: '/projects/:id/view/webhooks/create',
      //       componentPath: 'projects/project/webhooks/create-webhook',
      //       hideInMenu: true,
      //     },
      //     {
      //       name: 'menu.projects.id.webhooks.wId',
      //       path: '/projects/:id/view/webhooks/info/:wId',
      //       componentPath: 'projects/project/webhooks/webhook',
      //       hideInMenu: true,
      //     },
      //     {
      //       name: 'menu.projects.id.action',
      //       path: '/projects/:id/view/action',
      //       icon: 'action',
      //       componentPath: 'projects/project/action',
      //     },
      //     {
      //       name: 'menu.dashboard',
      //       path: '/projects/:id/view/dashboard',
      //       icon: 'dashboard',
      //       componentPath: 'projects/project/dashboard',
      //     },
      //     {
      //       name: 'menu.statistics',
      //       path: '/projects/:id/view/statistics',
      //       icon: 'statistics',
      //       componentPath: 'projects/project/statistics',
      //     },
      //     {
      //       name: 'menu.projects.id.messages',
      //       path: '/projects/:id/view/messages',
      //       icon: 'history',
      //       componentPath: 'projects/project/messages',
      //       spliter: true,
      //     },
      //   ],
      // },
      {
        name: 'menu.projects.id.setting',
        path: '/projects/:id/tool/setting',
        icon: 'setting',
        componentPath: 'projects/project/setting'
      },
      {
        name: 'menu.projects.id.manage',
        path: '/projects/:id/tool/manage',
        icon: 'train',
        componentPath: 'projects/project/train'
      }
      // {
      //   name: 'menu.projects.id.tool',
      //   path: '/projects/:id/tool/manage',
      //   icon: 'archive',
      //   componentPath: 'projects/project/train',
      //   children: [
      //     {
      //       name: 'menu.projects.id.setting',
      //       path: '/projects/:id/tool/setting',
      //       icon: 'setting',
      //       componentPath: 'projects/project/setting'
      //     },
      //     {
      //       name: 'menu.projects.id.manage',
      //       path: '/projects/:id/tool/manage',
      //       icon: 'train',
      //       componentPath: 'projects/project/train'
      //     }
      //     // {
      //     //   name: 'menu.projects.id.cycle',
      //     //   path: '/projects/:id/tool/cycle',
      //     //   icon: 'cycle',
      //     //   componentPath: 'projects/project/cycle-tasks',
      //     // },
      //   ]
      // }
    ]
  },
  {
    name: 'menu.templates',
    path: '/templates',
    componentPath: 'templates'
  },
  {
    name: 'menu.settings',
    path: '/settings',
    layout: 'settings-layout',
    redirect: '/settings/users',
    children: [
      {
        name: 'menu.settings.users',
        path: '/settings/users',
        icon: 'users',
        componentPath: 'settings/users'
      }
    ]
  },
  {
    name: 'menu.libs',
    path: '/libs',
    componentPath: 'libs'
  },
  {
    name: 'menu.profile',
    path: '/profile',
    componentPath: 'profile'
  },
  {
    name: 'menu.tokens',
    path: '/tokens',
    componentPath: 'tokens'
  },
  {
    name: 'menu.sysadmin',
    path: '/sysadmin',
    componentPath: 'sysadmin'
  },
  {
    name: 'menu.404',
    path: '/404',
    componentPath: 'exception/404'
  }
];

export function getAuthRoutes() {
  // todo auth
  return routes;
}
