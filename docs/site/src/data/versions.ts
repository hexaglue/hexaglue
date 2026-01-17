/**
 * Versions centralisées des artefacts Maven HexaGlue.
 * Modifier ici pour mettre à jour toutes les occurrences dans la documentation.
 */

export const versions = {
	/** Version de HexaGlue (core et hexaglue-maven-plugin) */
	hexaglue: '4.0.0',

	/** Version des plugins HexaGlue (living-doc, audit, jpa) */
	plugins: '2.0.0',

	/** GroupId Maven pour core et hexaglue-maven-plugin */
	groupId: 'io.hexaglue',

	/** GroupId Maven pour les plugins (living-doc, audit, jpa) */
	pluginsGroupId: 'io.hexaglue.plugins',
} as const;

export type Versions = typeof versions;

export default versions;
