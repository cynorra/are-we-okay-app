import { MetadataRoute } from 'next'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/checkin', '/feed', '/map', '/insights', '/friends', '/settings'],
    },
    sitemap: 'https://areweokay.com/sitemap.xml',
  }
}
