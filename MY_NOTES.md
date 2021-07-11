Publish notes:

1. Edit versio.sbt, build.sbt, generateVersionFile, ThisBuild / version
2. export OSS_ST_USERNAME=samma som man loggar in me ti https://oss.sonatype.org/
3. export OSS_ST_PASSWORD=samma som man loggar in me ti https://oss.sonatype.org/
4. export GPG_TTY=$(tty) (otherwise https://github.com/keybase/keybase-issues/issues/2798)
5. sbt clean compile
6. sbt +publishLocal +plugin/test +plugin/scripted
7. sbt +publishSigned +plugin/test +plugin/scripted
8. once published got to https://oss.sonatype.org/ sonatype and manually publish the release candidate
9. Go to staging repositories
10. You then need to "close" the release candidate which will trigger scans
11. After success you can perform actual release


