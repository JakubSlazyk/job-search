import { render, screen } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import DetailSidebar from './DetailSidebar';
import { OFFER } from './queries';

function offerMock(description: string) {
  return [
    {
      request: { query: OFFER, variables: { offerId: 'o1' } },
      result: {
        data: {
          offer: {
            __typename: 'Offer',
            offerId: 'o1',
            title: 'Senior Python Developer',
            company: 'Payne, Roberts and Davis',
            location: 'Stewartbury, AA',
            seniority: 'SENIOR',
            source: 'himalayas',
            url: 'https://example.com/job',
            description,
          },
        },
      },
    },
  ];
}

function renderAt(description: string) {
  return render(
    <MockedProvider mocks={offerMock(description)}>
      <MemoryRouter initialEntries={['/offers/o1']}>
        <Routes>
          <Route path="/offers/:offerId" element={<DetailSidebar />} />
        </Routes>
      </MemoryRouter>
    </MockedProvider>,
  );
}

describe('DetailSidebar description', () => {
  it('renders HTML tags in the description instead of escaping them', async () => {
    renderAt('<p>Build <strong>great</strong> things.</p><ul><li>One</li></ul>');

    const strong = await screen.findByText('great');
    expect(strong.tagName).toBe('STRONG');
    expect(screen.getByText('One').tagName).toBe('LI');
  });

  it('strips dangerous markup (XSS) before rendering', async () => {
    renderAt('<p>Safe text</p><script>window.__xss = true</script><img src=x onerror="window.__xss = true">');

    await screen.findByText('Safe text');
    expect(document.querySelector('script')).toBeNull();
    expect(document.querySelector('img')?.getAttribute('onerror')).toBeNull();
    expect((window as unknown as { __xss?: boolean }).__xss).toBeUndefined();
  });

  it('shows an empty-state when the source provides no description', async () => {
    renderAt('');

    expect(await screen.findByText(/no description provided/i)).toBeInTheDocument();
  });
});
