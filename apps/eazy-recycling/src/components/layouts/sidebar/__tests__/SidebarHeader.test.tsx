import { render, screen } from '@testing-library/react';
import { SidebarHeader } from '../SidebarHeader.tsx';

describe('SidebarHeader', () => {
  it('should render logo, text and button when not collapsed', () => {
    render(<SidebarHeader collapsed={false} toggleSidebar={() => {}} />);

    expect(screen.getByText('Eazy Recycling')).toBeInTheDocument();
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.getByAltText('Logo')).toBeInTheDocument();
  });

  it('should render only logo, not text and button when collapsed', () => {
    render(<SidebarHeader collapsed={true} toggleSidebar={() => {}} />);

    expect(screen.queryByText('Eazy Recycling')).not.toBeInTheDocument();
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.getByAltText('Logo')).toBeInTheDocument();
  });
});
