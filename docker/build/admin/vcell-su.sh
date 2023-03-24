#!/bin/bash
shopt -s -o nounset

# get latest configuration file (this script should be installed in /share/apps/vcell3/deployed_github/config)
# use a symbolic link rather than move this file
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
LATEST_CONFIG_FILE=$(ls -Art ${SCRIPT_DIR}/*.config | tail -n 1)

# extract config from (e.g. simdata paths) from the latest configuration file
PRIMARY_SIMDATA_DIR=$(cat $LATEST_CONFIG_FILE | grep VCELL_SIMDATADIR_EXTERNAL | cut -d"=" -f2)
SECONDARY_SIMDATA_DIR=$(cat $LATEST_CONFIG_FILE | grep VCELL_SIMDATADIR_SECONDARY_HOST | cut -d"=" -f2)
ARCHIVE_SIMDATA_DIR=$(cat $LATEST_CONFIG_FILE | grep VCELL_SIMDATADIR_ARCHIVE_HOST | cut -d"=" -f2)
SECRETS_DIR=$(cat $LATEST_CONFIG_FILE | grep VCELL_SECRETS_DIR | cut -d"=" -f2)
CONTAINER_TAG=$(cat $LATEST_CONFIG_FILE | grep VCELL_TAG | cut -d"=" -f2)
REPO_NAMESPACE=$(cat $LATEST_CONFIG_FILE | grep VCELL_REPO_NAMESPACE | cut -d"=" -f2)

show_help() {
  echo "vcell superuser cli - USE THIS POWER WITH CAUTION - for vcell admins only"
  echo "usage:"
  echo "   (1) cd /path/to/non-root-squashed/working/dir (e.g. /share/apps/vcell3/working)"
  echo "               **IMPORTANT** only the following drives are mounted into the CLI container:"
  echo "                     the current directory (MUST NOT BE ROOT SQUASHED)    ${PWD}"
  echo "                     VCell's primary simdata directory                    ${PRIMARY_SIMDATA_DIR}"
  echo "                     VCell's archive simdata directory                    ${ARCHIVE_SIMDATA_DIR}"
  echo "                     VCell's secondary simdata directory                  ${SECONDARY_SIMDATA_DIR}"
  echo ""
  echo "   (2) vcell-su COMMAND [args]"
  echo "               **IMPORTANT** any filenames in arguments should be:"
  echo "                     (a) all file arguments must be located under the current directory"
  echo "                     (b) only use absolute paths (use \${PWD}/filename, not ./filename)"
  echo ""
  echo "   example commands:"
  echo "      help"
  echo "      usage -o \${PWD}/report.html"
  echo "      simdata-verifier help"
  echo "      simdata-verifier -u boris -o $PWD/vcelldata/repair_reports --model-visibility PUBLIC "
  echo "      simdata-verifier -u schaff -o $PWD/vcelldata/repair_reports --simulation-id 252697124 --run-never-ran"
  echo "      simdata-verifier -u schaff -o $PWD/vcelldata/repair_reports --simulation-id 252697124 --run-never-ran --rerun-lost-data"
	exit 1
}

if [ "$#" -lt 1 ]; then
    show_help
fi

sudo docker pull "${REPO_NAMESPACE}/vcell-admin:${CONTAINER_TAG}"

arguments=$*

sudo env $(xargs < "${LATEST_CONFIG_FILE}") \
  docker run -it --rm \
  --env-file="${LATEST_CONFIG_FILE}" \
  -v "${SECRETS_DIR}:/run/secrets" \
  -v "${PRIMARY_SIMDATA_DIR}:${PRIMARY_SIMDATA_DIR}" \
  -v "${ARCHIVE_SIMDATA_DIR}:${ARCHIVE_SIMDATA_DIR}" \
  -v "${SECONDARY_SIMDATA_DIR}:${SECONDARY_SIMDATA_DIR}" \
  -v "${PWD}:${PWD}" \
  "${REPO_NAMESPACE}/vcell-admin:${CONTAINER_TAG}" \
  $arguments