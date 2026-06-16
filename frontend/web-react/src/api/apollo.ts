import { ApolloClient, HttpLink, InMemoryCache } from '@apollo/client';

// GraphQL goes to the gateway's public `/graphql` route (proxied to offer-service). `credentials:
// 'include'` keeps the BFF session cookie flowing even though offer browse is public, so the same
// client can serve authenticated queries in later subphases without reconfiguration.
export const apolloClient = new ApolloClient({
  link: new HttpLink({ uri: '/graphql', credentials: 'include' }),
  cache: new InMemoryCache(),
});
