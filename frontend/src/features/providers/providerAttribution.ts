export type AttributionImage = {
  src: string
  alt: string
  href?: string
}

export const providerAttribution: Record<string, AttributionImage[]> = {
  boardgamegeek: [
    {
      src: "/images/powered-by-bgg.svg",
      alt: "Powered by BoardGameGeek",
      href: "https://boardgamegeek.com",
    },
  ],
  openlibrary: [
    {
      src: "/images/powered-by-openlibrary.png",
      alt: "Powered by Open Library",
      href: "https://openlibrary.org",
    },
  ],
}
