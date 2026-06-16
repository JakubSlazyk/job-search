import { render, screen } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import OffersPage from './OffersPage';
import { OFFERS } from './queries';

const mocks = [
  {
    request: { query: OFFERS, variables: { page: 0, size: 20 } },
    result: {
      data: {
        offers: [
          {
            __typename: 'Offer',
            offerId: 'o1',
            title: 'Senior Kotlin Dev',
            company: 'Acme',
            location: 'Remote',
            seniority: 'SENIOR',
            source: 'demo',
          },
        ],
      },
    },
  },
];

describe('OffersPage', () => {
  it('renders offers from the GraphQL response', async () => {
    render(
      <MockedProvider mocks={mocks}>
        <MemoryRouter>
          <OffersPage />
        </MemoryRouter>
      </MockedProvider>,
    );

    expect(await screen.findByText('Senior Kotlin Dev')).toBeInTheDocument();
    expect(screen.getByText(/Acme/)).toBeInTheDocument();
  });
});
