export type SocialLink = {
	/** Longer descriptive label, e.g. `"Voir HexaGlue sur GitHub"` */
	text: string;
	/** Short label with the name of the platform, e.g. `"GitHub"`*/
	label: string;
	/** Icon name for use with `astro-icon`, e.g. `"social/github"`. */
	icon: string;
	/** URL for our profile on the external platform. */
	href: string;
	/** Platform ID, e.g. `"github"`. */
	platform: string;
	/** Whether this platform should be linked in the site header */
	showInHeader?: boolean;
};

type SiteInfo = {
	name: string;
	title: string;
	description: string;
	image: {
		src: string;
		alt: string;
	};
	socialLinks: SocialLink[];
};

const siteInfo: SiteInfo = {
	name: 'HexaGlue',
	title: 'Compilez l\'architecture, pas seulement le code',
	description:
		'HexaGlue est un moteur d\'analyse et d\'automatisation architecturale qui s\'active à la compilation des applications Java. Sans framework impose. Sans vendor lock-in. Sans impact sur le runtime.',
	image: {
		src: '/og/hexaglue.jpg',
		alt: 'HexaGlue - Compilez l\'architecture, pas seulement le code',
	},
	socialLinks: [
		{
			platform: 'github',
			icon: 'social/github',
			label: 'GitHub',
			text: 'Voir HexaGlue sur GitHub',
			href: 'https://github.com/hexaglue/hexaglue',
			showInHeader: true,
		},
	],
};

export default siteInfo;
