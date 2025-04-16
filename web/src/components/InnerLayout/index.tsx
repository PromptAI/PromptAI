import { Breadcrumb, Link } from '@arco-design/web-react';
import React, { ReactNode } from 'react';
import { Link as RouteLink } from 'react-router-dom';

export type BreadcrumbType = {
  text: ReactNode;
  link?: string;
};
interface InnerLayoutProps {
  breadcrumbs: BreadcrumbType[];
  children: ReactNode;
  extra?: ReactNode;
}

const InnerLayout = (props: InnerLayoutProps) => {
  const { breadcrumbs, children, extra } = props;
  return (
    <>
      <div className="app-breadcrumb app-flex-between">
        <Breadcrumb>
          {breadcrumbs.map(({ text, link }, index) => (
            <Breadcrumb.Item key={index}>
              {link ? (
                <RouteLink to={link}>
                  <Link style={{ padding: 0, margin: 0 }}>{text}</Link>
                </RouteLink>
              ) : (
                text
              )}
            </Breadcrumb.Item>
          ))}
        </Breadcrumb>
        <div>{extra}</div>
      </div>
      <div style={{ height: 'calc(100vh - 120px)' }}>{children}</div>
    </>
  );
};

export default InnerLayout;
