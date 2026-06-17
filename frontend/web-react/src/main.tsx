import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { ApolloProvider } from '@apollo/client/react';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { store } from './store';
import { apolloClient } from './api/apollo';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <ApolloProvider client={apolloClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ApolloProvider>
    </Provider>
  </StrictMode>,
);
