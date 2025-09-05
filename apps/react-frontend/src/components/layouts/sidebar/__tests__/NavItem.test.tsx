import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NavItem } from '../NavItem.tsx';

describe('basic arithmetic checks', () => {
  it('1 + 1 equals 2', () => {
    expect(1 + 1).toBe(2);
  });

  it('2 * 2 equals 4', () => {
    expect(2 * 2).toBe(4);
  });
});

describe('NavItem', () => {
  it('should display label when not collapsed', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <NavItem
          icon={() => <svg data-testid="nav-icon" />}
          label="Home"
          collapsed={false}
          to={'/'}
        />
      </MemoryRouter>
    );

    expect(screen.getByTestId('nav-icon')).toBeInTheDocument();
    expect(screen.getByText('Home')).toBeInTheDocument();
  });

  it('should not display label when not collapsed', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <NavItem
          icon={() => <svg data-testid="nav-icon" />}
          label="Home"
          collapsed={true}
          to={'/'}
        />
      </MemoryRouter>
    );

    expect(screen.getByTestId('nav-icon')).toBeInTheDocument();
    expect(screen.queryByText('Home')).not.toBeInTheDocument();
  });
});
