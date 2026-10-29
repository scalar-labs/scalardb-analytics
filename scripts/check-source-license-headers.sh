#!/usr/bin/env bash
# Copyright Scalar, Inc.
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

readonly required_copyright="Copyright Scalar, Inc."
readonly required_license="SPDX-License-Identifier: Apache-2.0"
status=0

# Third-party sources redistributed verbatim under their own license. They keep their original
# header, so the Scalar header is not expected in them.
readonly -a third_party_sources=(
  # Temporary fix for https://emudhra.we-have-a-problem.com/ (Oracle, BSD-3-Clause).
  # Remove this entry together with the file after we update to the fixed version of Java.
  "scalardb-analytics-core/docker/tmp-fix-ca-compromise/CheckCerts.java"
)

is_third_party_source() {
  local candidate
  for candidate in "${third_party_sources[@]}"; do
    [[ "$1" == "$candidate" ]] && return 0
  done
  return 1
}

while IFS= read -r -d '' file; do
  if is_third_party_source "$file"; then
    continue
  fi
  case "$file" in
    *.java | *.scala | *.kt | *.kts | *.py | *.mill)
      header=$(sed -n '1,12p' "$file")
      if ! grep -Fq "$required_copyright" <<<"$header"; then
        printf 'Missing copyright header: %s\n' "$file" >&2
        status=1
      fi
      if ! grep -Fq "$required_license" <<<"$header"; then
        printf 'Missing SPDX license identifier Apache-2.0: %s\n' "$file" >&2
        status=1
      fi
      ;;
  esac
done < <(git ls-files -z)

exit "$status"
