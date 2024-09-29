import {fireEvent, render, screen} from '@testing-library/react';
import App from './App';

test('open Add car modal form', () => {
  render(<App />);
  fireEvent.click(screen.getByText('New Car'));
  expect(screen.getByRole('dialog')).toHaveTextContent('Add car');
});
