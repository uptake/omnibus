# Omnibus

A tool that migrates Maven binaries from Nexus to Artifactory, or between different instances of Artifactory.

## Configuration

The configuration is a twofold: there is a `config.yml` that keeps preserved settings and a set of CLI arguments.

`cp config.yml.example config.yml` and edit the config

```yaml
# config.yml
nexus:
  apiHandle: https://mynexus.instance.com/nexus/service/local
  defaultRepository: releases

artifactory:
  host: https://myartifactory.instance.com
  sourceHost: https://artifactory-to-copy-from-when-transferring-arty-to-arty
  defaultRepository: libs-release-local
  apiKey: yOurApIKeyGoesHere

parallelism: 4
```

CLI arguments:
```shell
java -Xms4g -Xmx6g -jar ./build/libs/omnibus-0.0.1-all.jar --help
usage: java -jar omnibus.jar [-h] [-f FROM_REPO] [-p FROM_PATH] [-d DEST_REPO]
                             [-a ARTIFACTS_LIST] [--parallelism PARALLELISM]
                             [-s] [-v]

optional arguments:
  -h, --help                            show this help message and exit

  -f FROM_REPO, --from-repo FROM_REPO   Name of the source repo to copy. If
                                        not specified, the
                                        "nexus.defaultRepository" value from
                                        `config.yml` is taken

  -p FROM_PATH, --from-path FROM_PATH   Name of the source folder to copy. If
                                        not specified, all artifacts under the
                                        specified source will be copied.

  -d DEST_REPO, --dest-repo DEST_REPO   Name of the destination repo. If not
                                        specified, the
                                        "artifactory.defaultRepository" value
                                        from `config.yml` is taken

  -a ARTIFACTS_LIST,                    Name of the artifacts list JSON dump.
  --artifacts-list ARTIFACTS_LIST       If not specified, current list of
                                        artifacts will be dumped in a JSON
                                        file with current timestamp, e.g.
                                        artifactsList-1504196592.json

  --parallelism PARALLELISM             Number of coroutines running in
                                        parallel.

  --source-repo-type SOURCE_REPO_TYPE   Type of source repository:
                                        [artifactory, nexus]

  -s, --save-list                       Whether to save the list of artifacts
                                        to a JSON file.

  -v, --verbose                         Verbose output, prints [SKIP] log
                                        messages.
```

Artifacts that were not copied due to errors will be saved in a JSON dump file with current timestamp, e.g. `erroredOutArtifactsList-1504208085.json` .
Depending on how many parallel workers you run, you might want to rig your java heap size settings, e.g. `java -Xms4096m -Xmx8192m -jar ./build/libs/omnibus-0.1.0-all.jar --parallelism=25`

## Examples

```shel
# Copy all artifacts from Nexus "releases/com/wombat/android" repo to Artifactory "libs-release-local"
./gradlew sJ && java -jar ./build/libs/omnibus-1.0-SNAPSHOT-all.jar --from-repo=releases --dest-repo=libs-release-local --from-path=com/wombat/android

# Same as above, using the config.yml defauls
java -jar ./build/libs/omnibus-1.0-SNAPSHOT-all.jar --from-path=com/wombat/android

# Copy all packages from Nexus "releases" to Artifactory "libs-reease-local"
java -jar ./build/libs/omnibus-1.0-SNAPSHOT-all.jar

# Copy all packages from Nexus "releases/com" to Artifactory and use `artifactsList-1504126034.json` to load the artifacts list
java -jar ./build/libs/omnibus-1.0-SNAPSHOT-all.jar --from-path=com --artifacts-list=artifactsList-1504126034.json

# Copy artifacts from one instance of Artifactory to another.
# Note that it requires the presence of the `artifactory.sourceHost` key in `config.yml`
java -jar ./build/libs/omnibus-0.1.0-all.jar --source-repo-type=artifactory --from-repo=libs-release-local --dest-repo=wombat-local --from-path=blt/container/docker/blt.container.docker.gradle.plugin
```

## Development

To check for style compliance use [`ktlint`](https://github.com/shyiko/ktlint), for report on code smells and complexity analysis use [`detekt .`](https://github.com/arturbosch/detekt)

To make JVM accept self-signed SSL certificates:
```
openssl s_client -showcerts -connect 10.0.4.95:443 < /dev/null 2> /dev/null | openssl x509 -outform PEM > ~/insecure-artifactory.pem
sudo keytool -import -v -noprompt -trustcacerts -alias artifactory-test4 -file ~/insecure-artifactory.pem -keystore $(/usr/libexec/java_home)/jre/lib/security/cacerts -keypass changeit -storepass changeit
```
and then modify the hostsfile
```
# /etc/hosts
10.0.4.95 dev.artifactory.uptake.com
```
