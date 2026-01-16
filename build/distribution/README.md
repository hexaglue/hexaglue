# HexaGlue Distribution

Guide to publish HexaGlue on Maven Central via the Central Portal.

## Prerequisites

### 1. Central Portal Account

Log in at [central.sonatype.com](https://central.sonatype.com) with your account.

### 2. Authentication Token

Generate a token in **Account > Generate User Token**.

Add it to `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>YOUR_TOKEN_USERNAME</username>
            <password>YOUR_TOKEN_PASSWORD</password>
        </server>
    </servers>
</settings>
```

### 3. GPG Key

Create a GPG key if you don't have one:

```bash
gpg --gen-key
```

Publish it to a key server:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## Publishing

### 1. Prepare the Version

Update the version in all `pom.xml` files (remove `-SNAPSHOT`):

```bash
mvn versions:set -DnewVersion=2.0.0
mvn versions:commit
```

### 2. Verify the Build

```bash
mvn clean verify
```

### 3. Publish to Maven Central

```bash
mvn clean deploy -Prelease
```

This command:
- Compiles and tests the code
- Generates sources and Javadoc JARs
- Signs all artifacts with GPG
- Publishes to Maven Central via Central Portal

### 4. Verify Publication

Track progress at [central.sonatype.com](https://central.sonatype.com) > Deployments.

Once published, artifacts will be available at:
- https://central.sonatype.com/artifact/io.hexaglue/hexaglue-core
- https://repo1.maven.org/maven2/io/hexaglue/

### 5. Git Tag

```bash
git tag v2.0.0
git push origin v2.0.0
```

### 6. Prepare Next Version

```bash
mvn versions:set -DnewVersion=2.1.0-SNAPSHOT
mvn versions:commit
git commit -am "Prepare next development iteration"
git push
```

## GPG Configuration

### Using gpg-agent (recommended)

To avoid entering the passphrase for each signature:

```bash
# Start the agent
gpg-agent --daemon

# Cache the passphrase
echo "test" | gpg --clearsign > /dev/null

# Configure cache (in ~/.gnupg/gpg-agent.conf)
default-cache-ttl 3600
max-cache-ttl 86400
```

### Specify GPG Key

If you have multiple keys, specify which one to use in `~/.m2/settings.xml`:

```xml
<settings>
    <profiles>
        <profile>
            <id>gpg</id>
            <properties>
                <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
            </properties>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>gpg</activeProfile>
    </activeProfiles>
</settings>
```

## Troubleshooting

### GPG Signing Error

```
gpg: signing failed: No pinentry
```

Solution: install and configure pinentry:

```bash
brew install pinentry-mac
echo "pinentry-program $(which pinentry-mac)" >> ~/.gnupg/gpg-agent.conf
gpgconf --kill gpg-agent
```

### Central Authentication Error

Verify that:
1. The `<id>central</id>` in settings.xml matches `<publishingServerId>central</publishingServerId>`
2. The token has not expired
3. The namespace `io.hexaglue` is validated on your account

### Validation Failed

Maven Central requires:
- `groupId`, `artifactId`, `version`
- `name`, `description`, `url`
- `licenses`
- `developers`
- `scm`
- Sources JAR (`*-sources.jar`)
- Javadoc JAR (`*-javadoc.jar`)
- GPG signatures (`*.asc`)

Verify with:

```bash
mvn clean verify -Prelease
ls target/*.jar target/*.asc
```

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
