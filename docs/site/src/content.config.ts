import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { z } from 'astro/zod';

const seoSchema = z.object({
	title: z.string().min(5).max(120),
	description: z.string().min(15).max(160),
	image: z
		.object({
			src: z.string().default('/og/hexaglue.jpg'),
			alt: z.string().default('HexaGlue - Compilez l\'architecture, pas seulement le code'),
		})
		.default({}),
	pageType: z.enum(['website', 'article']).default('website'),
	robots: z
		.object({
			index: z.boolean().default(true),
			follow: z.boolean().default(true),
		})
		.default({}),
});

export const collections = {
	pages: defineCollection({
		loader: glob({ base: './src/content/pages', pattern: '**/*.{md,mdx}' }),
		schema: z.object({
			seo: seoSchema,
			locale: z.enum(['fr', 'en']).default('fr'),
			section: z.enum(['ressources', 'entreprise']).default('ressources'),
			updated_date: z.date().optional().describe('Date de dernière mise à jour du contenu.'),
		}),
	}),
};
