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
