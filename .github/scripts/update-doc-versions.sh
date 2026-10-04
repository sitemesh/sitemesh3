#!/usr/bin/env bash
# Points the install snippets in README.md and QUICKSTART.md at a new version. The Release
# workflow runs this for the release commit and again for the snapshot bump; run it by hand
# to preview a release: .github/scripts/update-doc-versions.sh 3.3.1
#
# A release version (3.3.1, 3.3.1-RC1) replaces every org.sitemesh coordinate and Maven
# <version> that is not a SNAPSHOT, plus the README's download link for the same 3.3 line
# (the table links every maintained line). A SNAPSHOT version replaces only the SNAPSHOT ones.
set -euo pipefail

version=${1:?usage: $0 <version>}
cd "$(dirname "$0")/../.."

files=()
for f in README.md QUICKSTART.md; do
  if [ -f "$f" ]; then files+=("$f"); fi
done
if [ ${#files[@]} -eq 0 ]; then exit 0; fi

VERSION="$version" perl -0777 -pi -e '
  my $v = $ENV{VERSION};
  my $kind = sub { $_[0] =~ /-SNAPSHOT$/ ? 1 : 0 };
  my $want = $kind->($v);

  s{(org\.sitemesh:[\w-]+:)(\d[\w.-]*)}{ $kind->($2) == $want ? "$1$v" : "$1$2" }ge;
  s{(<artifactId>(?:sitemesh[\w-]*|spring-[\w-]*sitemesh)</artifactId>\s*<version>)([^<]+)(</version>)}
   { $kind->($2) == $want ? "$1$v$3" : "$1$2$3" }ge;

  unless ($want) {
    (my $line = $v) =~ s/^(\d+\.\d+)\..*/$1/;
    s{\[Download \Q$line\E\.[^\]]*\]\(https://github\.com/sitemesh/sitemesh3/releases/tag/\Q$line\E\.[^)]*\)}
     {[Download $v](https://github.com/sitemesh/sitemesh3/releases/tag/$v)}g;
  }
' "${files[@]}"
