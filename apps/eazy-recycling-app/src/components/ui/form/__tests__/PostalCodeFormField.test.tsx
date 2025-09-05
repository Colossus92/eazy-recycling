import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useForm } from 'react-hook-form';
import { PostalCodeFormField } from '../PostalCodeFormField';

// Test wrapper component to provide react-hook-form context
const TestWrapper = ({
  defaultValue = '',
  required = true,
  disabled = false,
}: {
  defaultValue?: string;
  required?: boolean;
  disabled?: boolean;
}) => {
  const {
    register,
    setValue,
    formState: { errors },
    watch,
    handleSubmit,
  } = useForm({
    defaultValues: {
      postalCode: defaultValue,
    },
  });

  // Watch the postal code value to display it for testing
  const postalCodeValue = watch('postalCode');

  const onSubmit = () => {
    // Empty submit handler for testing validation
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <PostalCodeFormField
        register={register}
        setValue={setValue}
        errors={errors}
        name="postalCode"
        value={defaultValue}
        required={required}
        disabled={disabled}
      />
      <div data-testid="postal-code-value">{postalCodeValue}</div>
      <button type="submit" data-testid="submit-button">
        Submit
      </button>
    </form>
  );
};

describe('PostalCodeFormField', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders correctly with default props', () => {
    render(<TestWrapper />);

    expect(screen.getByText('Postcode')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Vul postcode in')).toBeInTheDocument();
  });

  it('formats postal code "1234AB" to "1234 AB" on blur', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Type the unformatted postal code
    await user.type(input, '1234AB');
    expect(input).toHaveValue('1234AB');

    // Trigger blur event
    await user.tab();

    // Check that the value is formatted correctly
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent(
        '1234 AB'
      );
    });
  });

  it('formats postal code "1234ab" to "1234 AB" on blur (lowercase letters)', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Type the unformatted postal code with lowercase letters
    await user.type(input, '1234ab');
    expect(input).toHaveValue('1234ab');

    // Trigger blur event
    await user.tab();

    // Check that the value is formatted correctly with uppercase letters
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent(
        '1234 AB'
      );
    });
  });

  it('formats postal code "1234 AB" to "1234 AB" on blur (already formatted)', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Type the already formatted postal code
    await user.type(input, '1234 AB');
    expect(input).toHaveValue('1234 AB');

    // Trigger blur event
    await user.tab();

    // Check that the value remains correctly formatted
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent(
        '1234 AB'
      );
    });
  });

  it('handles postal code with extra spaces "1234  AB" on blur', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Type postal code with extra spaces
    await user.type(input, '1234  AB');
    expect(input).toHaveValue('1234  AB');

    // Trigger blur event
    await user.tab();

    // Check that the value is formatted correctly with single space
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent(
        '1234 AB'
      );
    });
  });

  it('does not format invalid postal code on blur', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Type an invalid postal code
    await user.type(input, '123ABC');
    expect(input).toHaveValue('123ABC');

    // Trigger blur event
    await user.tab();

    // Check that the invalid value remains unchanged
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent(
        '123ABC'
      );
    });
  });

  it('handles empty value on blur', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Focus and then blur without typing anything
    await user.click(input);
    await user.tab();

    // Check that empty value remains empty
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent('');
    });
  });

  it('trims whitespace from postal code on blur', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');

    // Type postal code with leading and trailing spaces
    await user.type(input, '  1234AB  ');
    expect(input).toHaveValue('  1234AB  ');

    // Trigger blur event
    await user.tab();

    // Check that whitespace is trimmed and value is formatted
    await waitFor(() => {
      expect(screen.getByTestId('postal-code-value')).toHaveTextContent(
        '1234 AB'
      );
    });
  });

  it('shows validation error for invalid format', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByPlaceholderText('Vul postcode in');
    const submitButton = screen.getByTestId('submit-button');

    // Type an invalid postal code and blur
    await user.type(input, '123ABC');
    await user.tab();

    // Submit the form to trigger validation
    await user.click(submitButton);

    // The validation message should appear
    await waitFor(() => {
      expect(
        screen.getByText('Postcode moet het formaat 1234 AB hebben')
      ).toBeInTheDocument();
    });
  });

  it('shows required validation error when field is empty and required', async () => {
    const user = userEvent.setup();
    render(<TestWrapper required={true} />);

    const input = screen.getByPlaceholderText('Vul postcode in');
    const submitButton = screen.getByTestId('submit-button');

    // Focus and blur without typing anything
    await user.click(input);
    await user.tab();

    // Submit the form to trigger validation
    await user.click(submitButton);

    // The required validation message should appear
    await waitFor(() => {
      expect(screen.getByText('Postcode is verplicht')).toBeInTheDocument();
    });
  });

  it('does not show required validation error when field is not required', async () => {
    const user = userEvent.setup();
    render(<TestWrapper required={false} />);

    const input = screen.getByPlaceholderText('Vul postcode in');
    const submitButton = screen.getByTestId('submit-button');

    // Focus and blur without typing anything
    await user.click(input);
    await user.tab();

    // Submit the form
    await user.click(submitButton);

    // No validation error should appear
    expect(screen.queryByText('Postcode is verplicht')).not.toBeInTheDocument();
  });

  it('renders as disabled when disabled prop is true', () => {
    render(<TestWrapper disabled={true} />);

    const input = screen.getByPlaceholderText('Vul postcode in');
    expect(input).toBeDisabled();
  });

  it('renders with initial value', () => {
    render(<TestWrapper defaultValue="5678 CD" />);

    const input = screen.getByPlaceholderText('Vul postcode in');
    expect(input).toHaveValue('5678 CD');
  });
});
