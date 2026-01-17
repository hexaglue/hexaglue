// Type definitions for HexaGlue site

export type NavLink = {
	href: string;
	label: string;
	prefetch?: boolean;
};

export type NavGroup = {
	label: string;
	items: Array<NavLink & { icon: string; badge?: string }>;
};

export type Nav = Array<NavLink | NavGroup>;

export type Gradient = 'blue-purple' | 'blue-green' | 'red-pink' | 'orange-yellow';
