import { render, screen } from '@testing-library/react';
import { SidebarHeader } from '../SidebarHeader.tsx';

describe('SidebarHeader', () => {
  it('should render logo, text and button when not collapsed', () => {
    render(<SidebarHeader collapsed={false} toggleSidebar={() => {}} />);

    expect(screen.getByAltText('full-logo')).toBeInTheDocument();
    expect(screen.queryAllByAltText('icon-logo')).toHaveLength(0);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('should render only logo, not text and button when collapsed', () => {
    render(<SidebarHeader collapsed={true} toggleSidebar={() => {}} />);

    expect(screen.getByAltText('icon-logo')).toBeInTheDocument();
    expect(screen.queryAllByAltText('full-logo')).toHaveLength(0);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });
});
