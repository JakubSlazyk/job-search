import { gql } from '@apollo/client';

// Mirrors offer-service/src/main/resources/graphql/schema.graphqls.
export interface Offer {
  offerId: string;
  source: string;
  externalId: string;
  title: string;
  company: string;
  url: string;
  location: string;
  description: string;
  seniority: string;

  // Forward-compat enrichment fields the GraphQL schema does not expose yet. The UI renders each
  // only when present, so the richer detail layout (salary, stack, responsibilities, …) lights up
  // automatically once the backend starts projecting them. Not selected in the queries below until
  // the schema adds them — requesting an undefined field would error.
  employmentType?: string;
  remote?: boolean;
  salaryMin?: number;
  salaryMax?: number;
  skills?: string[];
  responsibilities?: string[];
  requirements?: string[];
  companyBlurb?: string;
}

export interface OffersVars {
  query?: string;
  source?: string;
  location?: string;
  seniority?: string;
  page?: number;
  size?: number;
}

export const OFFERS = gql`
  query Offers(
    $query: String
    $source: String
    $location: String
    $seniority: String
    $page: Int
    $size: Int
  ) {
    offers(
      query: $query
      source: $source
      location: $location
      seniority: $seniority
      page: $page
      size: $size
    ) {
      offerId
      title
      company
      location
      seniority
      source
    }
  }
`;

export const OFFER = gql`
  query Offer($offerId: ID!) {
    offer(offerId: $offerId) {
      offerId
      title
      company
      location
      seniority
      source
      url
      description
    }
  }
`;
