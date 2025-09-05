import { describe, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NavHeader } from '../header/NavHeader.tsx';

vi.mock('@/components/layouts/header/AvatarMenu.tsx', () => ({
  AvatarMenu: () => <div data-testid="avatar-menu-mock"></div>,
}));

describe('NavHeader', () => {
  it('should render the page name', () => {
    render(<NavHeader pageName={'Home'} />);

    expect(screen.queryByText('Home')).toBeInTheDocument();
  });
});
