// @ts-check

import mdx from '@astrojs/mdx';
import sitemap from '@astrojs/sitemap';
import tailwind from '@astrojs/tailwind';
import { defineConfig } from 'astro/config';
import astroExpressiveCode from 'astro-expressive-code';
import icon from 'astro-icon';
import houston from './houston.theme.json';

// https://astro.build/config
export default defineConfig({
	site: 'https://hexaglue.io',
	prefetch: true,
	integrations: [
		tailwind({
			applyBaseStyles: false,
		}),
		astroExpressiveCode({
			themes: [houston],
			styleOverrides: {
				borderRadius: '0.375rem',
				borderColor: 'rgb(84 88 100)',
				// Style moderne avec effet halo pour les lignes marquées
				textMarkers: {
					// Effet halo : dégradé lumineux de gauche à droite qui attire le regard
					markBackground: 'linear-gradient(90deg, rgba(56, 189, 248, 0.25) 0%, rgba(56, 189, 248, 0.12) 20%, rgba(56, 189, 248, 0.03) 60%, transparent 100%)',
					// Bordure gauche lumineuse (cyan/sky)
					markBorderColor: 'rgb(56, 189, 248)',
				},
			},
			defaultProps: {
				overridesByLang: {
					'bash,sh,shell': {
						frame: 'none',
					},
				},
			},
		}),
		icon({
			svgoOptions: {
				plugins: [
					{ name: 'preset-default' },
					{
						name: 'prefixIds',
						// Ensure IDs used in SVGs are unique to avoid clashes between inline SVGs.
						params: { prefix: () => Math.round(Math.random() * 1_000_000_000).toString(36) },
					},
				],
			},
		}),
		mdx(),
		sitemap(),
	],
	image: {
		domains: [],
	},
	vite: {
		ssr: {
			noExternal: [],
		},
	},
	experimental: {
		contentIntellisense: true,
	},
});
