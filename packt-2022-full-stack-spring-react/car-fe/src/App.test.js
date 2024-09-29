import {fireEvent, render, screen} from '@testing-library/react';
import App from './App';
import TestRednerer from 'react-test-renderer';
import {AddCar} from "./components/AddCar";

test('renders a snapshot', () => {
  const tree = TestRednerer.create(<AddCar />).toJSON();
  expect(tree).toMatchSnapshot();
});

test('open Add car modal form', () => {
  render(<App />);
  fireEvent.click(screen.getByText('New Car'));
  expect(screen.getByRole('dialog')).toHaveTextContent('Add car');
});
